package com.transitpulse.commuterservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.transitpulse.commuterservice",
        "com.transitpulse.common.exception"
})
@EntityScan(basePackages = "com.transitpulse.common.entity")
@EnableJpaRepositories(basePackages = "com.transitpulse.commuterservice.repository")
public class CommuterServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CommuterServiceApplication.class, args);
    }
}
