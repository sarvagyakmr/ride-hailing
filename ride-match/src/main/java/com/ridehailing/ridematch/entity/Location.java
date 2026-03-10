package com.ridehailing.ridematch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lat", nullable = false, precision = 10, scale = 8)
    private BigDecimal lat;

    @Column(name = "lng", nullable = false, precision = 11, scale = 8)
    private BigDecimal lng;

    @Column(name = "geo_hash", length = 12, unique = true)
    private String geoHash;
}
