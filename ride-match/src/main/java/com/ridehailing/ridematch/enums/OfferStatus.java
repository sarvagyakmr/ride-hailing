package com.ridehailing.ridematch.enums;

public enum OfferStatus {
    PENDING,    // Offer sent to driver, waiting for response
    ACCEPTED,   // Driver accepted the ride
    REJECTED,   // Driver rejected the ride
    EXPIRED     // Offer timed out without response
}
