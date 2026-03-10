package com.ridehailing.ridematch.scheduler;

import com.ridehailing.ridematch.entity.Location;
import com.ridehailing.ridematch.entity.Ride;
import com.ridehailing.ridematch.entity.Vehicle;
import com.ridehailing.ridematch.enums.RideStatus;
import com.ridehailing.ridematch.enums.VehicleStatus;
import com.ridehailing.ridematch.repository.LocationRepository;
import com.ridehailing.ridematch.repository.RideRepository;
import com.ridehailing.ridematch.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RideMatchScheduler {

    private final RideRepository rideRepository;
    private final VehicleRepository vehicleRepository;
    private final LocationRepository locationRepository;

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void matchRides() {
        log.info("Starting ride matching process...");
        
        List<Ride> requestedRides = rideRepository.findByStatus(RideStatus.REQUESTED);
        
        if (requestedRides.isEmpty()) {
            log.info("No rides in REQUESTED status found");
            return;
        }
        
        log.info("Found {} rides in REQUESTED status", requestedRides.size());
        
        List<Vehicle> availableVehicles = vehicleRepository.findByStatus(VehicleStatus.AVAILABLE);
        
        if (availableVehicles.isEmpty()) {
            log.info("No available vehicles found");
            return;
        }
        
        log.info("Found {} available vehicles", availableVehicles.size());
        
        for (Ride ride : requestedRides) {
            Optional<Location> startLocationOpt = locationRepository.findById(ride.getStartLocationId());
            
            if (startLocationOpt.isEmpty()) {
                log.warn("Start location not found for ride {}", ride.getId());
                continue;
            }
            
            Location startLocation = startLocationOpt.get();
            
            Vehicle closestVehicle = findClosestVehicle(startLocation, availableVehicles);
            
            if (closestVehicle == null) {
                log.info("No suitable vehicle found for ride {}", ride.getId());
                continue;
            }
            
            assignRideToVehicle(ride, closestVehicle);
            availableVehicles.remove(closestVehicle);
            
            log.info("Assigned ride {} to vehicle {}", ride.getId(), closestVehicle.getId());
        }
    }
    
    private Vehicle findClosestVehicle(Location startLocation, List<Vehicle> availableVehicles) {
        Vehicle closestVehicle = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Vehicle vehicle : availableVehicles) {
            Optional<Location> vehicleLocationOpt = locationRepository.findById(vehicle.getLocationId());
            
            if (vehicleLocationOpt.isEmpty()) {
                log.warn("Location not found for vehicle {}", vehicle.getId());
                continue;
            }
            
            Location vehicleLocation = vehicleLocationOpt.get();
            double distance = calculateDistance(
                    startLocation.getLat(), startLocation.getLng(),
                    vehicleLocation.getLat(), vehicleLocation.getLng()
            );
            
            if (distance < minDistance) {
                minDistance = distance;
                closestVehicle = vehicle;
            }
        }
        
        return closestVehicle;
    }
    
    private double calculateDistance(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        double earthRadius = 6371.0; // Earth's radius in kilometers
        
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
    
    @Transactional
    public void assignRideToVehicle(Ride ride, Vehicle vehicle) {
        ride.setStatus(RideStatus.IN_ROUTE);
        rideRepository.save(ride);
        
        vehicle.setStatus(VehicleStatus.IN_RIDE);
        vehicleRepository.save(vehicle);
    }
}