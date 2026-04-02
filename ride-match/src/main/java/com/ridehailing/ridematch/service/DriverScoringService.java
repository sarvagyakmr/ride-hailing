package com.ridehailing.ridematch.service;

import com.ridehailing.ridematch.entity.Driver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class DriverScoringService {

    @Value("${ride.scoring.distance.weight:0.4}")
    private double distanceWeight;

    @Value("${ride.scoring.last-ride-time.weight:0.3}")
    private double lastRideTimeWeight;

    @Value("${ride.scoring.rating.weight:0.3}")
    private double ratingWeight;

    @Value("${ride.scoring.max-distance-km:15}")
    private double maxDistanceKm;

    @Value("${ride.scoring.max-idle-minutes:60}")
    private int maxIdleMinutes;

    /**
     * Calculates a composite score for a driver based on multiple parameters.
     * Higher score = better candidate.
     *
     * @param driver       the driver to score
     * @param distanceKm   distance from pickup location in kilometers
     * @return composite score (0.0 to 1.0)
     */
    public double calculateScore(Driver driver, double distanceKm) {
        double distanceScore = calculateDistanceScore(distanceKm);
        double lastRideScore = calculateLastRideScore(driver.getLastRideTime());
        double ratingScore = calculateRatingScore(driver.getRating());

        double compositeScore = (distanceScore * distanceWeight)
                + (lastRideScore * lastRideTimeWeight)
                + (ratingScore * ratingWeight);

        log.debug("Driver {} scores - Distance: {:.2f}, LastRide: {:.2f}, Rating: {:.2f}, Composite: {:.2f}",
                driver.getId(), distanceScore, lastRideScore, ratingScore, compositeScore);

        return compositeScore;
    }

    /**
     * Distance score: closer is better.
     * Score = 1 - (distance / maxDistance)
     */
    private double calculateDistanceScore(double distanceKm) {
        if (distanceKm > maxDistanceKm) {
            return 0.0;
        }
        return 1.0 - (distanceKm / maxDistanceKm);
    }

    /**
     * Last ride time score: longer idle time is better (fairness).
     * Score = min(idleMinutes / maxIdleMinutes, 1.0)
     */
    private double calculateLastRideScore(LocalDateTime lastRideTime) {
        if (lastRideTime == null) {
            // Never driven before - highest priority
            return 1.0;
        }

        long idleMinutes = ChronoUnit.MINUTES.between(lastRideTime, LocalDateTime.now());
        return Math.min(idleMinutes / (double) maxIdleMinutes, 1.0);
    }

    /**
     * Rating score: higher rating is better.
     * Score = (rating - 1) / 4  (normalizes 1-5 scale to 0-1)
     */
    private double calculateRatingScore(BigDecimal rating) {
        if (rating == null) {
            return 0.5; // Default middle score for unknown rating
        }
        double ratingValue = rating.doubleValue();
        // Normalize 1-5 to 0-1
        return Math.max(0.0, Math.min(1.0, (ratingValue - 1.0) / 4.0));
    }
}
