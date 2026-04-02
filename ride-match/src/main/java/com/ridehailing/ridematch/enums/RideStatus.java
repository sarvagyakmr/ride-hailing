package com.ridehailing.ridematch.enums;

public enum RideStatus {
    REQUESTED,      // Initial state when ride is created
    OFFERED,        // Ride offer sent to driver(s), waiting for response
    IN_ROUTE,       // Driver accepted and is en route to pickup
    CANCELLED,      // Ride was cancelled
    COMPLETE        // Ride completed successfully
}
