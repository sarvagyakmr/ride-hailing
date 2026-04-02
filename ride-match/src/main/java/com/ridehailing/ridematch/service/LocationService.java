package com.ridehailing.ridematch.service;

import com.ridehailing.ridematch.dto.LocationRequest;
import com.ridehailing.ridematch.entity.Location;
import com.ridehailing.ridematch.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;

    @Transactional
    public Location createLocation(LocationRequest request) {
        Location location = Location.builder()
                .lat(request.getLat())
                .lng(request.getLng())
                .geoHash(request.getGeoHash())
                .build();
        return locationRepository.save(location);
    }

    public Optional<Location> getLocationById(Long locationId) {
        return locationRepository.findById(locationId);
    }
}
