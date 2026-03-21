package com.example.library.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Security security) {

    public record Security(Cors cors) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
