#!/usr/bin/env python3
"""
Redis Ride Listener Module

This module listens to Redis for ride creation events from the Java Spring Boot application.
When a ride is created, it receives the pickup location coordinates (lat, lng),
computes a geohash-based zone ID using python-geohash, and outputs the ride details.

Usage:
    ./redis_ride_listener.py
    
Requirements:
    pip install redis python-geohash
"""

import json
import logging
import signal
import sys
import os
from typing import Dict, Optional, Callable
from dataclasses import dataclass
from datetime import datetime

import redis
import geohash

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)

# Configuration
REDIS_HOST = os.getenv('REDIS_HOST', 'localhost')
REDIS_PORT = int(os.getenv('REDIS_PORT', 6379))
REDIS_DB = int(os.getenv('REDIS_DB', 0))
REDIS_CHANNEL = os.getenv('REDIS_CHANNEL', 'ride:requested')

# Geohash precision (number of characters)
# Precision approximations:
# 4 chars = ~20km x ~20km (city zone)
# 5 chars = ~2.4km x ~2.4km (neighborhood)
# 6 chars = ~0.6km x ~0.6km (block level)
# 7 chars = ~0.15km x ~0.15km (street level)
GEOHASH_PRECISION = int(os.getenv('GEOHASH_PRECISION', '6'))


@dataclass
class RideEvent:
    """Represents a ride creation event from Redis"""
    ride_id: int
    customer_id: int
    start_location_id: int
    drop_location_id: int
    upfront_fare: float
    pickup_lat: Optional[float] = None
    pickup_lng: Optional[float] = None
    geo_hash: Optional[str] = None
    computed_zone_id: Optional[str] = None
    received_at: Optional[datetime] = None
    
    @classmethod
    def from_json(cls, data: Dict) -> 'RideEvent':
        """Create RideEvent from JSON dictionary"""
        return cls(
            ride_id=data.get('rideId'),
            customer_id=data.get('customerId'),
            start_location_id=data.get('startLocationId'),
            drop_location_id=data.get('dropLocationId'),
            upfront_fare=data.get('upfrontFare', 0.0),
            pickup_lat=_to_float(data.get('pickupLat')),
            pickup_lng=_to_float(data.get('pickupLng')),
            geo_hash=data.get('geoHash'),
            received_at=datetime.now()
        )
    
    def compute_geohash_zone(self, precision: int = GEOHASH_PRECISION) -> Optional[str]:
        """
        Compute a geohash-based zone ID from pickup coordinates.
        
        Args:
            precision: Number of characters in the geohash (higher = more precise)
            
        Returns:
            Geohash string representing the geographic zone, or None if coordinates unavailable
        """
        if self.pickup_lat is None or self.pickup_lng is None:
            logger.warning(f"Ride {self.ride_id}: Missing coordinates for geohash computation")
            return None
        
        try:
            # Encode lat/lng to geohash
            zone_id = geohash.encode(self.pickup_lat, self.pickup_lng, precision=precision)
            self.computed_zone_id = zone_id
            return zone_id
        except Exception as e:
            logger.error(f"Ride {self.ride_id}: Failed to compute geohash: {e}")
            return None
    
    def to_dict(self) -> Dict:
        """Convert to dictionary for serialization"""
        return {
            'ride_id': self.ride_id,
            'customer_id': self.customer_id,
            'start_location_id': self.start_location_id,
            'drop_location_id': self.drop_location_id,
            'upfront_fare': self.upfront_fare,
            'pickup_lat': self.pickup_lat,
            'pickup_lng': self.pickup_lng,
            'geo_hash_from_java': self.geo_hash,
            'computed_zone_id': self.computed_zone_id,
            'received_at': self.received_at.isoformat() if self.received_at else None
        }
    
    def __str__(self) -> str:
        zone_info = f" Zone={self.computed_zone_id}" if self.computed_zone_id else ""
        coords = f" ({self.pickup_lat:.6f}, {self.pickup_lng:.6f})" if self.pickup_lat and self.pickup_lng else ""
        return f"Ride {self.ride_id}{coords}{zone_info} - Fare=${self.upfront_fare:.2f}"


def _to_float(value) -> Optional[float]:
    """Safely convert value to float"""
    if value is None:
        return None
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


class RideEventProcessor:
    """Processes ride events with custom handlers"""
    
    def __init__(self):
        self.handlers: list[Callable[[RideEvent], None]] = []
    
    def register_handler(self, handler: Callable[[RideEvent], None]):
        """Register a handler function for ride events"""
        self.handlers.append(handler)
    
    def process(self, event: RideEvent):
        """Process a ride event through all registered handlers"""
        for handler in self.handlers:
            try:
                handler(event)
            except Exception as e:
                logger.error(f"Handler {handler.__name__} failed: {e}")


class RedisRideListener:
    """
    Redis pub/sub listener for ride creation events.
    
    Listens to the configured Redis channel for ride:requested events,
    computes geohash zones, and processes the events.
    """
    
    def __init__(self, 
                 host: str = REDIS_HOST,
                 port: int = REDIS_PORT,
                 db: int = REDIS_DB,
                 channel: str = REDIS_CHANNEL,
                 geohash_precision: int = GEOHASH_PRECISION):
        self.host = host
        self.port = port
        self.db = db
        self.channel = channel
        self.geohash_precision = geohash_precision
        self.redis_client: Optional[redis.Redis] = None
        self.pubsub: Optional[redis.client.PubSub] = None
        self.processor = RideEventProcessor()
        self.running = False
        self.stats = {
            'messages_received': 0,
            'messages_processed': 0,
            'geohash_computed': 0,
            'errors': 0
        }
        
        # Register default handlers
        self.processor.register_handler(self._log_event)
        self.processor.register_handler(self._save_to_file)
    
    def connect(self) -> bool:
        """Establish connection to Redis"""
        try:
            self.redis_client = redis.Redis(
                host=self.host,
                port=self.port,
                db=self.db,
                decode_responses=True,
                socket_connect_timeout=5
            )
            # Test connection
            self.redis_client.ping()
            logger.info(f"Connected to Redis at {self.host}:{self.port}")
            return True
        except redis.ConnectionError as e:
            logger.error(f"Failed to connect to Redis: {e}")
            return False
        except Exception as e:
            logger.error(f"Unexpected error connecting to Redis: {e}")
            return False
    
    def subscribe(self) -> bool:
        """Subscribe to the Redis channel"""
        if not self.redis_client:
            logger.error("Not connected to Redis. Call connect() first.")
            return False
        
        try:
            self.pubsub = self.redis_client.pubsub()
            self.pubsub.subscribe(self.channel)
            logger.info(f"Subscribed to channel: {self.channel}")
            return True
        except Exception as e:
            logger.error(f"Failed to subscribe to channel {self.channel}: {e}")
            return False
    
    def _parse_message(self, message) -> Optional[RideEvent]:
        """Parse a Redis pub/sub message into a RideEvent"""
        try:
            # Redis pub/sub message format: {'type': 'message', 'channel': '...', 'data': '...'}
            if message['type'] != 'message':
                return None
            
            data = json.loads(message['data'])
            return RideEvent.from_json(data)
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse message JSON: {e}")
            self.stats['errors'] += 1
            return None
        except Exception as e:
            logger.error(f"Failed to parse message: {e}")
            self.stats['errors'] += 1
            return None
    
    def _compute_geohash(self, event: RideEvent) -> Optional[str]:
        """Compute geohash zone for the event"""
        zone_id = event.compute_geohash_zone(self.geohash_precision)
        if zone_id:
            self.stats['geohash_computed'] += 1
        return zone_id
    
    def _log_event(self, event: RideEvent):
        """Default handler: Log the event"""
        logger.info(f"PROCESSED: {event}")
    
    def _save_to_file(self, event: RideEvent):
        """Default handler: Append event to file"""
        try:
            with open('ride_events.jsonl', 'a') as f:
                f.write(json.dumps(event.to_dict()) + '\n')
        except Exception as e:
            logger.error(f"Failed to save event to file: {e}")
    
    def process_message(self, message):
        """Process a single Redis message"""
        self.stats['messages_received'] += 1
        
        event = self._parse_message(message)
        if not event:
            return
        
        # Compute geohash zone
        self._compute_geohash(event)
        
        # Process through handlers
        self.processor.process(event)
        self.stats['messages_processed'] += 1
    
    def listen(self):
        """Start listening for messages (blocking)"""
        if not self.pubsub:
            logger.error("Not subscribed to any channel. Call subscribe() first.")
            return
        
        self.running = True
        logger.info(f"Listening for ride events on channel '{self.channel}'...")
        logger.info(f"Geohash precision: {self.geohash_precision} chars (~{self._get_precision_description()})")
        
        try:
            for message in self.pubsub.listen():
                if not self.running:
                    break
                
                if message['type'] == 'message':
                    self.process_message(message)
                    
        except KeyboardInterrupt:
            logger.info("Interrupted by user")
        except Exception as e:
            logger.error(f"Error while listening: {e}")
        finally:
            self.stop()
    
    def _get_precision_description(self) -> str:
        """Get human-readable description of geohash precision"""
        descriptions = {
            4: "~20km (city zone)",
            5: "~2.4km (neighborhood)",
            6: "~0.6km (block level)",
            7: "~0.15km (street level)",
            8: "~0.04km (building level)"
        }
        return descriptions.get(self.geohash_precision, "custom")
    
    def stop(self):
        """Stop listening and clean up"""
        self.running = False
        logger.info("Stopping listener...")
        
        if self.pubsub:
            self.pubsub.unsubscribe()
            self.pubsub.close()
            logger.info("Unsubscribed from channel")
        
        if self.redis_client:
            self.redis_client.close()
            logger.info("Redis connection closed")
        
        self._print_stats()
    
    def _print_stats(self):
        """Print processing statistics"""
        logger.info("=" * 60)
        logger.info("LISTENER STATISTICS")
        logger.info("=" * 60)
        logger.info(f"Messages received:    {self.stats['messages_received']}")
        logger.info(f"Messages processed:   {self.stats['messages_processed']}")
        logger.info(f"Geohashes computed:   {self.stats['geohash_computed']}")
        logger.info(f"Errors:               {self.stats['errors']}")
        logger.info("=" * 60)


def demo_geohash_zones():
    """
    Demonstrate geohash zone computation for sample coordinates.
    This can be run standalone to understand geohash precision levels.
    """
    print("\n" + "=" * 60)
    print("GEOHASH ZONE DEMONSTRATION")
    print("=" * 60)
    
    # Sample coordinates (San Francisco area)
    test_locations = [
        ("Union Square, SF", 37.7879, -122.4075),
        ("Fisherman's Wharf", 37.8080, -122.4177),
        ("Golden Gate Park", 37.7694, -122.4862),
        ("SFO Airport", 37.6213, -122.3790),
        ("Oakland Downtown", 37.8044, -122.2712),
    ]
    
    precisions = [4, 5, 6, 7]
    
    for name, lat, lng in test_locations:
        print(f"\n{name}: ({lat}, {lng})")
        for precision in precisions:
            zone = geohash.encode(lat, lng, precision=precision)
            print(f"  Precision {precision}: {zone}")
    
    print("\n" + "=" * 60)
    print("Note: Same prefix = same zone")
    print("  4 chars = city-level zone")
    print("  6 chars = block-level zone (default)")
    print("=" * 60 + "\n")


def main():
    """Main entry point"""
    import argparse
    
    parser = argparse.ArgumentParser(
        description='Redis Ride Listener - Listens for ride events and computes geohash zones'
    )
    parser.add_argument(
        '--demo', 
        action='store_true',
        help='Run geohash demonstration and exit'
    )
    parser.add_argument(
        '--host',
        default=REDIS_HOST,
        help=f'Redis host (default: {REDIS_HOST})'
    )
    parser.add_argument(
        '--port',
        type=int,
        default=REDIS_PORT,
        help=f'Redis port (default: {REDIS_PORT})'
    )
    parser.add_argument(
        '--channel',
        default=REDIS_CHANNEL,
        help=f'Redis channel to subscribe (default: {REDIS_CHANNEL})'
    )
    parser.add_argument(
        '--precision',
        type=int,
        default=GEOHASH_PRECISION,
        help=f'Geohash precision in chars (default: {GEOHASH_PRECISION})'
    )
    
    args = parser.parse_args()
    
    if args.demo:
        demo_geohash_zones()
        return 0
    
    # Create and start listener
    listener = RedisRideListener(
        host=args.host,
        port=args.port,
        channel=args.channel,
        geohash_precision=args.precision
    )
    
    # Setup signal handlers for graceful shutdown
    def signal_handler(signum, frame):
        logger.info(f"Received signal {signum}")
        listener.stop()
        sys.exit(0)
    
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
    
    # Connect and start listening
    if not listener.connect():
        logger.error("Failed to connect to Redis. Exiting.")
        return 1
    
    if not listener.subscribe():
        logger.error("Failed to subscribe to channel. Exiting.")
        return 1
    
    listener.listen()
    return 0


if __name__ == "__main__":
    sys.exit(main())
