package com.ridehailing.ridematch.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingResponse {

    private Long id;
    private Long rideId;
    private Long driverId;
    private Long customerId;
    private BigDecimal score;
    private String feedback;
    private LocalDateTime createdAt;
}
