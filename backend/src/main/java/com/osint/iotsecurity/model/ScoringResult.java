package com.osint.iotsecurity.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("scoring_results")
public class ScoringResult {

    @Id
    private Long id;

    @Column("source_id")
    private Long sourceId;

    @Column("reliability_score")
    private Double reliabilityScore;

    @Column("timeliness_score")
    private Double timelinessScore;

    @Column("completeness_score")
    private Double completenessScore;

    @Column("accessibility_score")
    private Double accessibilityScore;

    @Column("total_score")
    private Double totalScore;

    private String parameters;

    @Column("calculated_at")
    private LocalDateTime calculatedAt;
}
