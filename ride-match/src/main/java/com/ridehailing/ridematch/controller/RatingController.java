package com.ridehailing.ridematch.controller;

import com.ridehailing.ridematch.dto.RatingRequest;
import com.ridehailing.ridematch.dto.RatingResponse;
import com.ridehailing.ridematch.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    public ResponseEntity<RatingResponse> createRating(@Valid @RequestBody RatingRequest request) {
        RatingResponse rating = ratingService.createRating(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(rating);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RatingResponse> getRatingById(@PathVariable("id") Long ratingId) {
        return ratingService.getRatingById(ratingId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ride/{rideId}")
    public ResponseEntity<RatingResponse> getRatingByRideId(@PathVariable("rideId") Long rideId) {
        return ratingService.getRatingByRideId(rideId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<RatingResponse>> getRatingsByDriverId(@PathVariable("driverId") Long driverId) {
        List<RatingResponse> ratings = ratingService.getRatingsByDriverId(driverId);
        return ResponseEntity.ok(ratings);
    }
}
