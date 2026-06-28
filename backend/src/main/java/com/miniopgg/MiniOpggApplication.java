package com.miniopgg;

import com.miniopgg.config.RiotApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RiotApiProperties.class)
public class MiniOpggApplication {
    public static void main(String[] args) {
        SpringApplication.run(MiniOpggApplication.class, args);
    }
}
