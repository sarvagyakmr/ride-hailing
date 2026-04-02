package com.ridehailing.ridematch.controller;

import com.ridehailing.ridematch.dto.VehicleRequest;
import com.ridehailing.ridematch.dto.VehicleResponse;
import com.ridehailing.ridematch.dto.UpdateVehicleLocationRequest;
import com.ridehailing.ridematch.entity.Vehicle;
import com.ridehailing.ridematch.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<VehicleResponse> createVehicle(@Valid @RequestBody VehicleRequest request) {
        Vehicle savedVehicle = vehicleService.createVehicle(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(VehicleResponse.fromEntity(savedVehicle));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getVehicleById(@PathVariable("id") Long vehicleId) {
        return vehicleService.getVehicleById(vehicleId)
                .map(vehicle -> ResponseEntity.ok(VehicleResponse.fromEntity(vehicle)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/location")
    public ResponseEntity<VehicleResponse> updateVehicleLocation(
            @PathVariable("id") Long vehicleId,
            @Valid @RequestBody UpdateVehicleLocationRequest request) {
        return vehicleService.updateVehicleLocation(vehicleId, request)
                .map(vehicle -> ResponseEntity.ok(VehicleResponse.fromEntity(vehicle)))
                .orElse(ResponseEntity.notFound().build());
    }
}