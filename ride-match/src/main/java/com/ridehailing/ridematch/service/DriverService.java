package com.ridehailing.ridematch.service;

import com.ridehailing.ridematch.dto.DriverRequest;
import com.ridehailing.ridematch.entity.Driver;
import com.ridehailing.ridematch.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;

    @Transactional
    public Driver createDriver(DriverRequest request) {
        Driver driver = Driver.builder()
                .userId(request.getUserId())
                .vehicleId(request.getVehicleId())
                .build();
        return driverRepository.save(driver);
    }

    public Optional<Driver> getDriverById(Long driverId) {
        return driverRepository.findById(driverId);
    }
}
