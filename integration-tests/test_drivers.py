"""
Integration tests for the Driver management endpoints.

Endpoints under test:
  POST /api/v1/drivers
  GET  /api/v1/drivers/{id}
"""

import pytest
import uuid


class TestCreateDriver:
    """Tests for POST /api/v1/drivers."""

    def test_create_driver_success(self, http_session, base_url, create_user, create_location, create_vehicle):
        """Creating a driver with a valid user and vehicle should return 201."""
        user = create_user(phone=uuid.uuid4().hex[:10])
        loc = create_location()
        vehicle = create_vehicle(location_id=loc["id"])

        payload = {"userId": user["id"], "vehicleId": vehicle["id"]}
        resp = http_session.post(f"{base_url}/drivers", json=payload)

        assert resp.status_code == 201
        body = resp.json()
        assert body["id"] is not None
        assert body["userId"] == user["id"]
        assert body["vehicleId"] == vehicle["id"]


class TestGetDriver:
    """Tests for GET /api/v1/drivers/{id}."""

    def test_get_driver_by_id(
        self, http_session, base_url, create_user, create_location, create_vehicle, create_driver
    ):
        """Fetching an existing driver should return 200 with correct data."""
        user = create_user(phone=uuid.uuid4().hex[:10])
        loc = create_location()
        vehicle = create_vehicle(location_id=loc["id"])
        driver = create_driver(user_id=user["id"], vehicle_id=vehicle["id"])

        resp = http_session.get(f"{base_url}/drivers/{driver['id']}")
        assert resp.status_code == 200
        body = resp.json()
        assert body["id"] == driver["id"]
        assert body["userId"] == user["id"]

    def test_get_nonexistent_driver_returns_404(self, http_session, base_url):
        """Fetching a non-existent driver should return 404."""
        resp = http_session.get(f"{base_url}/drivers/999999")
        assert resp.status_code == 404
