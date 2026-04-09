"""
Integration tests for the User management endpoints.

Endpoints under test:
  POST /api/v1/users
  GET  /api/v1/users/{id}
"""

import pytest


class TestCreateUser:
    """Tests for POST /api/v1/users."""

    def test_create_user_success(self, http_session, base_url):
        """Creating a user with a valid phone number should return 201."""
        payload = {"phone": "1234567890"}
        resp = http_session.post(f"{base_url}/users", json=payload)

        assert resp.status_code == 201
        body = resp.json()
        assert body["id"] is not None
        assert body["phone"] == "1234567890"

    def test_create_user_without_phone(self, http_session, base_url):
        """Creating a user without a phone should still succeed (phone is optional)."""
        resp = http_session.post(f"{base_url}/users", json={})

        # The DTO has no @NotNull on phone, so this should succeed
        assert resp.status_code == 201
        body = resp.json()
        assert body["id"] is not None

    def test_create_multiple_users(self, http_session, base_url):
        """Multiple users can be created, each gets a unique ID."""
        ids = set()
        for i in range(3):
            resp = http_session.post(f"{base_url}/users", json={"phone": f"555000{i}"})
            assert resp.status_code == 201
            ids.add(resp.json()["id"])

        assert len(ids) == 3, "Each user must receive a unique ID"


class TestGetUser:
    """Tests for GET /api/v1/users/{id}."""

    def test_get_user_by_id(self, http_session, base_url, create_user):
        """Fetching an existing user by ID should return 200 with correct data."""
        user = create_user(phone="7777777777")

        resp = http_session.get(f"{base_url}/users/{user['id']}")
        assert resp.status_code == 200
        body = resp.json()
        assert body["id"] == user["id"]
        assert body["phone"] == "7777777777"

    def test_get_nonexistent_user_returns_404(self, http_session, base_url):
        """Fetching a non-existent user should return 404."""
        resp = http_session.get(f"{base_url}/users/999999")
        assert resp.status_code == 404
