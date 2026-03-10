package com.ridehailing.ridematch.dto;

import com.ridehailing.ridematch.entity.Vehicle;
import com.ridehailing.ridematch.enums.VehicleStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponse {
    private Long id;
    private Long locationId;
    private VehicleStatus status;

    public static VehicleResponse fromEntity(Vehicle vehicle) {
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .locationId(vehicle.getLocationId())
                .status(vehicle.getStatus())
                .build();
    }
}