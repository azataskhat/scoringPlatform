package com.osint.iotsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoringWeightsDto {

    private Double reliabilityWeight;
    private Double timelinessWeight;
    private Double completenessWeight;
    private Double accessibilityWeight;
}
