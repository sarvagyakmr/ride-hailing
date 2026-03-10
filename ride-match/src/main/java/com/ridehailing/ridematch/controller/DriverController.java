package com.ridehailing.ridematch.controller;

import com.ridehailing.ridematch.dto.DriverRequest;
import com.ridehailing.ridematch.dto.DriverResponse;
import com.ridehailing.ridematch.entity.Driver;
import com.ridehailing.ridematch.repository.DriverRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverRepository driverRepository;

    @PostMapping
    public ResponseEntity<DriverResponse> createDriver(@Valid @RequestBody DriverRequest request) {
        Driver driver = Driver.builder()
                .userId(request.getUserId())
                .vehicleId(request.getVehicleId())
                .build();
        
        Driver savedDriver = driverRepository.save(driver);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DriverResponse.fromEntity(savedDriver));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverResponse> getDriverById(@PathVariable("id") Long driverId) {
        return driverRepository.findById(driverId)
                .map(driver -> ResponseEntity.ok(DriverResponse.fromEntity(driver)))
                .orElse(ResponseEntity.notFound().build());
    }
}