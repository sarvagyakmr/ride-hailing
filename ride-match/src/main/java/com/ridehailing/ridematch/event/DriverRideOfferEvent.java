package com.ridehailing.ridematch.event;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverRideOfferEvent {
    private Long offerId;
    private Long rideId;
    private Long driverId;
    private Long customerId;
    private Long startLocationId;
    private Long dropLocationId;
    private BigDecimal upfrontFare;
}
