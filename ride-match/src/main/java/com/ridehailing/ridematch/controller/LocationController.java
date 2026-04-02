package com.ridehailing.ridematch.controller;

import com.ridehailing.ridematch.dto.LocationRequest;
import com.ridehailing.ridematch.dto.LocationResponse;
import com.ridehailing.ridematch.entity.Location;
import com.ridehailing.ridematch.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping
    public ResponseEntity<LocationResponse> createLocation(@Valid @RequestBody LocationRequest request) {
        Location savedLocation = locationService.createLocation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(LocationResponse.fromEntity(savedLocation));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationResponse> getLocationById(@PathVariable("id") Long locationId) {
        return locationService.getLocationById(locationId)
                .map(location -> ResponseEntity.ok(LocationResponse.fromEntity(location)))
                .orElse(ResponseEntity.notFound().build());
    }
}
