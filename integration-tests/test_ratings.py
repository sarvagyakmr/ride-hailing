"""
Integration tests for the Rating management endpoints.

Endpoints under test:
  POST /api/v1/ratings
  GET  /api/v1/ratings/{id}
  GET  /api/v1/ratings/ride/{rideId}
  GET  /api/v1/ratings/driver/{driverId}
"""

import pytest


class TestCreateRating:
    """Tests for POST /api/v1/ratings."""

    def test_create_rating_for_non_completed_ride_fails(
        self, http_session, base_url, create_customer, create_location, create_ride
    ):
        """Rating a ride that is NOT in COMPLETE status should fail with an error."""
        customer = create_customer()
        pickup = create_location()
        drop = create_location(lat=13.0, lng=77.6)
        ride = create_ride(
            customer_id=customer["id"],
            start_location_id=pickup["id"],
            drop_location_id=drop["id"],
        )

        payload = {"rideId": ride["id"], "score": 4.5, "feedback": "Good ride"}
        resp = http_session.post(f"{base_url}/ratings", json=payload)

        # Should fail – ride is in REQUESTED status, not COMPLETE
        assert resp.status_code in (400, 500), f"Expected 4xx/5xx but got {resp.status_code}"

    def test_create_rating_for_nonexistent_ride_fails(self, http_session, base_url):
        """Rating a ride that doesn't exist should fail."""
        payload = {"rideId": 999999, "score": 5.0, "feedback": "Great!"}
        resp = http_session.post(f"{base_url}/ratings", json=payload)

        assert resp.status_code in (400, 500)

    def test_create_rating_invalid_score_below_range(self, http_session, base_url):
        """Score below 1 should fail validation."""
        payload = {"rideId": 1, "score": 0, "feedback": "Bad"}
        resp = http_session.post(f"{base_url}/ratings", json=payload)

        assert resp.status_code == 400

    def test_create_rating_invalid_score_above_range(self, http_session, base_url):
        """Score above 5 should fail validation."""
        payload = {"rideId": 1, "score": 6, "feedback": "Too high"}
        resp = http_session.post(f"{base_url}/ratings", json=payload)

        assert resp.status_code == 400

    def test_create_rating_missing_ride_id(self, http_session, base_url):
        """Rating without rideId should fail validation (rideId is @NotNull)."""
        payload = {"score": 4.0}
        resp = http_session.post(f"{base_url}/ratings", json=payload)

        assert resp.status_code == 400

    def test_create_rating_missing_score(self, http_session, base_url):
        """Rating without score should fail validation (score is @NotNull)."""
        payload = {"rideId": 1}
        resp = http_session.post(f"{base_url}/ratings", json=payload)

        assert resp.status_code == 400


class TestGetRating:
    """Tests for GET /api/v1/ratings/{id}."""

    def test_get_nonexistent_rating_returns_404(self, http_session, base_url):
        """Fetching a non-existent rating should return 404."""
        resp = http_session.get(f"{base_url}/ratings/999999")
        assert resp.status_code == 404


class TestGetRatingByRide:
    """Tests for GET /api/v1/ratings/ride/{rideId}."""

    def test_get_rating_for_unrated_ride_returns_404(self, http_session, base_url):
        """Getting a rating for a ride that has no rating should return 404."""
        resp = http_session.get(f"{base_url}/ratings/ride/999999")
        assert resp.status_code == 404


class TestGetRatingsByDriver:
    """Tests for GET /api/v1/ratings/driver/{driverId}."""

    def test_get_ratings_for_driver_with_no_ratings(self, http_session, base_url):
        """Getting ratings for a driver with no ratings should return 200 with empty list."""
        resp = http_session.get(f"{base_url}/ratings/driver/999999")
        assert resp.status_code == 200
        assert resp.json() == []
