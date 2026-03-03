package com.osint.iotsecurity.service;

import com.osint.iotsecurity.model.ScoringResult;
import com.osint.iotsecurity.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ScoringResultRepository scoringResultRepository;
    private final IoTDeviceRepository deviceRepository;
    private final VulnerabilityRepository vulnerabilityRepository;

    /**
     * Генерирует сводный отчёт за период.
     */
    public Mono<Map<String, Object>> generateReport(LocalDateTime from, LocalDateTime to) {
        Mono<List<ScoringResult>> scoringMono = scoringResultRepository.findByPeriod(from, to).collectList();
        Mono<Long> deviceCountMono = deviceRepository.countAll();
        Mono<Long> vulnCountMono = vulnerabilityRepository.countAll();
        Mono<Long> criticalMono = vulnerabilityRepository.countBySeverity("CRITICAL");

        return Mono.zip(scoringMono, deviceCountMono, vulnCountMono, criticalMono)
                .map(tuple -> {
                    List<ScoringResult> results = tuple.getT1();

                    Map<String, Object> report = new LinkedHashMap<>();
                    report.put("period", Map.of("from", from.toString(), "to", to.toString()));
                    report.put("generatedAt", LocalDateTime.now().toString());
                    report.put("totalDevices", tuple.getT2());
                    report.put("totalVulnerabilities", tuple.getT3());
                    report.put("criticalVulnerabilities", tuple.getT4());

                    double avgScore = results.stream()
                            .mapToDouble(ScoringResult::getTotalScore)
                            .average().orElse(0.0);
                    report.put("averageScore", Math.round(avgScore * 10000.0) / 10000.0);

                    List<Map<String, Object>> scoringDetails = new ArrayList<>();
                    results.forEach(r -> {
                        Map<String, Object> detail = new LinkedHashMap<>();
                        detail.put("sourceId", r.getSourceId());
                        detail.put("reliability", r.getReliabilityScore());
                        detail.put("timeliness", r.getTimelinessScore());
                        detail.put("completeness", r.getCompletenessScore());
                        detail.put("accessibility", r.getAccessibilityScore());
                        detail.put("total", r.getTotalScore());
                        detail.put("calculatedAt", r.getCalculatedAt().toString());
                        scoringDetails.add(detail);
                    });
                    report.put("scoringResults", scoringDetails);

                    return report;
                });
    }
}
