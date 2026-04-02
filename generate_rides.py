#!/usr/bin/env python3
"""
Ride Hailing Test Data Generator

This script generates test data for the ride-hailing application by:
1. Creating users (for drivers)
2. Creating customers
3. Creating locations (pickup and dropoff points)
4. Creating vehicles
5. Creating drivers (linked to users and vehicles)
6. Creating rides (linked to customers and locations)

Logs all ride IDs with their requested location coordinates.
"""

import requests
import random
import time
import logging
from faker import Faker
from typing import Dict, List, Tuple, Optional
from dataclasses import dataclass

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('ride_generation.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

# Configuration
BASE_URL = "http://localhost:8080/api/v1"
NUM_RIDES = 100
MAX_SEARCH_RADIUS_KM = 15  # Should match app config

# Initialize Faker
fake = Faker()

# Store created entity IDs for reuse
@dataclass
class EntityPool:
    users: List[int] = None
    customers: List[int] = None
    locations: List[int] = None
    vehicles: List[int] = None
    drivers: List[int] = None
    rides: List[Dict] = None

    def __post_init__(self):
        if self.users is None:
            self.users = []
        if self.customers is None:
            self.customers = []
        if self.locations is None:
            self.locations = []
        if self.vehicles is None:
            self.vehicles = []
        if self.drivers is None:
            self.drivers = []
        if self.rides is None:
            self.rides = []


class RideHailingAPI:
    """Client for the Ride Hailing Java API"""

    def __init__(self, base_url: str):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        })

    def _post(self, endpoint: str, data: Dict) -> Optional[Dict]:
        """Make POST request and return JSON response"""
        try:
            response = self.session.post(f"{self.base_url}{endpoint}", json=data)
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            logger.error(f"POST {endpoint} failed: {e}")
            return None

    def _get(self, endpoint: str) -> Optional[Dict]:
        """Make GET request and return JSON response"""
        try:
            response = self.session.get(f"{self.base_url}{endpoint}")
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            logger.error(f"GET {endpoint} failed: {e}")
            return None

    def create_user(self, phone: str) -> Optional[int]:
        """Create a user and return the user ID"""
        data = {"phone": phone}
        response = self._post("/users", data)
        if response:
            user_id = response.get("id")
            logger.debug(f"Created user {user_id} with phone {phone}")
            return user_id
        return None

    def create_customer(self) -> Optional[int]:
        """Create a customer and return the customer ID"""
        response = self._post("/customers", {})
        if response:
            customer_id = response.get("id")
            logger.debug(f"Created customer {customer_id}")
            return customer_id
        return None

    def create_location(self, lat: float, lng: float, geo_hash: str) -> Optional[int]:
        """Create a location and return the location ID"""
        data = {
            "lat": round(lat, 8),
            "lng": round(lng, 8),
            "geoHash": geo_hash
        }
        response = self._post("/locations", data)
        if response:
            location_id = response.get("id")
            logger.debug(f"Created location {location_id} at ({lat}, {lng})")
            return location_id
        return None

    def create_vehicle(self, location_id: int, status: str = "AVAILABLE") -> Optional[int]:
        """Create a vehicle and return the vehicle ID"""
        data = {
            "locationId": location_id,
            "status": status
        }
        response = self._post("/vehicles", data)
        if response:
            vehicle_id = response.get("id")
            logger.debug(f"Created vehicle {vehicle_id} at location {location_id}")
            return vehicle_id
        return None

    def create_driver(self, user_id: int, vehicle_id: int) -> Optional[int]:
        """Create a driver and return the driver ID"""
        data = {
            "userId": user_id,
            "vehicleId": vehicle_id
        }
        response = self._post("/drivers", data)
        if response:
            driver_id = response.get("id")
            logger.debug(f"Created driver {driver_id} for user {user_id}, vehicle {vehicle_id}")
            return driver_id
        return None

    def create_ride(self, customer_id: int, start_location_id: int,
                    drop_location_id: int, upfront_fare: float) -> Optional[Dict]:
        """Create a ride and return the ride data"""
        data = {
            "customerId": customer_id,
            "startLocationId": start_location_id,
            "dropLocationId": drop_location_id,
            "upfrontFare": round(upfront_fare, 2)
        }
        response = self._post("/rides", data)
        if response:
            ride_id = response.get("id")
            logger.debug(f"Created ride {ride_id} for customer {customer_id}")
            return response
        return None

    def get_location(self, location_id: int) -> Optional[Dict]:
        """Get location details by ID"""
        return self._get(f"/locations/{location_id}")


class TestDataGenerator:
    """Generates test data for the ride-hailing application"""

    def __init__(self, api: RideHailingAPI, entity_pool: EntityPool):
        self.api = api
        self.pool = entity_pool
        # Center point for generating clustered locations (e.g., San Francisco)
        self.center_lat = 37.7749
        self.center_lng = -122.4194

    def generate_phone_number(self) -> str:
        """Generate a unique phone number"""
        return f"+1{fake.numerify('##########')}"

    def generate_nearby_coordinates(self) -> Tuple[float, float]:
        """
        Generate coordinates within MAX_SEARCH_RADIUS_KM of center point.
        Uses approximate conversion: 1 degree ≈ 111 km
        """
        # Generate random offset within ~15km radius
        max_offset_deg = MAX_SEARCH_RADIUS_KM / 111.0

        lat_offset = random.uniform(-max_offset_deg, max_offset_deg)
        lng_offset = random.uniform(-max_offset_deg, max_offset_deg)

        lat = self.center_lat + lat_offset
        lng = self.center_lng + lng_offset

        return round(lat, 8), round(lng, 8)

    def generate_geo_hash(self, lat: float, lng: float) -> str:
        """Generate a simple geohash-like string (for demo purposes)"""
        # In production, use proper geohash library
        import hashlib
        coord_str = f"{lat:.6f},{lng:.6f}"
        return hashlib.md5(coord_str.encode()).hexdigest()[:12]

    def create_user(self) -> Optional[int]:
        """Create a single user"""
        phone = self.generate_phone_number()
        user_id = self.api.create_user(phone)
        if user_id:
            self.pool.users.append(user_id)
        return user_id

    def create_customer(self) -> Optional[int]:
        """Create a single customer"""
        customer_id = self.api.create_customer()
        if customer_id:
            self.pool.customers.append(customer_id)
        return customer_id

    def create_location(self) -> Optional[int]:
        """Create a single location with random nearby coordinates"""
        lat, lng = self.generate_nearby_coordinates()
        geo_hash = self.generate_geo_hash(lat, lng)
        location_id = self.api.create_location(lat, lng, geo_hash)
        if location_id:
            self.pool.locations.append({
                "id": location_id,
                "lat": lat,
                "lng": lng,
                "geoHash": geo_hash
            })
        return location_id

    def create_vehicle(self, location_id: int = None) -> Optional[int]:
        """Create a single vehicle"""
        if location_id is None:
            if not self.pool.locations:
                logger.error("No locations available for vehicle creation")
                return None
            location = random.choice(self.pool.locations)
            location_id = location["id"]

        vehicle_id = self.api.create_vehicle(location_id, "AVAILABLE")
        if vehicle_id:
            self.pool.vehicles.append(vehicle_id)
        return vehicle_id

    def create_driver(self, user_id: int = None, vehicle_id: int = None) -> Optional[int]:
        """Create a single driver linked to user and vehicle"""
        if user_id is None:
            if not self.pool.users:
                logger.error("No users available for driver creation")
                return None
            user_id = random.choice(self.pool.users)

        if vehicle_id is None:
            if not self.pool.vehicles:
                logger.error("No vehicles available for driver creation")
                return None
            vehicle_id = random.choice(self.pool.vehicles)

        driver_id = self.api.create_driver(user_id, vehicle_id)
        if driver_id:
            self.pool.drivers.append(driver_id)
        return driver_id

    def create_ride(self, customer_id: int = None) -> Optional[Dict]:
        """Create a single ride with pickup and dropoff locations"""
        if customer_id is None:
            if not self.pool.customers:
                logger.error("No customers available for ride creation")
                return None
            customer_id = random.choice(self.pool.customers)

        # Create pickup location
        pickup_lat, pickup_lng = self.generate_nearby_coordinates()
        pickup_geo = self.generate_geo_hash(pickup_lat, pickup_lng)
        pickup_id = self.api.create_location(pickup_lat, pickup_lng, pickup_geo)

        if not pickup_id:
            logger.error("Failed to create pickup location")
            return None

        # Create dropoff location
        dropoff_lat, dropoff_lng = self.generate_nearby_coordinates()
        dropoff_geo = self.generate_geo_hash(dropoff_lat, dropoff_lng)
        dropoff_id = self.api.create_location(dropoff_lat, dropoff_lng, dropoff_geo)

        if not dropoff_id:
            logger.error("Failed to create dropoff location")
            return None

        # Generate random upfront fare between $5 and $100
        upfront_fare = random.uniform(5.0, 100.0)

        ride = self.api.create_ride(customer_id, pickup_id, dropoff_id, upfront_fare)
        if ride:
            ride_data = {
                "rideId": ride.get("id"),
                "customerId": customer_id,
                "pickupLocationId": pickup_id,
                "pickupLat": pickup_lat,
                "pickupLng": pickup_lng,
                "dropoffLocationId": dropoff_id,
                "dropoffLat": dropoff_lat,
                "dropoffLng": dropoff_lng,
                "upfrontFare": round(upfront_fare, 2),
                "status": ride.get("status")
            }
            self.pool.rides.append(ride_data)
            return ride_data
        return None


def setup_prerequisites(api: RideHailingAPI, generator: TestDataGenerator) -> bool:
    """
    Create prerequisite entities needed for rides:
    - Users (for drivers)
    - Customers
    - Locations (for vehicles)
    - Vehicles
    - Drivers
    """
    logger.info("Setting up prerequisite entities...")

    # Create users for drivers (need at least some drivers)
    num_drivers_needed = max(10, NUM_RIDES // 5)  # At least 1 driver per 5 rides
    logger.info(f"Creating {num_drivers_needed} users for drivers...")
    for i in range(num_drivers_needed):
        user_id = generator.create_user()
        if user_id:
            logger.info(f"  Created user {i+1}/{num_drivers_needed}: ID {user_id}")
        else:
            logger.error(f"  Failed to create user {i+1}")
            return False
        time.sleep(0.05)  # Small delay to avoid overwhelming the server

    # Create customers
    num_customers_needed = max(20, NUM_RIDES // 3)  # Multiple rides per customer
    logger.info(f"Creating {num_customers_needed} customers...")
    for i in range(num_customers_needed):
        customer_id = generator.create_customer()
        if customer_id:
            logger.info(f"  Created customer {i+1}/{num_customers_needed}: ID {customer_id}")
        else:
            logger.error(f"  Failed to create customer {i+1}")
            return False
        time.sleep(0.05)

    # Create locations and vehicles for drivers
    logger.info(f"Creating {num_drivers_needed} locations and vehicles...")
    for i in range(num_drivers_needed):
        # Create location for vehicle
        location_id = generator.create_location()
        if not location_id:
            logger.error(f"  Failed to create location {i+1}")
            return False

        # Create vehicle at that location
        vehicle_id = generator.create_vehicle(location_id)
        if not vehicle_id:
            logger.error(f"  Failed to create vehicle {i+1}")
            return False

        # Create driver linking user and vehicle
        user_id = generator.pool.users[i]
        driver_id = generator.create_driver(user_id, vehicle_id)
        if driver_id:
            logger.info(f"  Created driver {i+1}/{num_drivers_needed}: ID {driver_id}")
        else:
            logger.error(f"  Failed to create driver {i+1}")
            return False

        time.sleep(0.05)

    logger.info("Prerequisite setup complete!")
    logger.info(f"  Total users: {len(generator.pool.users)}")
    logger.info(f"  Total customers: {len(generator.pool.customers)}")
    logger.info(f"  Total locations: {len(generator.pool.locations)}")
    logger.info(f"  Total vehicles: {len(generator.pool.vehicles)}")
    logger.info(f"  Total drivers: {len(generator.pool.drivers)}")

    return True


def generate_rides(api: RideHailingAPI, generator: TestDataGenerator, num_rides: int) -> bool:
    """Generate the specified number of rides"""
    logger.info(f"Generating {num_rides} rides...")

    for i in range(num_rides):
        ride_data = generator.create_ride()
        if ride_data:
            logger.info(
                f"Ride {i+1}/{num_rides}: ID={ride_data['rideId']}, "
                f"Pickup=({ride_data['pickupLat']:.6f}, {ride_data['pickupLng']:.6f}), "
                f"Fare=${ride_data['upfrontFare']:.2f}"
            )
        else:
            logger.error(f"Failed to create ride {i+1}")

        # Small delay every 10 rides
        if (i + 1) % 10 == 0:
            time.sleep(0.1)

    return True


def log_summary(entity_pool: EntityPool):
    """Log a summary of all created rides with their location coordinates"""
    logger.info("=" * 80)
    logger.info("RIDE GENERATION SUMMARY")
    logger.info("=" * 80)

    logger.info(f"\nTotal Entities Created:")
    logger.info(f"  Users: {len(entity_pool.users)}")
    logger.info(f"  Customers: {len(entity_pool.customers)}")
    logger.info(f"  Locations: {len(entity_pool.locations)}")
    logger.info(f"  Vehicles: {len(entity_pool.vehicles)}")
    logger.info(f"  Drivers: {len(entity_pool.drivers)}")
    logger.info(f"  Rides: {len(entity_pool.rides)}")

    logger.info(f"\nRide Details (ID, Pickup Lat, Pickup Lng):")
    logger.info("-" * 80)
    for ride in entity_pool.rides:
        logger.info(
            f"Ride ID: {ride['rideId']:>5} | "
            f"Pickup: ({ride['pickupLat']:>12.8f}, {ride['pickupLng']:>13.8f}) | "
            f"Dropoff: ({ride['dropoffLat']:>12.8f}, {ride['dropoffLng']:>13.8f}) | "
            f"Fare: ${ride['upfrontFare']:>6.2f}"
        )

    # Also write a CSV-like format for easy parsing
    logger.info("\nCSV Format (ride_id,pickup_lat,pickup_lng,dropoff_lat,dropoff_lng,fare):")
    logger.info("-" * 80)
    for ride in entity_pool.rides:
        logger.info(
            f"{ride['rideId']},{ride['pickupLat']:.8f},{ride['pickupLng']:.8f},"
            f"{ride['dropoffLat']:.8f},{ride['dropoffLng']:.8f},{ride['upfrontFare']:.2f}"
        )

    # Write to a separate file for easy consumption
    with open('rides_summary.csv', 'w') as f:
        f.write("ride_id,pickup_lat,pickup_lng,dropoff_lat,dropoff_lng,fare,status\n")
        for ride in entity_pool.rides:
            f.write(
                f"{ride['rideId']},{ride['pickupLat']:.8f},{ride['pickupLng']:.8f},"
                f"{ride['dropoffLat']:.8f},{ride['dropoffLng']:.8f},{ride['upfrontFare']:.2f},"
                f"{ride['status']}\n"
            )
    logger.info("\nSummary also written to: rides_summary.csv")


def main():
    """Main entry point"""
    logger.info("=" * 80)
    logger.info("Ride Hailing Test Data Generator")
    logger.info("=" * 80)
    logger.info(f"Target: {NUM_RIDES} rides")
    logger.info(f"API Base URL: {BASE_URL}")

    # Initialize API client and entity pool
    api = RideHailingAPI(BASE_URL)
    entity_pool = EntityPool()
    generator = TestDataGenerator(api, entity_pool)

    # Check if server is running
    try:
        response = requests.get(f"{BASE_URL}/health", timeout=5)
        if response.status_code == 200:
            logger.info("✓ API server is running")
        else:
            logger.warning(f"API health check returned status {response.status_code}")
    except requests.exceptions.ConnectionError:
        logger.error("✗ Cannot connect to API server at {BASE_URL}")
        logger.error("Please ensure the Java application is running on port 8080")
        return 1
    except Exception as e:
        logger.warning(f"Health check failed: {e}")

    # Create prerequisite entities
    if not setup_prerequisites(api, generator):
        logger.error("Failed to setup prerequisite entities. Exiting.")
        return 1

    # Generate rides
    if not generate_rides(api, generator, NUM_RIDES):
        logger.error("Failed to generate all rides.")

    # Log summary
    log_summary(entity_pool)

    logger.info("\n" + "=" * 80)
    logger.info("Test data generation complete!")
    logger.info("=" * 80)

    return 0


if __name__ == "__main__":
    exit(main())
