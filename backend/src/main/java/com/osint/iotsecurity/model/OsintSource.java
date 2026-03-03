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
@Table("osint_sources")
public class OsintSource {

    @Id
    private Long id;

    private String name;

    private String type;

    @Column("base_url")
    private String baseUrl;

    @Column("api_key_ref")
    private String apiKeyRef;

    @Builder.Default
    private Boolean active = true;

    @Column("update_interval_minutes")
    @Builder.Default
    private Integer updateIntervalMinutes = 60;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
