package com.osint.iotsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngestRequest {

    private String source;
    private String collectedAt;
    private List<DeviceData> data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceData {
        private String ipAddress;
        private Integer port;
        private String protocol;
        private String deviceType;
        private String manufacturer;
        private String firmwareVersion;
        private String city;
        private Double latitude;
        private Double longitude;
        private Object rawData;
        private List<VulnData> vulnerabilities;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VulnData {
        private String cveId;
        private String severity;
        private Double cvssScore;
        private String description;
    }
}
