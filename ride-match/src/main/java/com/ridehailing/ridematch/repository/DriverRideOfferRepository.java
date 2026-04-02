package com.ridehailing.ridematch.repository;

import com.ridehailing.ridematch.entity.DriverRideOffer;
import com.ridehailing.ridematch.enums.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRideOfferRepository extends JpaRepository<DriverRideOffer, Long> {

    List<DriverRideOffer> findByRideId(Long rideId);

    List<DriverRideOffer> findByRideIdAndStatus(Long rideId, OfferStatus status);

    List<DriverRideOffer> findByDriverIdAndStatus(Long driverId, OfferStatus status);

    Optional<DriverRideOffer> findByRideIdAndDriverId(Long rideId, Long driverId);

    boolean existsByRideIdAndDriverId(Long rideId, Long driverId);

    List<DriverRideOffer> findByStatusAndOfferedAtBefore(OfferStatus status, LocalDateTime before);
}
