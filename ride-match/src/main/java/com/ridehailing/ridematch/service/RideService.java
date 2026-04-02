package com.ridehailing.ridematch.service;

import com.ridehailing.ridematch.dto.RideRequest;
import com.ridehailing.ridematch.entity.Location;
import com.ridehailing.ridematch.entity.Ride;
import com.ridehailing.ridematch.entity.Vehicle;
import com.ridehailing.ridematch.enums.RideStatus;
import com.ridehailing.ridematch.enums.VehicleStatus;
import com.ridehailing.ridematch.event.RedisPublisher;
import com.ridehailing.ridematch.event.RideRequestedEvent;
import com.ridehailing.ridematch.repository.DriverRepository;
import com.ridehailing.ridematch.repository.LocationRepository;
import com.ridehailing.ridematch.repository.RideRepository;
import com.ridehailing.ridematch.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideService {

    private final RideRepository rideRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final LocationRepository locationRepository;
    private final RedisPublisher redisPublisher;

    @Transactional
    public Ride createRide(RideRequest request) {
        Ride ride = Ride.builder()
                .customerId(request.getCustomerId())
                .startLocationId(request.getStartLocationId())
                .dropLocationId(request.getDropLocationId())
                .status(RideStatus.REQUESTED)
                .upfrontFare(request.getUpfrontFare())
                .build();
        
        Ride savedRide = rideRepository.save(ride);

        // Fetch pickup location details for the event
        Location pickupLocation = locationRepository.findById(request.getStartLocationId())
                .orElse(null);

        // Build ride requested event with location coordinates
        RideRequestedEvent.RideRequestedEventBuilder eventBuilder = RideRequestedEvent.builder()
                .rideId(savedRide.getId())
                .customerId(savedRide.getCustomerId())
                .startLocationId(savedRide.getStartLocationId())
                .dropLocationId(savedRide.getDropLocationId())
                .upfrontFare(savedRide.getUpfrontFare());

        // Include pickup coordinates if location exists
        if (pickupLocation != null) {
            eventBuilder
                    .pickupLat(pickupLocation.getLat())
                    .pickupLng(pickupLocation.getLng())
                    .geoHash(pickupLocation.getGeoHash());
            log.info("Publishing ride {} with pickup location ({}, {}) - geohash: {}",
                    savedRide.getId(), pickupLocation.getLat(), pickupLocation.getLng(),
                    pickupLocation.getGeoHash());
        } else {
            log.warn("Pickup location {} not found for ride {}", 
                    request.getStartLocationId(), savedRide.getId());
        }

        RideRequestedEvent event = eventBuilder.build();
        redisPublisher.publishRideRequested(event);

        return savedRide;
    }

    public Optional<Ride> getRideById(Long rideId) {
        return rideRepository.findById(rideId);
    }

    @Transactional
    public Optional<Ride> completeRide(Long rideId) {
        Optional<Ride> rideOpt = rideRepository.findById(rideId);
        if (rideOpt.isEmpty()) {
            return Optional.empty();
        }

        Ride ride = rideOpt.get();

        // Only allow completing rides that are IN_ROUTE
        if (ride.getStatus() != RideStatus.IN_ROUTE) {
            log.warn("Cannot complete ride {} with status {}", rideId, ride.getStatus());
            return Optional.empty();
        }

        // Update ride status
        ride.setStatus(RideStatus.COMPLETE);
        Ride completedRide = rideRepository.save(ride);

        // Update vehicle status to AVAILABLE
        vehicleRepository.findById(ride.getVehicleId()).ifPresent(vehicle -> {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicleRepository.save(vehicle);
        });

        // Update driver's last ride time
        driverRepository.findById(ride.getDriverId()).ifPresent(driver -> {
            driver.setLastRideTime(LocalDateTime.now());
            driverRepository.save(driver);
        });

        log.info("Completed ride {}", rideId);
        return Optional.of(completedRide);
    }
}
