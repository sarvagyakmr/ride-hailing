package com.ridehailing.ridematch.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverResponseRequest {

    @NotNull(message = "Driver ID is required")
    private Long driverId;

    private String reason;  // Optional reason for rejection
}
