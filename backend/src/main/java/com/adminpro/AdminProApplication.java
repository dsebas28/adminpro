package com.adminpro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AdminProApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminProApplication.class, args);
    }
}
