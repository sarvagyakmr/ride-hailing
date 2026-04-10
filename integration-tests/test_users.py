"""
Integration tests for the User management endpoints.

Endpoints under test:
  POST /api/v1/users
  GET  /api/v1/users/{id}
"""

import pytest
import uuid


class TestCreateUser:
    """Tests for POST /api/v1/users."""

    def test_create_user_success(self, http_session, base_url):
        """Creating a user with a valid phone number should return 201."""
        phone = uuid.uuid4().hex[:10]
        payload = {"phone": phone}
        resp = http_session.post(f"{base_url}/users", json=payload)

        assert resp.status_code == 201
        body = resp.json()
        assert body["id"] is not None
        assert body["phone"] == phone

    def test_create_user_without_phone(self, http_session, base_url):
        """Creating a user without a phone should fail (phone is NOT NULL)."""
        resp = http_session.post(f"{base_url}/users", json={})

        # The entity has nullable=false on phone, so this should fail
        assert resp.status_code in (400, 500)

    def test_create_multiple_users(self, http_session, base_url):
        """Multiple users can be created, each gets a unique ID."""
        ids = set()
        for i in range(3):
            resp = http_session.post(f"{base_url}/users", json={"phone": uuid.uuid4().hex[:10]})
            assert resp.status_code == 201
            ids.add(resp.json()["id"])

        assert len(ids) == 3, "Each user must receive a unique ID"


class TestGetUser:
    """Tests for GET /api/v1/users/{id}."""

    def test_get_user_by_id(self, http_session, base_url, create_user):
        """Fetching an existing user by ID should return 200 with correct data."""
        user = create_user(phone=uuid.uuid4().hex[:10])

        resp = http_session.get(f"{base_url}/users/{user['id']}")
        assert resp.status_code == 200
        body = resp.json()
        assert body["id"] == user["id"]
        assert body["phone"] == user["phone"]

    def test_get_nonexistent_user_returns_404(self, http_session, base_url):
        """Fetching a non-existent user should return 404."""
        resp = http_session.get(f"{base_url}/users/999999")
        assert resp.status_code == 404
