package com.osint.iotsecurity.controller;

import com.osint.iotsecurity.dto.DashboardStats;
import com.osint.iotsecurity.model.SecurityEvent;
import com.osint.iotsecurity.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final OsintSourceRepository sourceRepository;
    private final IoTDeviceRepository deviceRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final ScoringResultRepository scoringResultRepository;
    private final SecurityEventRepository eventRepository;

    @GetMapping("/stats")
    public Mono<DashboardStats> getStats() {
        Mono<Long> totalSources = sourceRepository.count();
        Mono<Long> activeSources = sourceRepository.findByActiveTrue().count();
        Mono<Long> totalDevices = deviceRepository.countAll();
        Mono<Long> totalVulns = vulnerabilityRepository.countAll();
        Mono<Long> criticalVulns = vulnerabilityRepository.countBySeverity("CRITICAL");
        Mono<Long> totalEvents = eventRepository.countAll();
        Mono<Double> avgScore = scoringResultRepository.findAllOrderByCalculatedAtDesc()
                .map(r -> r.getTotalScore())
                .collectList()
                .map(scores -> scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

        return Mono.zip(totalSources, activeSources, totalDevices, totalVulns, criticalVulns, totalEvents, avgScore)
                .map(t -> DashboardStats.builder()
                        .totalSources(t.getT1())
                        .activeSources(t.getT2())
                        .totalDevices(t.getT3())
                        .totalVulnerabilities(t.getT4())
                        .criticalVulnerabilities(t.getT5())
                        .totalEvents(t.getT6())
                        .averageScore(Math.round(t.getT7() * 100.0) / 100.0)
                        .build());
    }

    @GetMapping("/events/latest")
    public Flux<SecurityEvent> latestEvents(@RequestParam(defaultValue = "10") int limit) {
        return eventRepository.findLatest(limit);
    }
}
