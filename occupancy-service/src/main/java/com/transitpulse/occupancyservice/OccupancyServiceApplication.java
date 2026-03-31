package com.transitpulse.occupancyservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.transitpulse.occupancyservice",
        "com.transitpulse.common.exception"
})
@EntityScan(basePackages = "com.transitpulse.common.entity")
@EnableJpaRepositories(basePackages = "com.transitpulse.occupancyservice.repository")
public class OccupancyServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OccupancyServiceApplication.class, args);
    }
}
