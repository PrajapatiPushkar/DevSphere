package com.devsphere.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DevSphereAuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevSphereAuthServiceApplication.class, args);
    }
}
