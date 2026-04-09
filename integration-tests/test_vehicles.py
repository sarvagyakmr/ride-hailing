"""
Integration tests for the Vehicle management endpoints.

Endpoints under test:
  POST  /api/v1/vehicles
  GET   /api/v1/vehicles/{id}
  PATCH /api/v1/vehicles/{id}/location
"""

import pytest


class TestCreateVehicle:
    """Tests for POST /api/v1/vehicles."""

    def test_create_vehicle_available(self, http_session, base_url, create_location):
        """Creating a vehicle with AVAILABLE status should return 201."""
        loc = create_location()
        payload = {"locationId": loc["id"], "status": "AVAILABLE"}
        resp = http_session.post(f"{base_url}/vehicles", json=payload)

        assert resp.status_code == 201
        body = resp.json()
        assert body["id"] is not None
        assert body["locationId"] == loc["id"]
        assert body["status"] == "AVAILABLE"

    def test_create_vehicle_in_ride(self, http_session, base_url, create_location):
        """Creating a vehicle with IN_RIDE status should return 201."""
        loc = create_location()
        payload = {"locationId": loc["id"], "status": "IN_RIDE"}
        resp = http_session.post(f"{base_url}/vehicles", json=payload)

        assert resp.status_code == 201
        assert resp.json()["status"] == "IN_RIDE"

    def test_create_vehicle_completing_ride(self, http_session, base_url, create_location):
        """Creating a vehicle with COMPLETING_RIDE status should return 201."""
        loc = create_location()
        payload = {"locationId": loc["id"], "status": "COMPLETING_RIDE"}
        resp = http_session.post(f"{base_url}/vehicles", json=payload)

        assert resp.status_code == 201
        assert resp.json()["status"] == "COMPLETING_RIDE"


class TestGetVehicle:
    """Tests for GET /api/v1/vehicles/{id}."""

    def test_get_vehicle_by_id(self, http_session, base_url, create_location, create_vehicle):
        """Fetching an existing vehicle should return 200 with correct data."""
        loc = create_location()
        vehicle = create_vehicle(location_id=loc["id"])

        resp = http_session.get(f"{base_url}/vehicles/{vehicle['id']}")
        assert resp.status_code == 200
        body = resp.json()
        assert body["id"] == vehicle["id"]
        assert body["locationId"] == loc["id"]
        assert body["status"] == "AVAILABLE"

    def test_get_nonexistent_vehicle_returns_404(self, http_session, base_url):
        """Fetching a non-existent vehicle should return 404."""
        resp = http_session.get(f"{base_url}/vehicles/999999")
        assert resp.status_code == 404


class TestUpdateVehicleLocation:
    """Tests for PATCH /api/v1/vehicles/{id}/location."""

    def test_update_vehicle_location(self, http_session, base_url, create_location, create_vehicle):
        """Updating a vehicle's location should succeed and reflect the new location."""
        old_loc = create_location(lat=12.9716, lng=77.5946)
        vehicle = create_vehicle(location_id=old_loc["id"])

        new_loc = create_location(lat=13.0350, lng=77.5970)
        resp = http_session.patch(
            f"{base_url}/vehicles/{vehicle['id']}/location",
            json={"locationId": new_loc["id"]},
        )

        assert resp.status_code == 200
        body = resp.json()
        assert body["locationId"] == new_loc["id"]

    def test_update_nonexistent_vehicle_location_returns_404(self, http_session, base_url, create_location):
        """Updating location of a non-existent vehicle should return 404."""
        loc = create_location()
        resp = http_session.patch(
            f"{base_url}/vehicles/999999/location",
            json={"locationId": loc["id"]},
        )
        assert resp.status_code == 404
