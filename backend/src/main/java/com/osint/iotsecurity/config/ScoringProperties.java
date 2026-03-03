package com.osint.iotsecurity.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "scoring")
public class ScoringProperties {

    private Weights weights = new Weights();
    private double emaBeta = 0.3;
    private double lambdaDecay = 0.05;

    @Data
    public static class Weights {
        private double reliability = 0.35;
        private double timeliness = 0.25;
        private double completeness = 0.25;
        private double accessibility = 0.15;
    }
}
