# Redis Geohash Integration

This document describes the integration between the Java Spring Boot ride-hailing application and the Python Redis listener with geohash zone computation.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              RIDE CREATION FLOW                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────┐      POST /api/v1/rides       ┌──────────────────────────┐
│   Client/App    │ ─────────────────────────────►│   Java Spring Boot App   │
└─────────────────┘                               │   (RideMatchApplication) │
                                                  └────────────┬─────────────┘
                                                               │
                                                               │ 1. Save Ride
                                                               │ 2. Fetch Location
                                                               │ 3. Publish Event
                                                               ▼
                                                  ┌──────────────────────────┐
                                                  │     Redis Channel        │
                                                  │   "ride:requested"       │
                                                  └────────────┬─────────────┘
                                                               │
                                                               │ JSON Message
                                                               │ {
                                                               │   "rideId": 1,
                                                               │   "pickupLat": 37.77,
                                                               │   "pickupLng": -122.41,
                                                               │   ...
                                                               │ }
                                                               ▼
                                                  ┌──────────────────────────┐
                                                  │   Python Redis Listener  │
                                                  │  (redis_ride_listener.py)│
                                                  └────────────┬─────────────┘
                                                               │
                                                               │ Compute Geohash
                                                               │ from lat/lng
                                                               ▼
                                                  ┌──────────────────────────┐
                                                  │   Zone ID: "9q8yym"      │
                                                  │   (6-char precision)     │
                                                  └──────────────────────────┘
```

## Components

### 1. Java Spring Boot Application

**Modified Files:**
- `RideRequestedEvent.java` - Added pickup coordinates and geohash fields
- `RideService.java` - Enhanced to fetch location and publish coordinates to Redis

**Event Published to Redis:**
```json
{
  "rideId": 123,
  "customerId": 456,
  "startLocationId": 789,
  "dropLocationId": 101,
  "upfrontFare": 25.50,
  "pickupLat": 37.77490000,
  "pickupLng": -122.41940000,
  "geoHash": "9q8yym0kpr0t"
}
```

### 2. Python Redis Listener

**File:** `redis_ride_listener.py`

**Features:**
- Subscribes to Redis pub/sub channel `ride:requested`
- Parses incoming ride events
- Computes geohash zone ID from pickup coordinates using `python-geohash`
- Configurable precision (default: 6 characters = ~0.6km x 0.6km)
- Logs events and saves to JSONL file
- Graceful shutdown handling

**Geohash Precision Levels:**

| Precision | Approximate Size | Use Case |
|-----------|------------------|----------|
| 4 chars   | ~20km x ~20km    | City zone |
| 5 chars   | ~2.4km x ~2.4km  | Neighborhood |
| 6 chars   | ~0.6km x ~0.6km  | Block level (default) |
| 7 chars   | ~0.15km x ~0.15km| Street level |
| 8 chars   | ~0.04km x ~0.04km| Building level |

## Setup Instructions

### Prerequisites
```bash
# Install Python dependencies
pip install -r requirements.txt

# Or install individually
pip install redis python-geohash requests faker
```

### Step 1: Start Redis

Redis is embedded in the Java app (dev mode), or use external Redis:
```bash
# Using Docker
docker run -d -p 6379:6379 redis:7-alpine

# Or use embedded Redis (already configured in Spring app)
```

### Step 2: Start Java Application

```bash
cd ride-match
mvn spring-boot:run
```

The application will start on port 8080 with embedded Redis on port 6379.

### Step 3: Start Python Listener

```bash
# Basic usage
./redis_ride_listener.py

# With custom options
./redis_ride_listener.py --precision 7 --host localhost --port 6379

# Run geohash demonstration
./redis_ride_listener.py --demo
```

### Step 4: Generate Test Rides

In another terminal:
```bash
./generate_rides.py
```

## Usage Examples

### Running the Geohash Demo

```bash
$ ./redis_ride_listener.py --demo

============================================================
GEOHASH ZONE DEMONSTRATION
============================================================

Union Square, SF: (37.7879, -122.4075)
  Precision 4: 9q8y
  Precision 5: 9q8yy
  Precision 6: 9q8yym
  Precision 7: 9q8yym0

Fisherman's Wharf: (37.808, -122.4177)
  Precision 4: 9q8y
  Precision 5: 9q8yy
  Precision 6: 9q8yym
  Precision 7: 9q8yym1

============================================================
Note: Same prefix = same zone
  4 chars = city-level zone
  6 chars = block-level zone (default)
============================================================
```

### Listener Output

```bash
$ ./redis_ride_listener.py

2024-01-15 10:30:45,123 - __main__ - INFO - Connected to Redis at localhost:6379
2024-01-15 10:30:45,125 - __main__ - INFO - Subscribed to channel: ride:requested
2024-01-15 10:30:45,126 - __main__ - INFO - Listening for ride events on channel 'ride:requested'...
2024-01-15 10:30:45,126 - __main__ - INFO - Geohash precision: 6 chars (~0.6km (block level))

2024-01-15 10:32:10,456 - __main__ - INFO - PROCESSED: Ride 1 (37.784512, -122.405623) Zone=9q8yym - Fare=$42.50
2024-01-15 10:32:10,789 - __main__ - INFO - PROCESSED: Ride 2 (37.771234, -122.428901) Zone=9q8yym - Fare=$18.75
2024-01-15 10:32:11,123 - __main__ - INFO - PROCESSED: Ride 3 (37.798765, -122.412345) Zone=9q8yyk - Fare=$35.00
...
```

### Output Files

The listener creates:
- `ride_events.jsonl` - One JSON object per line with all ride data and computed zones

Example `ride_events.jsonl`:
```json
{"ride_id": 1, "customer_id": 5, "pickup_lat": 37.784512, "pickup_lng": -122.405623, "computed_zone_id": "9q8yym", "geo_hash_from_java": "9q8yym0kpr0t", ...}
{"ride_id": 2, "customer_id": 8, "pickup_lat": 37.771234, "pickup_lng": -122.428901, "computed_zone_id": "9q8yym", "geo_hash_from_java": "9q8yym1abc2d", ...}
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `REDIS_HOST` | `localhost` | Redis server hostname |
| `REDIS_PORT` | `6379` | Redis server port |
| `REDIS_DB` | `0` | Redis database number |
| `REDIS_CHANNEL` | `ride:requested` | Pub/sub channel to subscribe |
| `GEOHASH_PRECISION` | `6` | Geohash precision (4-8 chars) |

### Java Application Properties

In `application.yml`:
```yaml
ride:
  assignment:
    max-search-radius-km: 15  # Should match geohash precision
```

## Programmatic Usage

### Using the Python Module in Your Code

```python
from redis_ride_listener import RedisRideListener, RideEvent

# Create custom listener
listener = RedisRideListener(
    host='localhost',
    port=6379,
    geohash_precision=7  # Street-level precision
)

# Register custom handler
def my_handler(event: RideEvent):
    print(f"Ride {event.ride_id} in zone {event.computed_zone_id}")
    # Send to analytics, assign driver, etc.

listener.processor.register_handler(my_handler)

# Connect and listen
if listener.connect() and listener.subscribe():
    listener.listen()  # Blocking
```

### Computing Geohash Manually

```python
import geohash

# Encode coordinates to geohash
zone = geohash.encode(37.7749, -122.4194, precision=6)
print(zone)  # Output: "9q8yym"

# Decode geohash back to coordinates (center of zone)
lat, lng = geohash.decode("9q8yym")
print(f"Center: {lat}, {lng}")

# Get neighbors (adjacent zones)
neighbors = geohash.expand("9q8yym")
print(neighbors)  # ['9q8yym', '9q8yyt', '9q8yyk', ...]
```

## Testing the Integration

### End-to-End Test

```bash
# Terminal 1: Start Java app
cd ride-match && mvn spring-boot:run

# Terminal 2: Start Python listener
./redis_ride_listener.py

# Terminal 3: Generate rides
./generate_rides.py --count 10
```

Expected output in Terminal 2:
```
2024-01-15 10:35:01,234 - INFO - PROCESSED: Ride 1 (37.784512, -122.405623) Zone=9q8yym - Fare=$42.50
2024-01-15 10:35:01,567 - INFO - PROCESSED: Ride 2 (37.771234, -122.428901) Zone=9q8yym - Fare=$18.75
...
```

## Troubleshooting

### Connection Refused
```
redis.exceptions.ConnectionError: Error 111 connecting to localhost:6379
```
- Ensure Redis is running (embedded or external)
- Check `REDIS_HOST` and `REDIS_PORT` environment variables

### No Messages Received
- Verify Java app is publishing to the correct channel (`ride:requested`)
- Check that Python listener is subscribed to the same channel
- Look for JSON parse errors in logs

### Geohash Computation Fails
- Ensure `pickupLat` and `pickupLng` are present in the Redis message
- Check that coordinates are valid (lat: -90 to 90, lng: -180 to 180)

## API Reference

### RideEvent Class

```python
@dataclass
class RideEvent:
    ride_id: int
    customer_id: int
    start_location_id: int
    drop_location_id: int
    upfront_fare: float
    pickup_lat: Optional[float]
    pickup_lng: Optional[float]
    geo_hash: Optional[str]         # From Java app
    computed_zone_id: Optional[str] # Computed by python-geohash
    received_at: Optional[datetime]
    
    def compute_geohash_zone(self, precision: int = 6) -> Optional[str]
    def to_dict(self) -> Dict
```

### RedisRideListener Class

```python
class RedisRideListener:
    def __init__(self, host='localhost', port=6379, db=0, 
                 channel='ride:requested', geohash_precision=6)
    def connect() -> bool
    def subscribe() -> bool
    def listen()  # Blocking
    def stop()
    def process_message(message)
```
