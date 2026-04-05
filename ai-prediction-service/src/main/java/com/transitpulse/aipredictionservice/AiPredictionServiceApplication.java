package com.transitpulse.aipredictionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.transitpulse.aipredictionservice",
        "com.transitpulse.common.exception"
})
@EntityScan(basePackages = "com.transitpulse.common.entity")
@EnableJpaRepositories(basePackages = "com.transitpulse.aipredictionservice.repository")
public class AiPredictionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiPredictionServiceApplication.class, args);
    }
}
