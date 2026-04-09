"""
Integration tests for the Location management endpoints.

Endpoints under test:
  POST /api/v1/locations
  GET  /api/v1/locations/{id}
"""

import pytest


class TestCreateLocation:
    """Tests for POST /api/v1/locations."""

    def test_create_location_success(self, http_session, base_url):
        """Creating a location with valid coordinates should return 201."""
        payload = {"lat": 12.9716, "lng": 77.5946, "geoHash": "tdr1y0"}
        resp = http_session.post(f"{base_url}/locations", json=payload)

        assert resp.status_code == 201
        body = resp.json()
        assert body["id"] is not None
        assert float(body["lat"]) == pytest.approx(12.9716, abs=0.001)
        assert float(body["lng"]) == pytest.approx(77.5946, abs=0.001)
        assert body["geoHash"] == "tdr1y0"

    def test_create_location_negative_coordinates(self, http_session, base_url):
        """Locations with negative coordinates (southern/western hemisphere) should work."""
        payload = {"lat": -33.8688, "lng": -151.2093, "geoHash": "r3gx2f"}
        resp = http_session.post(f"{base_url}/locations", json=payload)

        assert resp.status_code == 201
        body = resp.json()
        assert float(body["lat"]) == pytest.approx(-33.8688, abs=0.001)
        assert float(body["lng"]) == pytest.approx(-151.2093, abs=0.001)

    def test_create_location_without_geohash(self, http_session, base_url):
        """Creating a location without geoHash should still succeed."""
        payload = {"lat": 40.7128, "lng": -74.0060}
        resp = http_session.post(f"{base_url}/locations", json=payload)

        assert resp.status_code == 201
        body = resp.json()
        assert body["id"] is not None


class TestGetLocation:
    """Tests for GET /api/v1/locations/{id}."""

    def test_get_location_by_id(self, http_session, base_url, create_location):
        """Fetching an existing location should return correct data."""
        loc = create_location(lat=28.6139, lng=77.2090, geo_hash="ttncg9")

        resp = http_session.get(f"{base_url}/locations/{loc['id']}")
        assert resp.status_code == 200
        body = resp.json()
        assert body["id"] == loc["id"]
        assert float(body["lat"]) == pytest.approx(28.6139, abs=0.001)

    def test_get_nonexistent_location_returns_404(self, http_session, base_url):
        """Fetching a non-existent location should return 404."""
        resp = http_session.get(f"{base_url}/locations/999999")
        assert resp.status_code == 404
