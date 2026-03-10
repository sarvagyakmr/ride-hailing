package com.ridehailing.ridematch.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationRequest {
    private BigDecimal lat;
    private BigDecimal lng;
    private String geoHash;
}
