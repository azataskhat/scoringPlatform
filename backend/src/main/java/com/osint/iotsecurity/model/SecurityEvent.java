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
@Table("security_events")
public class SecurityEvent {

    @Id
    private Long id;

    @Column("device_id")
    private Long deviceId;

    @Column("event_type")
    private String eventType;

    private String severity;

    private String description;

    @Column("source_id")
    private Long sourceId;

    @Column("event_time")
    private LocalDateTime eventTime;
}
