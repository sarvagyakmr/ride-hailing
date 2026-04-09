"""
Integration tests for the Customer management endpoints.

Endpoints under test:
  POST /api/v1/customers
  GET  /api/v1/customers/{id}
"""


class TestCreateCustomer:
    """Tests for POST /api/v1/customers."""

    def test_create_customer_success(self, http_session, base_url):
        """Creating a customer (no body required) should return 201."""
        resp = http_session.post(f"{base_url}/customers")

        assert resp.status_code == 201
        body = resp.json()
        assert body["id"] is not None

    def test_create_multiple_customers(self, http_session, base_url):
        """Multiple customers can be created, each with a unique ID."""
        ids = set()
        for _ in range(3):
            resp = http_session.post(f"{base_url}/customers")
            assert resp.status_code == 201
            ids.add(resp.json()["id"])

        assert len(ids) == 3


class TestGetCustomer:
    """Tests for GET /api/v1/customers/{id}."""

    def test_get_customer_by_id(self, http_session, base_url, create_customer):
        """Fetching an existing customer by ID should return 200."""
        customer = create_customer()

        resp = http_session.get(f"{base_url}/customers/{customer['id']}")
        assert resp.status_code == 200
        assert resp.json()["id"] == customer["id"]

    def test_get_nonexistent_customer_returns_404(self, http_session, base_url):
        """Fetching a non-existent customer should return 404."""
        resp = http_session.get(f"{base_url}/customers/999999")
        assert resp.status_code == 404
