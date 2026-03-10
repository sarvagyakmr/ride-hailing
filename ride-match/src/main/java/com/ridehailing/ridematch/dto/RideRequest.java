package com.ridehailing.ridematch.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideRequest {
    private Long customerId;
    private Long driverId;
    private Long vehicleId;
    private Long startLocationId;
    private Long dropLocationId;
    private BigDecimal upfrontFare;
}