package com.custom.kafka.dlt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.custom.kafka")
@EnableScheduling
@EnableAsync
public class KafkaDltApplication {
    public static void main(String[] args) {
        SpringApplication.run(KafkaDltApplication.class, args);
    }
}
