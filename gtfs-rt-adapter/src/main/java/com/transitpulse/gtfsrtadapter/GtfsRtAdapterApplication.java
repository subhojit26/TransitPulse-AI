package com.transitpulse.gtfsrtadapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GtfsRtAdapterApplication {
    public static void main(String[] args) {
        SpringApplication.run(GtfsRtAdapterApplication.class, args);
    }
}
