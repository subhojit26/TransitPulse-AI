package com.transitpulse.busservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.transitpulse.busservice",
        "com.transitpulse.common.exception"
})
@EntityScan(basePackages = "com.transitpulse.common.entity")
@EnableJpaRepositories(basePackages = "com.transitpulse.busservice.repository")
public class BusServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BusServiceApplication.class, args);
    }
}
