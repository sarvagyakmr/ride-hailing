"""
Pytest configuration and shared fixtures for ride-match integration tests.

This module provides:
- Base URL configuration for the ride-match service
- Reusable HTTP session fixture
- Common helper fixtures for creating test entities
"""

import pytest
import requests
import time
import uuid

BASE_URL = "http://localhost:8081/api/v1"


@pytest.fixture(scope="session")
def base_url():
    """Base URL for the ride-match service."""
    return BASE_URL


@pytest.fixture(scope="session")
def http_session():
    """Reusable HTTP session with default headers."""
    session = requests.Session()
    session.headers.update({
        "Content-Type": "application/json",
        "Accept": "application/json",
    })
    return session


@pytest.fixture(scope="session", autouse=True)
def wait_for_service(http_session, base_url):
    """Wait for the ride-match service to be ready before running tests."""
    health_url = f"{base_url}/health"
    max_retries = 30
    retry_interval = 2

    for attempt in range(max_retries):
        try:
            resp = http_session.get(health_url, timeout=5)
            if resp.status_code == 200:
                print(f"\n✅ ride-match service is ready (attempt {attempt + 1})")
                return
        except requests.ConnectionError:
            pass

        if attempt < max_retries - 1:
            print(f"⏳ Waiting for ride-match service... (attempt {attempt + 1}/{max_retries})")
            time.sleep(retry_interval)

    pytest.exit("❌ ride-match service did not become ready in time", returncode=1)


# ---------------------------------------------------------------------------
# Helper fixtures for creating commonly needed test entities
# ---------------------------------------------------------------------------

@pytest.fixture
def create_user(http_session, base_url):
    """Factory fixture – returns a function that creates a user."""
    def _create(phone=None):
        if phone is None:
            phone = uuid.uuid4().hex[:10]
        resp = http_session.post(f"{base_url}/users", json={"phone": phone})
        assert resp.status_code == 201, f"Failed to create user: {resp.text}"
        return resp.json()
    return _create


@pytest.fixture
def create_customer(http_session, base_url):
    """Factory fixture – returns a function that creates a customer."""
    def _create():
        resp = http_session.post(f"{base_url}/customers")
        assert resp.status_code == 201, f"Failed to create customer: {resp.text}"
        return resp.json()
    return _create


@pytest.fixture
def create_location(http_session, base_url):
    """Factory fixture – returns a function that creates a location."""
    def _create(lat=12.9716, lng=77.5946, geo_hash=None):
        if geo_hash is None:
            geo_hash = uuid.uuid4().hex[:6]
        payload = {"lat": lat, "lng": lng, "geoHash": geo_hash}
        resp = http_session.post(f"{base_url}/locations", json=payload)
        assert resp.status_code == 201, f"Failed to create location: {resp.text}"
        return resp.json()
    return _create


@pytest.fixture
def create_vehicle(http_session, base_url):
    """Factory fixture – returns a function that creates a vehicle."""
    def _create(location_id, status="AVAILABLE"):
        payload = {"locationId": location_id, "status": status}
        resp = http_session.post(f"{base_url}/vehicles", json=payload)
        assert resp.status_code == 201, f"Failed to create vehicle: {resp.text}"
        return resp.json()
    return _create


@pytest.fixture
def create_driver(http_session, base_url):
    """Factory fixture – returns a function that creates a driver."""
    def _create(user_id, vehicle_id):
        payload = {"userId": user_id, "vehicleId": vehicle_id}
        resp = http_session.post(f"{base_url}/drivers", json=payload)
        assert resp.status_code == 201, f"Failed to create driver: {resp.text}"
        return resp.json()
    return _create


@pytest.fixture
def create_ride(http_session, base_url):
    """Factory fixture – returns a function that creates a ride."""
    def _create(customer_id, start_location_id, drop_location_id, upfront_fare=150.00):
        payload = {
            "customerId": customer_id,
            "startLocationId": start_location_id,
            "dropLocationId": drop_location_id,
            "upfrontFare": upfront_fare,
        }
        resp = http_session.post(f"{base_url}/rides", json=payload)
        assert resp.status_code == 201, f"Failed to create ride: {resp.text}"
        return resp.json()
    return _create


@pytest.fixture
def setup_full_ride_ecosystem(
    create_user, create_customer, create_location, create_vehicle, create_driver
):
    """
    High-level fixture that sets up a complete ecosystem:
      - 1 customer
      - 1 driver (user → vehicle → driver)
      - pickup & drop locations

    Returns a dict with all created entity IDs.
    """
    def _setup(
        driver_lat=12.9716, driver_lng=77.5946,
        pickup_lat=12.9750, pickup_lng=77.5900,
        drop_lat=13.0200, drop_lng=77.6500,
    ):
        # Customer
        customer = create_customer()

        # Driver side
        user = create_user(phone=uuid.uuid4().hex[:10])
        driver_loc = create_location(lat=driver_lat, lng=driver_lng)
        vehicle = create_vehicle(location_id=driver_loc["id"])
        driver = create_driver(user_id=user["id"], vehicle_id=vehicle["id"])

        # Ride locations
        pickup = create_location(lat=pickup_lat, lng=pickup_lng)
        drop = create_location(lat=drop_lat, lng=drop_lng)

        return {
            "customer": customer,
            "user": user,
            "driver": driver,
            "vehicle": vehicle,
            "driver_location": driver_loc,
            "pickup_location": pickup,
            "drop_location": drop,
        }
    return _setup
