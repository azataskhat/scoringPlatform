package com.osint.iotsecurity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osint.iotsecurity.dto.IngestRequest;
import com.osint.iotsecurity.dto.IngestResponse;
import com.osint.iotsecurity.model.IoTDevice;
import com.osint.iotsecurity.model.SecurityEvent;
import com.osint.iotsecurity.model.Vulnerability;
import com.osint.iotsecurity.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestService {

    private final OsintSourceRepository sourceRepository;
    private final IoTDeviceRepository deviceRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final SecurityEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Обрабатывает входящие данные от Python-коллектора.
     */
    public Mono<IngestResponse> ingest(IngestRequest request) {
        log.info("Ingesting data from source: {}, items: {}", request.getSource(), request.getData().size());

        return sourceRepository.findByNameIgnoreCase(request.getSource())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Unknown source: " + request.getSource())))
                .flatMap(source -> {
                    Long sourceId = source.getId();
                    AtomicInteger devCount = new AtomicInteger(0);
                    AtomicInteger vulnCount = new AtomicInteger(0);

                    return Flux.fromIterable(request.getData())
                            .flatMap(deviceData -> {
                                String rawJson;
                                try {
                                    rawJson = objectMapper.writeValueAsString(deviceData.getRawData());
                                } catch (Exception e) {
                                    rawJson = "{}";
                                }

                                IoTDevice device = IoTDevice.builder()
                                        .sourceId(sourceId)
                                        .ipAddress(deviceData.getIpAddress())
                                        .port(deviceData.getPort())
                                        .protocol(deviceData.getProtocol())
                                        .deviceType(deviceData.getDeviceType())
                                        .manufacturer(deviceData.getManufacturer())
                                        .firmwareVersion(deviceData.getFirmwareVersion())
                                        .city(deviceData.getCity() != null ? deviceData.getCity() : "Almaty")
                                        .latitude(deviceData.getLatitude())
                                        .longitude(deviceData.getLongitude())
                                        .rawData(rawJson)
                                        .discoveredAt(LocalDateTime.now())
                                        .build();

                                return deviceRepository.save(device)
                                        .flatMap(savedDevice -> {
                                            devCount.incrementAndGet();

                                            // Create security event for new exposure
                                            SecurityEvent event = SecurityEvent.builder()
                                                    .deviceId(savedDevice.getId())
                                                    .eventType("new_exposure")
                                                    .severity("MEDIUM")
                                                    .description("New IoT device discovered: " + savedDevice.getIpAddress() + ":" + savedDevice.getPort())
                                                    .sourceId(sourceId)
                                                    .eventTime(LocalDateTime.now())
                                                    .build();

                                            Mono<SecurityEvent> eventMono = eventRepository.save(event);

                                            if (deviceData.getVulnerabilities() != null && !deviceData.getVulnerabilities().isEmpty()) {
                                                return eventMono.thenMany(
                                                    Flux.fromIterable(deviceData.getVulnerabilities())
                                                        .flatMap(v -> {
                                                            Vulnerability vuln = Vulnerability.builder()
                                                                    .deviceId(savedDevice.getId())
                                                                    .cveId(v.getCveId())
                                                                    .severity(v.getSeverity())
                                                                    .cvssScore(v.getCvssScore())
                                                                    .description(v.getDescription())
                                                                    .sourceId(sourceId)
                                                                    .detectedAt(LocalDateTime.now())
                                                                    .build();
                                                            vulnCount.incrementAndGet();
                                                            return vulnerabilityRepository.save(vuln);
                                                        })
                                                ).then(Mono.just(savedDevice));
                                            }
                                            return eventMono.thenReturn(savedDevice);
                                        });
                            })
                            .then(Mono.fromCallable(() -> IngestResponse.builder()
                                    .status("ok")
                                    .devicesIngested(devCount.get())
                                    .vulnerabilitiesIngested(vulnCount.get())
                                    .message("Data ingested from " + request.getSource())
                                    .build()));
                });
    }
}
