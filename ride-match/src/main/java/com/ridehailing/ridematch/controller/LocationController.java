package com.ridehailing.ridematch.controller;

import com.ridehailing.ridematch.dto.LocationRequest;
import com.ridehailing.ridematch.dto.LocationResponse;
import com.ridehailing.ridematch.entity.Location;
import com.ridehailing.ridematch.repository.LocationRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationRepository locationRepository;

    @PostMapping
    public ResponseEntity<LocationResponse> createLocation(@Valid @RequestBody LocationRequest request) {
        Location location = Location.builder()
                .lat(request.getLat())
                .lng(request.getLng())
                .geoHash(request.getGeoHash())
                .build();
        
        Location savedLocation = locationRepository.save(location);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(LocationResponse.fromEntity(savedLocation));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationResponse> getLocationById(@PathVariable("id") Long locationId) {
        return locationRepository.findById(locationId)
                .map(location -> ResponseEntity.ok(LocationResponse.fromEntity(location)))
                .orElse(ResponseEntity.notFound().build());
    }
}
