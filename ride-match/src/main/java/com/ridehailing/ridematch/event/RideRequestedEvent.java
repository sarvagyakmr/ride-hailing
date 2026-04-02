package com.ridehailing.ridematch.event;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideRequestedEvent {
    private Long rideId;
    private Long customerId;
    private Long startLocationId;
    private Long dropLocationId;
    private BigDecimal upfrontFare;
}
