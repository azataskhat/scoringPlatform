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
@Table("iot_devices")
public class IoTDevice {

    @Id
    private Long id;

    @Column("source_id")
    private Long sourceId;

    @Column("ip_address")
    private String ipAddress;

    private Integer port;

    private String protocol;

    @Column("device_type")
    private String deviceType;

    private String manufacturer;

    @Column("firmware_version")
    private String firmwareVersion;

    @Builder.Default
    private String city = "Almaty";

    private Double latitude;

    private Double longitude;

    @Column("raw_data")
    private String rawData;

    @Column("discovered_at")
    private LocalDateTime discoveredAt;
}
