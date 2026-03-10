package com.ridehailing.ridematch.entity;

import jakarta.persistence.*;
import lombok.*;
import com.ridehailing.ridematch.enums.RideStatus;

import java.math.BigDecimal;

@Entity
@Table(name = "rides")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "start_location_id", nullable = false)
    private Long startLocationId;

    @Column(name = "drop_location_id", nullable = false)
    private Long dropLocationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RideStatus status;

    @Column(name = "upfront_fare", precision = 10, scale = 2)
    private BigDecimal upfrontFare;

    @Column(name = "calculated_fare", precision = 10, scale = 2)
    private BigDecimal calculatedFare;
}
