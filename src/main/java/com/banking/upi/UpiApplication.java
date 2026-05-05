package com.banking.upi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UpiApplication {
    public static void main(String[] args) {
        SpringApplication.run(UpiApplication.class, args);
    }
}
