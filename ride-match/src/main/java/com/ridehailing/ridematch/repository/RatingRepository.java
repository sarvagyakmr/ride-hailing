package com.ridehailing.ridematch.repository;

import com.ridehailing.ridematch.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByDriverId(Long driverId);

    Optional<Rating> findByRideId(Long rideId);

    boolean existsByRideId(Long rideId);
}
