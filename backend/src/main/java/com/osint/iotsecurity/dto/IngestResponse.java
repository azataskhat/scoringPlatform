package com.osint.iotsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestResponse {

    private String status;
    private int devicesIngested;
    private int vulnerabilitiesIngested;
    private String message;
}
