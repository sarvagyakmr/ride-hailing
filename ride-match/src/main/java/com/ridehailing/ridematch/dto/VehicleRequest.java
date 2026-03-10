package com.ridehailing.ridematch.dto;

import com.ridehailing.ridematch.enums.VehicleStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleRequest {
    private Long locationId;
    private VehicleStatus status;
}