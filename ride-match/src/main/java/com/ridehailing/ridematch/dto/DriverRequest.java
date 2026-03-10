package com.ridehailing.ridematch.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverRequest {
    private Long userId;
    private Long vehicleId;
}