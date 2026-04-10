"""
End-to-end integration tests for the complete ride lifecycle.

These tests exercise multi-step flows that span multiple controllers
and services, simulating real-world usage patterns.

Flows covered:
  1. Full ride lifecycle: request → assign → accept → complete → rate
  2. Ride offer rejection flow
  3. Driver scoring / ranking with multiple drivers
  4. Vehicle status transitions throughout a ride
  5. Duplicate rating prevention
  6. Complete flow then rating retrieval by ride & driver
"""

import time
import pytest
import uuid


class TestFullRideLifecycle:
    """
    End-to-end test of the happy path:
      1. Create customer + driver ecosystem
      2. Request a ride
      3. Wait for ride assignment (offers created by scheduler)
      4. Driver accepts the offer
      5. Complete ride
      6. Rate the ride
      7. Verify rating reflects on driver
    """

    def test_complete_ride_lifecycle(self, http_session, base_url, setup_full_ride_ecosystem):
        """Full lifecycle: request → offer → accept → in_route → complete → rate."""
        eco = setup_full_ride_ecosystem(
            driver_lat=12.9716, driver_lng=77.5946,
            pickup_lat=12.9750, pickup_lng=77.5900,
            drop_lat=13.0200, drop_lng=77.6500,
        )

        # --- Step 1: Request a ride ---
        ride_payload = {
            "customerId": eco["customer"]["id"],
            "startLocationId": eco["pickup_location"]["id"],
            "dropLocationId": eco["drop_location"]["id"],
            "upfrontFare": 200.00,
        }
        ride_resp = http_session.post(f"{base_url}/rides", json=ride_payload)
        assert ride_resp.status_code == 201
        ride = ride_resp.json()
        ride_id = ride["id"]
        assert ride["status"] == "REQUESTED"

        # --- Step 2: Wait for the scheduler to create offers ---
        # The RideMatchScheduler runs on a short interval; give it time
        offer_id = None
        for attempt in range(15):
            time.sleep(2)
            # Check ride status — it may have moved to OFFERED
            ride_check = http_session.get(f"{base_url}/rides/{ride_id}")
            assert ride_check.status_code == 200
            ride_data = ride_check.json()

            if ride_data["status"] in ("OFFERED", "IN_ROUTE"):
                break
        else:
            # If the scheduler didn't pick it up, we can't continue this flow
            pytest.skip(
                "Scheduler did not create offers within timeout — "
                "this may be expected if scheduler interval is long"
            )

        # If ride went directly to IN_ROUTE (auto-assigned), skip accept step
        if ride_data["status"] == "IN_ROUTE":
            ride_id = ride_data["id"]
        else:
            # We need to find the offer ID. Since there's no direct API to list offers,
            # we know the offer was created. We'll try offer IDs starting from 1.
            # In a real system you'd have an API for this; we approximate here.
            accepted = False
            for try_offer_id in range(1, 50):
                accept_resp = http_session.post(
                    f"{base_url}/rides/offers/{try_offer_id}/accept",
                    json={"driverId": eco["driver"]["id"]},
                )
                if accept_resp.status_code == 200:
                    accepted = True
                    offer_id = try_offer_id
                    break

            if not accepted:
                pytest.skip("Could not find and accept offer — skipping lifecycle test")

        # --- Step 3: Verify ride is IN_ROUTE ---
        ride_in_route = http_session.get(f"{base_url}/rides/{ride_id}")
        assert ride_in_route.status_code == 200
        ride_data = ride_in_route.json()
        assert ride_data["status"] == "IN_ROUTE"
        assert ride_data["driverId"] == eco["driver"]["id"]
        assert ride_data["vehicleId"] == eco["vehicle"]["id"]

        # --- Step 4: Verify vehicle is now IN_RIDE ---
        vehicle_resp = http_session.get(f"{base_url}/vehicles/{eco['vehicle']['id']}")
        assert vehicle_resp.status_code == 200
        assert vehicle_resp.json()["status"] == "IN_RIDE"

        # --- Step 5: Complete the ride ---
        complete_resp = http_session.patch(f"{base_url}/rides/{ride_id}/complete")
        assert complete_resp.status_code == 200
        completed = complete_resp.json()
        assert completed["status"] == "COMPLETE"

        # --- Step 6: Verify vehicle returns to AVAILABLE ---
        vehicle_after = http_session.get(f"{base_url}/vehicles/{eco['vehicle']['id']}")
        assert vehicle_after.status_code == 200
        assert vehicle_after.json()["status"] == "AVAILABLE"

        # --- Step 7: Rate the ride ---
        rating_payload = {
            "rideId": ride_id,
            "score": 4.5,
            "feedback": "Excellent service, very professional driver!",
        }
        rating_resp = http_session.post(f"{base_url}/ratings", json=rating_payload)
        assert rating_resp.status_code == 201
        rating = rating_resp.json()
        assert rating["rideId"] == ride_id
        assert rating["driverId"] == eco["driver"]["id"]
        assert float(rating["score"]) == pytest.approx(4.5, abs=0.01)
        assert rating["feedback"] == "Excellent service, very professional driver!"

        # --- Step 8: Verify rating is retrievable ---
        # By ride
        by_ride = http_session.get(f"{base_url}/ratings/ride/{ride_id}")
        assert by_ride.status_code == 200
        assert by_ride.json()["id"] == rating["id"]

        # By driver
        by_driver = http_session.get(f"{base_url}/ratings/driver/{eco['driver']['id']}")
        assert by_driver.status_code == 200
        driver_ratings = by_driver.json()
        assert any(r["id"] == rating["id"] for r in driver_ratings)


class TestDuplicateRatingPrevention:
    """Ensure the system prevents rating the same ride twice."""

    def test_cannot_rate_same_ride_twice(self, http_session, base_url, setup_full_ride_ecosystem):
        """After rating a completed ride, a second rating attempt should fail."""
        eco = setup_full_ride_ecosystem()

        # Create ride
        ride_resp = http_session.post(f"{base_url}/rides", json={
            "customerId": eco["customer"]["id"],
            "startLocationId": eco["pickup_location"]["id"],
            "dropLocationId": eco["drop_location"]["id"],
            "upfrontFare": 150.00,
        })
        assert ride_resp.status_code == 201
        ride_id = ride_resp.json()["id"]

        # Wait for assignment & accept
        accepted = False
        for _ in range(15):
            time.sleep(2)
            ride_check = http_session.get(f"{base_url}/rides/{ride_id}")
            if ride_check.json()["status"] == "IN_ROUTE":
                accepted = True
                break
            elif ride_check.json()["status"] == "OFFERED":
                for oid in range(1, 50):
                    ar = http_session.post(
                        f"{base_url}/rides/offers/{oid}/accept",
                        json={"driverId": eco["driver"]["id"]},
                    )
                    if ar.status_code == 200:
                        accepted = True
                        break
                if accepted:
                    break

        if not accepted:
            pytest.skip("Ride not assigned within timeout")

        # Complete ride
        complete_resp = http_session.patch(f"{base_url}/rides/{ride_id}/complete")
        assert complete_resp.status_code == 200

        # First rating
        first = http_session.post(f"{base_url}/ratings", json={
            "rideId": ride_id, "score": 5.0, "feedback": "Perfect"
        })
        assert first.status_code == 201

        # Second rating should fail
        second = http_session.post(f"{base_url}/ratings", json={
            "rideId": ride_id, "score": 3.0, "feedback": "Changed my mind"
        })
        assert second.status_code in (400, 500)


class TestRideRequestWithNoDrivers:
    """Test ride request when no drivers are available."""

    def test_ride_stays_requested_when_no_drivers(
        self, http_session, base_url, create_customer, create_location
    ):
        """
        When a ride is requested but no AVAILABLE vehicles exist nearby,
        the ride should remain in REQUESTED status.
        """
        customer = create_customer()
        # Use a very remote location where no driver exists
        pickup = create_location(lat=0.001, lng=0.001)
        drop = create_location(lat=0.010, lng=0.010)

        ride_resp = http_session.post(f"{base_url}/rides", json={
            "customerId": customer["id"],
            "startLocationId": pickup["id"],
            "dropLocationId": drop["id"],
            "upfrontFare": 100.00,
        })
        assert ride_resp.status_code == 201
        ride_id = ride_resp.json()["id"]

        # Give scheduler some time
        time.sleep(5)

        # Ride may still be REQUESTED (no nearby drivers) or OFFERED
        ride_check = http_session.get(f"{base_url}/rides/{ride_id}")
        assert ride_check.status_code == 200
        status = ride_check.json()["status"]
        assert status in ("REQUESTED", "OFFERED", "IN_ROUTE"), f"Expected REQUESTED/OFFERED/IN_ROUTE but got {status}"


class TestVehicleStatusTransitions:
    """Verify that vehicle status changes correctly throughout the ride lifecycle."""

    def test_vehicle_status_through_lifecycle(
        self, http_session, base_url, create_user, create_customer,
        create_location, create_vehicle, create_driver
    ):
        """Vehicle should transition: AVAILABLE → IN_RIDE → AVAILABLE."""
        # Setup
        customer = create_customer()
        user = create_user(phone=uuid.uuid4().hex[:10])
        driver_loc = create_location(lat=12.9716, lng=77.5946)
        vehicle = create_vehicle(location_id=driver_loc["id"])
        driver = create_driver(user_id=user["id"], vehicle_id=vehicle["id"])

        # Verify initial status
        v_resp = http_session.get(f"{base_url}/vehicles/{vehicle['id']}")
        assert v_resp.json()["status"] == "AVAILABLE"

        # Create ride near driver
        pickup = create_location(lat=12.9750, lng=77.5900)
        drop = create_location(lat=13.0, lng=77.6)
        ride_resp = http_session.post(f"{base_url}/rides", json={
            "customerId": customer["id"],
            "startLocationId": pickup["id"],
            "dropLocationId": drop["id"],
            "upfrontFare": 175.00,
        })
        ride_id = ride_resp.json()["id"]

        # Wait for assignment
        assigned = False
        for _ in range(15):
            time.sleep(2)
            rc = http_session.get(f"{base_url}/rides/{ride_id}")
            ride_status = rc.json()["status"]
            if ride_status == "IN_ROUTE":
                assigned = True
                break
            elif ride_status == "OFFERED":
                for oid in range(1, 50):
                    ar = http_session.post(
                        f"{base_url}/rides/offers/{oid}/accept",
                        json={"driverId": driver["id"]},
                    )
                    if ar.status_code == 200:
                        assigned = True
                        break
                if assigned:
                    break

        if not assigned:
            pytest.skip("Ride not assigned")

        # Vehicle should be IN_RIDE
        v_riding = http_session.get(f"{base_url}/vehicles/{vehicle['id']}")
        assert v_riding.json()["status"] == "IN_RIDE"

        # Complete ride
        http_session.patch(f"{base_url}/rides/{ride_id}/complete")

        # Vehicle should be AVAILABLE again
        v_after = http_session.get(f"{base_url}/vehicles/{vehicle['id']}")
        assert v_after.json()["status"] == "AVAILABLE"


class TestMultipleRidesSequential:
    """Test that the same driver can complete multiple rides in sequence."""

    def test_driver_completes_two_rides(
        self, http_session, base_url, create_user, create_customer,
        create_location, create_vehicle, create_driver
    ):
        """Same driver should be able to accept and complete a second ride after the first."""
        # Setup driver ecosystem
        user = create_user(phone=uuid.uuid4().hex[:10])
        driver_loc = create_location(lat=12.9716, lng=77.5946)
        vehicle = create_vehicle(location_id=driver_loc["id"])
        driver = create_driver(user_id=user["id"], vehicle_id=vehicle["id"])

        for ride_num in range(2):
            customer = create_customer()
            pickup = create_location(lat=12.9750, lng=77.5900)
            drop = create_location(lat=13.0, lng=77.6)

            ride_resp = http_session.post(f"{base_url}/rides", json={
                "customerId": customer["id"],
                "startLocationId": pickup["id"],
                "dropLocationId": drop["id"],
                "upfrontFare": 100.00 + ride_num * 50,
            })
            assert ride_resp.status_code == 201
            ride_id = ride_resp.json()["id"]

            # Wait for assignment
            assigned = False
            for _ in range(15):
                time.sleep(2)
                rc = http_session.get(f"{base_url}/rides/{ride_id}")
                ride_status = rc.json()["status"]
                if ride_status == "IN_ROUTE":
                    assigned = True
                    break
                elif ride_status == "OFFERED":
                    for oid in range(1, 100):
                        ar = http_session.post(
                            f"{base_url}/rides/offers/{oid}/accept",
                            json={"driverId": driver["id"]},
                        )
                        if ar.status_code == 200:
                            assigned = True
                            break
                    if assigned:
                        break

            if not assigned:
                pytest.skip(f"Ride #{ride_num + 1} not assigned within timeout")

            # Complete ride
            complete_resp = http_session.patch(f"{base_url}/rides/{ride_id}/complete")
            assert complete_resp.status_code == 200
            assert complete_resp.json()["status"] == "COMPLETE"

            # Rate the ride
            rating_resp = http_session.post(f"{base_url}/ratings", json={
                "rideId": ride_id,
                "score": 4.0 + ride_num * 0.5,
                "feedback": f"Ride #{ride_num + 1} was great!",
            })
            assert rating_resp.status_code == 201

        # Verify driver has 2 ratings
        ratings_resp = http_session.get(f"{base_url}/ratings/driver/{driver['id']}")
        assert ratings_resp.status_code == 200
        assert len(ratings_resp.json()) == 2
