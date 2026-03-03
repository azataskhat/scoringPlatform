package com.osint.iotsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {

    private long totalSources;
    private long activeSources;
    private long totalDevices;
    private long totalVulnerabilities;
    private long criticalVulnerabilities;
    private long totalEvents;
    private double averageScore;
}
