package com.ridehailing.ridematch.service;

import com.ridehailing.ridematch.dto.UpdateVehicleLocationRequest;
import com.ridehailing.ridematch.dto.VehicleRequest;
import com.ridehailing.ridematch.entity.Vehicle;
import com.ridehailing.ridematch.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    @Transactional
    public Vehicle createVehicle(VehicleRequest request) {
        Vehicle vehicle = Vehicle.builder()
                .locationId(request.getLocationId())
                .status(request.getStatus())
                .build();
        return vehicleRepository.save(vehicle);
    }

    public Optional<Vehicle> getVehicleById(Long vehicleId) {
        return vehicleRepository.findById(vehicleId);
    }

    @Transactional
    public Optional<Vehicle> updateVehicleLocation(Long vehicleId, UpdateVehicleLocationRequest request) {
        return vehicleRepository.findById(vehicleId)
                .map(vehicle -> {
                    vehicle.setLocationId(request.getLocationId());
                    return vehicleRepository.save(vehicle);
                });
    }
}
