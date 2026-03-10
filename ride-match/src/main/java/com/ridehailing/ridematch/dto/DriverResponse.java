package com.ridehailing.ridematch.dto;

import com.ridehailing.ridematch.entity.Driver;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverResponse {
    private Long id;
    private Long userId;
    private Long vehicleId;

    public static DriverResponse fromEntity(Driver driver) {
        return DriverResponse.builder()
                .id(driver.getId())
                .userId(driver.getUserId())
                .vehicleId(driver.getVehicleId())
                .build();
    }
}