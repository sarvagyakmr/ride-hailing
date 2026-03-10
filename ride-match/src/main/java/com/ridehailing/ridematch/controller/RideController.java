package com.ridehailing.ridematch.controller;

import com.ridehailing.ridematch.dto.RideRequest;
import com.ridehailing.ridematch.dto.RideResponse;
import com.ridehailing.ridematch.entity.Ride;
import com.ridehailing.ridematch.enums.RideStatus;
import com.ridehailing.ridematch.repository.RideRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rides")
@RequiredArgsConstructor
public class RideController {

    private final RideRepository rideRepository;

    @PostMapping
    public ResponseEntity<RideResponse> createRide(@Valid @RequestBody RideRequest request) {
        Ride ride = Ride.builder()
                .customerId(request.getCustomerId())
                .driverId(request.getDriverId())
                .vehicleId(request.getVehicleId())
                .startLocationId(request.getStartLocationId())
                .dropLocationId(request.getDropLocationId())
                .status(RideStatus.REQUESTED)
                .upfrontFare(request.getUpfrontFare())
                .build();
        
        Ride savedRide = rideRepository.save(ride);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RideResponse.fromEntity(savedRide));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RideResponse> getRideById(@PathVariable("id") Long rideId) {
        return rideRepository.findById(rideId)
                .map(ride -> ResponseEntity.ok(RideResponse.fromEntity(ride)))
                .orElse(ResponseEntity.notFound().build());
    }
}