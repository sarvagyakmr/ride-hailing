package com.ridehailing.ridematch.service;

import com.ridehailing.ridematch.entity.Driver;
import com.ridehailing.ridematch.entity.DriverRideOffer;
import com.ridehailing.ridematch.entity.Location;
import com.ridehailing.ridematch.entity.Ride;
import com.ridehailing.ridematch.entity.Vehicle;
import com.ridehailing.ridematch.enums.OfferStatus;
import com.ridehailing.ridematch.enums.RideStatus;
import com.ridehailing.ridematch.enums.VehicleStatus;
import com.ridehailing.ridematch.repository.DriverRepository;
import com.ridehailing.ridematch.repository.DriverRideOfferRepository;
import com.ridehailing.ridematch.repository.LocationRepository;
import com.ridehailing.ridematch.repository.RideRepository;
import com.ridehailing.ridematch.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideAssignmentService {

    private final RideRepository rideRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final LocationRepository locationRepository;
    private final DriverRideOfferRepository driverRideOfferRepository;
    private final DriverScoringService driverScoringService;
    private final DriverNotificationService driverNotificationService;

    @Value("${ride.assignment.max-search-radius-km:15}")
    private double maxSearchRadiusKm;

    @Value("${ride.assignment.max-offers-per-ride:3}")
    private int maxOffersPerRide;

    /**
     * Process ride assignment by sending offers to top-scoring drivers.
     */
    @Transactional
    public boolean processRideAssignment(Long rideId) {
        log.info("Processing ride assignment for ride: {}", rideId);

        Optional<Ride> rideOpt = rideRepository.findById(rideId);
        if (rideOpt.isEmpty()) {
            log.warn("Ride not found: {}", rideId);
            return false;
        }

        Ride ride = rideOpt.get();

        // Check if already assigned
        if (ride.getDriverId() != null && ride.getVehicleId() != null) {
            log.info("Ride {} already assigned", rideId);
            return true;
        }

        // Check if ride is still in a state that allows offers
        if (ride.getStatus() != RideStatus.REQUESTED && ride.getStatus() != RideStatus.OFFERED) {
            log.info("Ride {} is in status {} - not eligible for offers", rideId, ride.getStatus());
            return false;
        }

        Optional<Location> startLocationOpt = locationRepository.findById(ride.getStartLocationId());
        if (startLocationOpt.isEmpty()) {
            log.warn("Start location not found for ride {}", rideId);
            return false;
        }

        Location startLocation = startLocationOpt.get();
        List<Vehicle> availableVehicles = vehicleRepository.findByStatus(VehicleStatus.AVAILABLE);

        if (availableVehicles.isEmpty()) {
            log.info("No available vehicles for ride {}", rideId);
            return false;
        }

        // Get already offered drivers to exclude them
        List<Long> alreadyOfferedDrivers = driverRideOfferRepository.findByRideId(rideId).stream()
                .map(DriverRideOffer::getDriverId)
                .collect(Collectors.toList());

        // Find and rank all eligible drivers
        List<DriverScore> rankedDrivers = findAndRankDrivers(startLocation, availableVehicles, alreadyOfferedDrivers);

        if (rankedDrivers.isEmpty()) {
            log.info("No suitable drivers found for ride {}", rideId);
            return false;
        }

        // Send offers to top N drivers (one at a time or batch depending on strategy)
        int offersToSend = Math.min(maxOffersPerRide, rankedDrivers.size());
        boolean anyOfferSent = false;

        for (int i = 0; i < offersToSend; i++) {
            DriverScore driverScore = rankedDrivers.get(i);
            boolean sent = driverNotificationService.sendOfferToDriver(
                    rideId,
                    driverScore.driver.getId(),
                    driverScore.vehicle.getId(),
                    driverScore.score
            );
            if (sent) {
                anyOfferSent = true;
                log.info("Sent offer to driver {} for ride {} (rank {} with score {})",
                        driverScore.driver.getId(), rideId, i + 1, driverScore.score);
            }
        }

        return anyOfferSent;
    }

    /**
     * Find and rank all eligible drivers by score.
     */
    private List<DriverScore> findAndRankDrivers(Location startLocation, List<Vehicle> availableVehicles, List<Long> excludeDrivers) {
        return availableVehicles.stream()
                .map(vehicle -> {
                    Optional<Location> vehicleLocationOpt = locationRepository.findById(vehicle.getLocationId());
                    if (vehicleLocationOpt.isEmpty()) {
                        return null;
                    }

                    Optional<Driver> driverOpt = driverRepository.findByVehicleId(vehicle.getId());
                    if (driverOpt.isEmpty()) {
                        return null;
                    }

                    Driver driver = driverOpt.get();

                    // Skip if already offered
                    if (excludeDrivers.contains(driver.getId())) {
                        return null;
                    }

                    Location vehicleLocation = vehicleLocationOpt.get();
                    double distance = calculateDistance(
                            startLocation.getLat(), startLocation.getLng(),
                            vehicleLocation.getLat(), vehicleLocation.getLng()
                    );

                    if (distance > maxSearchRadiusKm) {
                        return null;
                    }

                    double score = driverScoringService.calculateScore(driver, distance);
                    return new DriverScore(driver, vehicle, distance, score);
                })
                .filter(ds -> ds != null)
                .sorted(Comparator.comparingDouble((DriverScore ds) -> ds.score).reversed())
                .collect(Collectors.toList());
    }

    private double calculateDistance(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        double earthRadius = 6371.0;

        double lat1Rad = Math.toRadians(lat1.doubleValue());
        double lat2Rad = Math.toRadians(lat2.doubleValue());
        double deltaLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double deltaLng = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c;
    }

    /**
     * Directly assign ride to vehicle (used when driver accepts offer).
     */
    @Transactional
    public boolean assignRideToVehicle(Ride ride, Vehicle vehicle) {
        Optional<Driver> driverOpt = driverRepository.findByVehicleId(vehicle.getId());
        if (driverOpt.isEmpty()) {
            log.warn("No driver found for vehicle {}", vehicle.getId());
            return false;
        }

        Driver driver = driverOpt.get();

        ride.setDriverId(driver.getId());
        ride.setVehicleId(vehicle.getId());
        ride.setStatus(RideStatus.IN_ROUTE);
        rideRepository.save(ride);

        vehicle.setStatus(VehicleStatus.IN_RIDE);
        vehicleRepository.save(vehicle);

        log.info("Assigned ride {} to driver {} and vehicle {}", ride.getId(), driver.getId(), vehicle.getId());
        return true;
    }

    /**
     * Handle expired offers and retry with next drivers.
     */
    @Transactional
    public void handleExpiredOffersAndRetry() {
        // Find rides that have pending offers but all have expired
        // This would be called by a scheduled job
        // Implementation depends on the retry strategy
    }

    /**
     * Helper class to hold driver scoring information.
     */
    private record DriverScore(Driver driver, Vehicle vehicle, double distance, double score) {
    }
}
