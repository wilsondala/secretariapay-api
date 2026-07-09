package com.secretariapay.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SecretariaPayApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecretariaPayApiApplication.class, args);
    }
}
