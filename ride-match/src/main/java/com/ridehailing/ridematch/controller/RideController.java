package com.ridehailing.ridematch.controller;

import com.ridehailing.ridematch.dto.DriverResponseRequest;
import com.ridehailing.ridematch.dto.RideRequest;
import com.ridehailing.ridematch.dto.RideResponse;
import com.ridehailing.ridematch.entity.Ride;
import com.ridehailing.ridematch.service.DriverResponseService;
import com.ridehailing.ridematch.service.RideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rides")
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;
    private final DriverResponseService driverResponseService;

    @PostMapping
    public ResponseEntity<RideResponse> createRide(@Valid @RequestBody RideRequest request) {
        Ride savedRide = rideService.createRide(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RideResponse.fromEntity(savedRide));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RideResponse> getRideById(@PathVariable("id") Long rideId) {
        return rideService.getRideById(rideId)
                .map(ride -> ResponseEntity.ok(RideResponse.fromEntity(ride)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<RideResponse> completeRide(@PathVariable("id") Long rideId) {
        return rideService.completeRide(rideId)
                .map(ride -> ResponseEntity.ok(RideResponse.fromEntity(ride)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Driver accepts a ride offer.
     */
    @PostMapping("/offers/{offerId}/accept")
    public ResponseEntity<String> acceptRide(
            @PathVariable("offerId") Long offerId,
            @Valid @RequestBody DriverResponseRequest request) {
        boolean accepted = driverResponseService.acceptRide(offerId, request.getDriverId());
        if (accepted) {
            return ResponseEntity.ok("Ride accepted successfully");
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Failed to accept ride - may already be assigned or expired");
        }
    }

    /**
     * Driver rejects a ride offer.
     */
    @PostMapping("/offers/{offerId}/reject")
    public ResponseEntity<String> rejectRide(
            @PathVariable("offerId") Long offerId,
            @Valid @RequestBody DriverResponseRequest request) {
        boolean rejected = driverResponseService.rejectRide(offerId, request.getDriverId(), request.getReason());
        if (rejected) {
            return ResponseEntity.ok("Ride rejected successfully");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to reject ride - may not be in pending state");
        }
    }
}