package com.mudit.piingogateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PiingoGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(PiingoGatewayApplication.class, args);
    }

}
