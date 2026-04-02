package com.ridehailing.ridematch.service;

import com.ridehailing.ridematch.entity.DriverRideOffer;
import com.ridehailing.ridematch.entity.Ride;
import com.ridehailing.ridematch.enums.OfferStatus;
import com.ridehailing.ridematch.enums.RideStatus;
import com.ridehailing.ridematch.event.DriverRideOfferEvent;
import com.ridehailing.ridematch.event.RedisPublisher;
import com.ridehailing.ridematch.repository.DriverRideOfferRepository;
import com.ridehailing.ridematch.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverNotificationService {

    private final DriverRideOfferRepository driverRideOfferRepository;
    private final RideRepository rideRepository;
    private final RedisPublisher redisPublisher;

    /**
     * Send ride offer to a driver.
     */
    @Transactional
    public boolean sendOfferToDriver(Long rideId, Long driverId, Long vehicleId, double score) {
        // Check if ride is still available
        Optional<Ride> rideOpt = rideRepository.findById(rideId);
        if (rideOpt.isEmpty()) {
            log.warn("Cannot send offer: Ride {} not found", rideId);
            return false;
        }

        Ride ride = rideOpt.get();
        if (ride.getStatus() != RideStatus.REQUESTED && ride.getStatus() != RideStatus.OFFERED) {
            log.warn("Cannot send offer: Ride {} is already in status {}", rideId, ride.getStatus());
            return false;
        }

        // Check if offer already exists
        if (driverRideOfferRepository.existsByRideIdAndDriverId(rideId, driverId)) {
            log.warn("Offer already exists for ride {} and driver {}", rideId, driverId);
            return false;
        }

        // Create offer record
        DriverRideOffer offer = DriverRideOffer.builder()
                .rideId(rideId)
                .driverId(driverId)
                .vehicleId(vehicleId)
                .status(OfferStatus.PENDING)
                .score(score)
                .build();

        DriverRideOffer savedOffer = driverRideOfferRepository.save(offer);

        // Update ride status to OFFERED
        if (ride.getStatus() == RideStatus.REQUESTED) {
            ride.setStatus(RideStatus.OFFERED);
            rideRepository.save(ride);
        }

        // Publish event to notify driver
        DriverRideOfferEvent event = DriverRideOfferEvent.builder()
                .offerId(savedOffer.getId())
                .rideId(rideId)
                .driverId(driverId)
                .customerId(ride.getCustomerId())
                .startLocationId(ride.getStartLocationId())
                .dropLocationId(ride.getDropLocationId())
                .upfrontFare(ride.getUpfrontFare())
                .build();

        redisPublisher.publishDriverRideOffer(event);

        log.info("Sent ride offer {} to driver {} for ride {} with score {}",
                savedOffer.getId(), driverId, rideId, score);
        return true;
    }

    /**
     * Get pending offers for a driver.
     */
    public List<DriverRideOffer> getPendingOffersForDriver(Long driverId) {
        return driverRideOfferRepository.findByDriverIdAndStatus(driverId, OfferStatus.PENDING);
    }

    /**
     * Get all offers for a ride.
     */
    public List<DriverRideOffer> getOffersForRide(Long rideId) {
        return driverRideOfferRepository.findByRideId(rideId);
    }
}
