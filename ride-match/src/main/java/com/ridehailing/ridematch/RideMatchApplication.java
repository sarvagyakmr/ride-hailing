package com.ridehailing.ridematch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RideMatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(RideMatchApplication.class, args);
    }
}
