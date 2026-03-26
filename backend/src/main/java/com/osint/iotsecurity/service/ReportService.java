package com.osint.iotsecurity.service;

import com.osint.iotsecurity.model.OsintSource;
import com.osint.iotsecurity.model.ScoringResult;
import com.osint.iotsecurity.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ScoringResultRepository scoringResultRepository;
    private final OsintSourceRepository osintSourceRepository;
    private final IoTDeviceRepository deviceRepository;
    private final VulnerabilityRepository vulnerabilityRepository;

    /**
     * Генерирует сводный отчёт за период.
     */
    public Mono<Map<String, Object>> generateReport(LocalDateTime from, LocalDateTime to) {
        // Берём только последний результат скоринга по каждому источнику за период
        Mono<List<ScoringResult>> scoringMono = scoringResultRepository.findByPeriod(from, to).collectList()
                .map(list -> list.stream()
                        .collect(Collectors.toMap(
                                ScoringResult::getSourceId,
                                r -> r,
                                (a, b) -> a.getCalculatedAt() != null && b.getCalculatedAt() != null
                                        && a.getCalculatedAt().isAfter(b.getCalculatedAt()) ? a : b))
                        .values().stream().collect(Collectors.toList()));
        Mono<Long> deviceCountMono = deviceRepository.countAll();
        Mono<Long> vulnCountMono = vulnerabilityRepository.countAll();
        Mono<Long> criticalMono = vulnerabilityRepository.countBySeverity("CRITICAL");
        Mono<Map<Long, String>> sourceNamesMono = osintSourceRepository.findAll().collectList()
                .map(list -> list.stream().collect(Collectors.toMap(OsintSource::getId, OsintSource::getName)));

        return Mono.zip(scoringMono, deviceCountMono, vulnCountMono, criticalMono, sourceNamesMono)
                .map(tuple -> {
                    List<ScoringResult> results = tuple.getT1();
                    Map<Long, String> sourceNames = tuple.getT5();

                    Map<String, Object> report = new LinkedHashMap<>();
                    report.put("period", Map.of("from", from.toString(), "to", to.toString()));
                    report.put("generatedAt", LocalDateTime.now().toString());
                    report.put("totalDevices", tuple.getT2());
                    report.put("totalVulnerabilities", tuple.getT3());
                    report.put("criticalVulnerabilities", tuple.getT4());

                    double avgScore = results.stream()
                            .mapToDouble(ScoringResult::getTotalScore)
                            .average().orElse(0.0);
                    report.put("averageScore", round4(avgScore * 100));

                    report.put("averageReliability", round4(results.stream()
                            .mapToDouble(ScoringResult::getReliabilityScore).average().orElse(0.0) * 100));
                    report.put("averageTimeliness", round4(results.stream()
                            .mapToDouble(ScoringResult::getTimelinessScore).average().orElse(0.0) * 100));
                    report.put("averageCompleteness", round4(results.stream()
                            .mapToDouble(ScoringResult::getCompletenessScore).average().orElse(0.0) * 100));
                    report.put("averageAccessibility", round4(results.stream()
                            .mapToDouble(ScoringResult::getAccessibilityScore).average().orElse(0.0) * 100));

                    List<Map<String, Object>> scoringDetails = new ArrayList<>();
                    results.forEach(r -> {
                        Map<String, Object> detail = new LinkedHashMap<>();
                        detail.put("sourceId", r.getSourceId());
                        detail.put("sourceName", sourceNames.getOrDefault(r.getSourceId(), "Source #" + r.getSourceId()));
                        detail.put("reliability", round4(r.getReliabilityScore() * 100));
                        detail.put("timeliness", round4(r.getTimelinessScore() * 100));
                        detail.put("completeness", round4(r.getCompletenessScore() * 100));
                        detail.put("accessibility", round4(r.getAccessibilityScore() * 100));
                        detail.put("totalScore", round4(r.getTotalScore() * 100));
                        detail.put("calculatedAt", r.getCalculatedAt() != null ? r.getCalculatedAt().toString() : null);
                        scoringDetails.add(detail);
                    });
                    report.put("scoringDetails", scoringDetails);

                    return report;
                });
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
