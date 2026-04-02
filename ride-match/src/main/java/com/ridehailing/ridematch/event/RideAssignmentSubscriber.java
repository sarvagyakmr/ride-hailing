package com.ridehailing.ridematch.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.ridematch.service.RideAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RideAssignmentSubscriber {

    private final RideAssignmentService rideAssignmentService;
    private final ObjectMapper objectMapper;

    public void onMessage(String message) {
        log.info("Received ride requested event: {}", message);

        try {
            RideRequestedEvent event = objectMapper.readValue(message, RideRequestedEvent.class);
            log.info("Processing ride assignment for rideId: {}", event.getRideId());

            boolean processed = rideAssignmentService.processRideAssignment(event.getRideId());

            if (processed) {
                log.info("Successfully processed ride assignment for ride: {} - offers sent to drivers", event.getRideId());
            } else {
                log.warn("Failed to process ride assignment for ride: {} - no offers sent", event.getRideId());
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to parse RideRequestedEvent: {}", message, e);
        } catch (Exception e) {
            log.error("Error processing ride assignment for message: {}", message, e);
        }
    }
}
