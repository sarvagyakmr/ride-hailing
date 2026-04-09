"""
Integration tests for the Health Check endpoint.

Endpoints under test:
  GET /api/v1/health
"""


class TestHealthCheck:
    """Tests for the health-check endpoint."""

    def test_health_returns_200(self, http_session, base_url):
        """Service health endpoint should return 200 with status UP."""
        resp = http_session.get(f"{base_url}/health")

        assert resp.status_code == 200
        body = resp.json()
        assert body["status"] == "UP"
        assert body["service"] == "ride-match"
        assert "timestamp" in body
