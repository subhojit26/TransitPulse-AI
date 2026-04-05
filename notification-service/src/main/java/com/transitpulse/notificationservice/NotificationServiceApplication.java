package com.transitpulse.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.transitpulse.notificationservice",
        "com.transitpulse.common.exception"
})
@EntityScan(basePackages = "com.transitpulse.common.entity")
@EnableJpaRepositories(basePackages = "com.transitpulse.notificationservice.repository")
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
