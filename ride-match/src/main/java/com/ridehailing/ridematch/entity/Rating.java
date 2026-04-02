package com.ridehailing.ridematch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ratings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ride_id", nullable = false)
    private Long rideId;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "score", precision = 2, scale = 1, nullable = false)
    private BigDecimal score;

    @Column(name = "feedback", length = 500)
    private String feedback;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
