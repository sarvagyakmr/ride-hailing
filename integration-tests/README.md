# Ride-Match Integration Tests

End-to-end integration test suite for the `ride-match` Spring Boot service.  
Written in Python using `pytest` + `requests`.

## Prerequisites

| Requirement | Version |
|-------------|---------|
| Python      | 3.8+    |
| pip         | latest  |
| ride-match  | running on `localhost:8080` |

## Quick Start

```bash
# 1. Start the ride-match service (from project root)
cd ride-match
mvn spring-boot:run

# 2. In another terminal, run the tests
cd integration-tests
pip install -r requirements.txt
pytest
```

Or use the convenience script:

```bash
chmod +x run_tests.sh
./run_tests.sh
```

## Test Modules

| Module                | Coverage |
|-----------------------|----------|
| `test_health.py`      | Health check endpoint |
| `test_users.py`       | User CRUD operations |
| `test_customers.py`   | Customer CRUD operations |
| `test_locations.py`   | Location CRUD operations |
| `test_vehicles.py`    | Vehicle CRUD + location update |
| `test_drivers.py`     | Driver CRUD operations |
| `test_rides.py`       | Ride request, get, complete, offer accept/reject |
| `test_ratings.py`     | Rating CRUD + validation edge cases |
| `test_e2e_flows.py`   | Full lifecycle & multi-step flow tests |

## Running Specific Tests

```bash
# Run a specific module
pytest test_health.py

# Run a specific test class
pytest test_rides.py::TestCreateRide

# Run a single test
pytest test_rides.py::TestCreateRide::test_create_ride_success

# Run by keyword
pytest -k "rating"

# Generate HTML report
pytest --html=report.html --self-contained-html
```

## Architecture

```
integration-tests/
├── conftest.py          # Shared fixtures, HTTP session, service readiness
├── pytest.ini           # Pytest configuration
├── requirements.txt     # Python dependencies
├── run_tests.sh         # Convenience runner script
├── test_health.py       # Health endpoint tests
├── test_users.py        # User endpoint tests
├── test_customers.py    # Customer endpoint tests
├── test_locations.py    # Location endpoint tests
├── test_vehicles.py     # Vehicle endpoint tests
├── test_drivers.py      # Driver endpoint tests
├── test_rides.py        # Ride endpoint tests
├── test_ratings.py      # Rating endpoint tests
└── test_e2e_flows.py    # End-to-end lifecycle tests
```
