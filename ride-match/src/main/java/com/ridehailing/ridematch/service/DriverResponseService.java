package com.ridehailing.ridematch.service;

import com.ridehailing.ridematch.entity.Driver;
import com.ridehailing.ridematch.entity.DriverRideOffer;
import com.ridehailing.ridematch.entity.Ride;
import com.ridehailing.ridematch.entity.Vehicle;
import com.ridehailing.ridematch.enums.OfferStatus;
import com.ridehailing.ridematch.enums.RideStatus;
import com.ridehailing.ridematch.enums.VehicleStatus;
import com.ridehailing.ridematch.repository.DriverRepository;
import com.ridehailing.ridematch.repository.DriverRideOfferRepository;
import com.ridehailing.ridematch.repository.RideRepository;
import com.ridehailing.ridematch.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverResponseService {

    private final DriverRideOfferRepository driverRideOfferRepository;
    private final RideRepository rideRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final DriverNotificationService driverNotificationService;
    private final DriverScoringService driverScoringService;

    /**
     * Driver accepts a ride offer.
     */
    @Transactional
    public boolean acceptRide(Long offerId, Long driverId) {
        log.info("Driver {} attempting to accept offer {}", driverId, offerId);

        Optional<DriverRideOffer> offerOpt = driverRideOfferRepository.findById(offerId);
        if (offerOpt.isEmpty()) {
            log.warn("Offer {} not found", offerId);
            return false;
        }

        DriverRideOffer offer = offerOpt.get();

        // Validate driver
        if (!offer.getDriverId().equals(driverId)) {
            log.warn("Driver {} does not match offer's driver {}", driverId, offer.getDriverId());
            return false;
        }

        // Check if offer is still pending
        if (offer.getStatus() != OfferStatus.PENDING) {
            log.warn("Offer {} is not pending, status: {}", offerId, offer.getStatus());
            return false;
        }

        // Get ride and check status
        Optional<Ride> rideOpt = rideRepository.findById(offer.getRideId());
        if (rideOpt.isEmpty()) {
            log.warn("Ride {} not found for offer {}", offer.getRideId(), offerId);
            return false;
        }

        Ride ride = rideOpt.get();

        // Check if ride is already assigned
        if (ride.getStatus() == RideStatus.IN_ROUTE || ride.getDriverId() != null) {
            log.warn("Ride {} is already assigned to driver {}", offer.getRideId(), ride.getDriverId());
            // Mark this offer as rejected since another driver got it
            offer.setStatus(OfferStatus.REJECTED);
            offer.setRejectionReason("Ride already assigned to another driver");
            offer.setRespondedAt(LocalDateTime.now());
            driverRideOfferRepository.save(offer);
            return false;
        }

        // Accept the ride
        offer.setStatus(OfferStatus.ACCEPTED);
        offer.setRespondedAt(LocalDateTime.now());
        driverRideOfferRepository.save(offer);

        // Assign ride to driver
        ride.setDriverId(offer.getDriverId());
        ride.setVehicleId(offer.getVehicleId());
        ride.setStatus(RideStatus.IN_ROUTE);
        rideRepository.save(ride);

        // Update vehicle status
        vehicleRepository.findById(offer.getVehicleId()).ifPresent(vehicle -> {
            vehicle.setStatus(VehicleStatus.IN_RIDE);
            vehicleRepository.save(vehicle);
        });

        // Reject all other pending offers for this ride
        rejectOtherOffers(offer.getRideId(), offerId);

        log.info("Driver {} accepted ride {} via offer {}", driverId, offer.getRideId(), offerId);
        return true;
    }

    /**
     * Driver rejects a ride offer.
     */
    @Transactional
    public boolean rejectRide(Long offerId, Long driverId, String reason) {
        log.info("Driver {} attempting to reject offer {} with reason: {}", driverId, offerId, reason);

        Optional<DriverRideOffer> offerOpt = driverRideOfferRepository.findById(offerId);
        if (offerOpt.isEmpty()) {
            log.warn("Offer {} not found", offerId);
            return false;
        }

        DriverRideOffer offer = offerOpt.get();

        // Validate driver
        if (!offer.getDriverId().equals(driverId)) {
            log.warn("Driver {} does not match offer's driver {}", driverId, offer.getDriverId());
            return false;
        }

        // Check if offer is still pending
        if (offer.getStatus() != OfferStatus.PENDING) {
            log.warn("Offer {} is not pending, status: {}", offerId, offer.getStatus());
            return false;
        }

        // Reject the offer
        offer.setStatus(OfferStatus.REJECTED);
        offer.setRejectionReason(reason);
        offer.setRespondedAt(LocalDateTime.now());
        driverRideOfferRepository.save(offer);

        log.info("Driver {} rejected ride {} via offer {}", driverId, offer.getRideId(), offerId);

        // Try to offer to next best driver
        offerToNextDriver(offer.getRideId(), offerId);

        return true;
    }

    /**
     * Reject all other pending offers for a ride.
     */
    private void rejectOtherOffers(Long rideId, Long acceptedOfferId) {
        List<DriverRideOffer> pendingOffers = driverRideOfferRepository
                .findByRideIdAndStatus(rideId, OfferStatus.PENDING);

        for (DriverRideOffer offer : pendingOffers) {
            if (!offer.getId().equals(acceptedOfferId)) {
                offer.setStatus(OfferStatus.REJECTED);
                offer.setRejectionReason("Ride accepted by another driver");
                offer.setRespondedAt(LocalDateTime.now());
                driverRideOfferRepository.save(offer);
                log.info("Auto-rejected offer {} for ride {} (accepted by another driver)",
                        offer.getId(), rideId);
            }
        }
    }

    /**
     * Offer ride to next best driver when one rejects.
     */
    private void offerToNextDriver(Long rideId, Long rejectedOfferId) {
        // This will be triggered by the scheduler/background job
        // For now, just log it - the offer timeout handler will pick this up
        log.info("Driver rejected offer {} for ride {} - will offer to next driver", rejectedOfferId, rideId);
    }

    /**
     * Get pending offer for a driver.
     */
    public Optional<DriverRideOffer> getPendingOffer(Long offerId, Long driverId) {
        return driverRideOfferRepository.findById(offerId)
                .filter(offer -> offer.getDriverId().equals(driverId))
                .filter(offer -> offer.getStatus() == OfferStatus.PENDING);
    }

    /**
     * Get all pending offers for a driver.
     */
    public List<DriverRideOffer> getPendingOffersForDriver(Long driverId) {
        return driverRideOfferRepository.findByDriverIdAndStatus(driverId, OfferStatus.PENDING);
    }
}
