package com.ridehailing.ridematch.dto;

import com.ridehailing.ridematch.entity.Ride;
import com.ridehailing.ridematch.enums.RideStatus;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideResponse {
    private Long id;
    private Long customerId;
    private Long driverId;
    private Long vehicleId;
    private Long startLocationId;
    private Long dropLocationId;
    private RideStatus status;
    private BigDecimal upfrontFare;
    private BigDecimal calculatedFare;

    public static RideResponse fromEntity(Ride ride) {
        return RideResponse.builder()
                .id(ride.getId())
                .customerId(ride.getCustomerId())
                .driverId(ride.getDriverId())
                .vehicleId(ride.getVehicleId())
                .startLocationId(ride.getStartLocationId())
                .dropLocationId(ride.getDropLocationId())
                .status(ride.getStatus())
                .upfrontFare(ride.getUpfrontFare())
                .calculatedFare(ride.getCalculatedFare())
                .build();
    }
}