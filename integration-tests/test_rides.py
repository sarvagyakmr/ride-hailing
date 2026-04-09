"""
Integration tests for the Ride management endpoints.

Endpoints under test:
  POST  /api/v1/rides                       – Request a ride
  GET   /api/v1/rides/{id}                  – Get ride details
  PATCH /api/v1/rides/{id}/complete          – Complete a ride
  POST  /api/v1/rides/offers/{id}/accept     – Driver accepts an offer
  POST  /api/v1/rides/offers/{id}/reject     – Driver rejects an offer
"""

import time
import pytest


class TestCreateRide:
    """Tests for POST /api/v1/rides."""

    def test_create_ride_success(self, http_session, base_url, create_customer, create_location):
        """Requesting a ride with valid data should return 201 with REQUESTED status."""
        customer = create_customer()
        pickup = create_location(lat=12.9716, lng=77.5946)
        drop = create_location(lat=13.0200, lng=77.6500)

        payload = {
            "customerId": customer["id"],
            "startLocationId": pickup["id"],
            "dropLocationId": drop["id"],
            "upfrontFare": 250.00,
        }
        resp = http_session.post(f"{base_url}/rides", json=payload)

        assert resp.status_code == 201
        body = resp.json()
        assert body["id"] is not None
        assert body["customerId"] == customer["id"]
        assert body["startLocationId"] == pickup["id"]
        assert body["dropLocationId"] == drop["id"]
        assert body["status"] == "REQUESTED"
        assert float(body["upfrontFare"]) == pytest.approx(250.00, abs=0.01)
        # Driver and vehicle should not be assigned yet
        assert body["driverId"] is None
        assert body["vehicleId"] is None

    def test_create_ride_stores_all_fields(self, http_session, base_url, create_customer, create_location):
        """All fields from the ride request should be persisted and retrievable."""
        customer = create_customer()
        pickup = create_location(lat=28.6139, lng=77.2090)
        drop = create_location(lat=28.7041, lng=77.1025)

        payload = {
            "customerId": customer["id"],
            "startLocationId": pickup["id"],
            "dropLocationId": drop["id"],
            "upfrontFare": 99.50,
        }
        create_resp = http_session.post(f"{base_url}/rides", json=payload)
        assert create_resp.status_code == 201
        ride_id = create_resp.json()["id"]

        # Fetch the ride and verify
        get_resp = http_session.get(f"{base_url}/rides/{ride_id}")
        assert get_resp.status_code == 200
        body = get_resp.json()
        assert body["customerId"] == customer["id"]
        assert body["startLocationId"] == pickup["id"]
        assert body["dropLocationId"] == drop["id"]
        assert float(body["upfrontFare"]) == pytest.approx(99.50, abs=0.01)


class TestGetRide:
    """Tests for GET /api/v1/rides/{id}."""

    def test_get_ride_by_id(self, http_session, base_url, create_customer, create_location, create_ride):
        """Fetching an existing ride should return 200 with correct data."""
        customer = create_customer()
        pickup = create_location()
        drop = create_location(lat=13.0, lng=77.6)
        ride = create_ride(
            customer_id=customer["id"],
            start_location_id=pickup["id"],
            drop_location_id=drop["id"],
        )

        resp = http_session.get(f"{base_url}/rides/{ride['id']}")
        assert resp.status_code == 200
        assert resp.json()["id"] == ride["id"]

    def test_get_nonexistent_ride_returns_404(self, http_session, base_url):
        """Fetching a non-existent ride should return 404."""
        resp = http_session.get(f"{base_url}/rides/999999")
        assert resp.status_code == 404


class TestCompleteRide:
    """Tests for PATCH /api/v1/rides/{id}/complete."""

    def test_complete_ride_not_in_route_fails(
        self, http_session, base_url, create_customer, create_location, create_ride
    ):
        """Completing a ride that is in REQUESTED status (not IN_ROUTE) should fail."""
        customer = create_customer()
        pickup = create_location()
        drop = create_location(lat=13.0, lng=77.6)
        ride = create_ride(
            customer_id=customer["id"],
            start_location_id=pickup["id"],
            drop_location_id=drop["id"],
        )

        # Ride is REQUESTED, so completing it should return 404 (empty optional)
        resp = http_session.patch(f"{base_url}/rides/{ride['id']}/complete")
        assert resp.status_code == 404

    def test_complete_nonexistent_ride_returns_404(self, http_session, base_url):
        """Completing a non-existent ride should return 404."""
        resp = http_session.patch(f"{base_url}/rides/999999/complete")
        assert resp.status_code == 404


class TestRideOfferAccept:
    """Tests for POST /api/v1/rides/offers/{offerId}/accept."""

    def test_accept_nonexistent_offer_fails(self, http_session, base_url):
        """Accepting a non-existent offer should return CONFLICT (409)."""
        payload = {"driverId": 1}
        resp = http_session.post(
            f"{base_url}/rides/offers/999999/accept",
            json=payload,
        )
        assert resp.status_code == 409


class TestRideOfferReject:
    """Tests for POST /api/v1/rides/offers/{offerId}/reject."""

    def test_reject_nonexistent_offer_fails(self, http_session, base_url):
        """Rejecting a non-existent offer should return BAD_REQUEST (400)."""
        payload = {"driverId": 1, "reason": "Too far"}
        resp = http_session.post(
            f"{base_url}/rides/offers/999999/reject",
            json=payload,
        )
        assert resp.status_code == 400
