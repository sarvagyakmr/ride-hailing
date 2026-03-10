package com.ridehailing.ridematch.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateVehicleLocationRequest {
    private Long locationId;
}