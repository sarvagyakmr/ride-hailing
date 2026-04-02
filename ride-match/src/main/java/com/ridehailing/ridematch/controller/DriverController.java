package com.ridehailing.ridematch.controller;

import com.ridehailing.ridematch.dto.DriverRequest;
import com.ridehailing.ridematch.dto.DriverResponse;
import com.ridehailing.ridematch.entity.Driver;
import com.ridehailing.ridematch.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @PostMapping
    public ResponseEntity<DriverResponse> createDriver(@Valid @RequestBody DriverRequest request) {
        Driver savedDriver = driverService.createDriver(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DriverResponse.fromEntity(savedDriver));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverResponse> getDriverById(@PathVariable("id") Long driverId) {
        return driverService.getDriverById(driverId)
                .map(driver -> ResponseEntity.ok(DriverResponse.fromEntity(driver)))
                .orElse(ResponseEntity.notFound().build());
    }
}