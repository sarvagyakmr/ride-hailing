package com.ridehailing.ridematch.controller;

import com.ridehailing.ridematch.dto.VehicleRequest;
import com.ridehailing.ridematch.dto.VehicleResponse;
import com.ridehailing.ridematch.dto.UpdateVehicleLocationRequest;
import com.ridehailing.ridematch.entity.Vehicle;
import com.ridehailing.ridematch.repository.VehicleRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleRepository vehicleRepository;

    @PostMapping
    public ResponseEntity<VehicleResponse> createVehicle(@Valid @RequestBody VehicleRequest request) {
        Vehicle vehicle = Vehicle.builder()
                .locationId(request.getLocationId())
                .status(request.getStatus())
                .build();
        
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(VehicleResponse.fromEntity(savedVehicle));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getVehicleById(@PathVariable("id") Long vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .map(vehicle -> ResponseEntity.ok(VehicleResponse.fromEntity(vehicle)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/location")
    public ResponseEntity<VehicleResponse> updateVehicleLocation(
            @PathVariable("id") Long vehicleId,
            @Valid @RequestBody UpdateVehicleLocationRequest request) {
        return vehicleRepository.findById(vehicleId)
                .map(vehicle -> {
                    vehicle.setLocationId(request.getLocationId());
                    Vehicle updatedVehicle = vehicleRepository.save(vehicle);
                    return ResponseEntity.ok(VehicleResponse.fromEntity(updatedVehicle));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}