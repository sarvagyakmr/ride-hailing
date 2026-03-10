package com.ridehailing.ridematch.dto;

import lombok.*;
import com.ridehailing.ridematch.entity.Location;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationResponse {
    private Long id;
    private BigDecimal lat;
    private BigDecimal lng;
    private String geoHash;

    public static LocationResponse fromEntity(Location location) {
        return LocationResponse.builder()
                .id(location.getId())
                .lat(location.getLat())
                .lng(location.getLng())
                .geoHash(location.getGeoHash())
                .build();
    }
}
