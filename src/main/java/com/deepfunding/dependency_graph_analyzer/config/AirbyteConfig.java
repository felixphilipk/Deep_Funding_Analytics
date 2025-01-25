package com.deepfunding.dependency_graph_analyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;


// AirbyteConfig.java
@Configuration
@ConfigurationProperties(prefix = "airbyte")
@Getter
@Setter
public class AirbyteConfig {
    private String apiUrl;
    private String apiKey;
    private String sourceId;
    private String destinationId;
}

