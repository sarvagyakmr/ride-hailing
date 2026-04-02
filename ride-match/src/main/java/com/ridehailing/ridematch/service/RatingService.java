package com.ridehailing.ridematch.service;

import com.ridehailing.ridematch.dto.RatingRequest;
import com.ridehailing.ridematch.dto.RatingResponse;
import com.ridehailing.ridematch.entity.Driver;
import com.ridehailing.ridematch.entity.Rating;
import com.ridehailing.ridematch.entity.Ride;
import com.ridehailing.ridematch.enums.RideStatus;
import com.ridehailing.ridematch.repository.DriverRepository;
import com.ridehailing.ridematch.repository.RatingRepository;
import com.ridehailing.ridematch.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;

    @Transactional
    public RatingResponse createRating(RatingRequest request) {
        // Validate ride exists and is completed
        Ride ride = rideRepository.findById(request.getRideId())
                .orElseThrow(() -> new IllegalArgumentException("Ride not found: " + request.getRideId()));

        if (ride.getStatus() != RideStatus.COMPLETE) {
            throw new IllegalArgumentException("Can only rate completed rides");
        }

        // Check if rating already exists
        if (ratingRepository.existsByRideId(request.getRideId())) {
            throw new IllegalArgumentException("Rating already exists for this ride");
        }

        // Create rating
        Rating rating = Rating.builder()
                .rideId(request.getRideId())
                .driverId(ride.getDriverId())
                .customerId(ride.getCustomerId())
                .score(request.getScore())
                .feedback(request.getFeedback())
                .build();

        Rating savedRating = ratingRepository.save(rating);

        // Update driver's overall rating
        updateDriverRating(ride.getDriverId());

        log.info("Created rating {} for ride {} and driver {}",
                savedRating.getId(), request.getRideId(), ride.getDriverId());

        return mapToResponse(savedRating);
    }

    public Optional<RatingResponse> getRatingById(Long ratingId) {
        return ratingRepository.findById(ratingId)
                .map(this::mapToResponse);
    }

    public Optional<RatingResponse> getRatingByRideId(Long rideId) {
        return ratingRepository.findByRideId(rideId)
                .map(this::mapToResponse);
    }

    public List<RatingResponse> getRatingsByDriverId(Long driverId) {
        return ratingRepository.findByDriverId(driverId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    protected void updateDriverRating(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + driverId));

        List<Rating> ratings = ratingRepository.findByDriverId(driverId);

        if (ratings.isEmpty()) {
            driver.setRating(BigDecimal.valueOf(5.0));
            driver.setTotalRatings(0);
        } else {
            BigDecimal sum = ratings.stream()
                    .map(Rating::getScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal average = sum.divide(BigDecimal.valueOf(ratings.size()), 2, RoundingMode.HALF_UP);

            driver.setRating(average);
            driver.setTotalRatings(ratings.size());
        }

        driverRepository.save(driver);
        log.info("Updated driver {} rating to {} ({} ratings)",
                driverId, driver.getRating(), driver.getTotalRatings());
    }

    private RatingResponse mapToResponse(Rating rating) {
        return RatingResponse.builder()
                .id(rating.getId())
                .rideId(rating.getRideId())
                .driverId(rating.getDriverId())
                .customerId(rating.getCustomerId())
                .score(rating.getScore())
                .feedback(rating.getFeedback())
                .createdAt(rating.getCreatedAt())
                .build();
    }
}
