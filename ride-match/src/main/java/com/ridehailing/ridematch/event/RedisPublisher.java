package com.ridehailing.ridematch.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ChannelTopic rideRequestedTopic;
    @Qualifier("driverRideOfferTopic")
    private final ChannelTopic driverRideOfferTopic;
    private final ObjectMapper objectMapper;

    public void publishRideRequested(RideRequestedEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            stringRedisTemplate.convertAndSend(rideRequestedTopic.getTopic(), message);
            log.info("Published RideRequestedEvent for rideId: {}", event.getRideId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize RideRequestedEvent for rideId: {}", event.getRideId(), e);
        }
    }

    public void publishDriverRideOffer(DriverRideOfferEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            stringRedisTemplate.convertAndSend(driverRideOfferTopic.getTopic(), message);
            log.info("Published DriverRideOfferEvent for offerId: {} to driverId: {}",
                    event.getOfferId(), event.getDriverId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize DriverRideOfferEvent for offerId: {}", event.getOfferId(), e);
        }
    }
}
