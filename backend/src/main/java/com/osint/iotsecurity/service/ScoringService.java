package com.osint.iotsecurity.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.osint.iotsecurity.config.ScoringProperties;
import com.osint.iotsecurity.dto.ScoringWeightsDto;
import com.osint.iotsecurity.model.IoTDevice;
import com.osint.iotsecurity.model.ScoringResult;
import com.osint.iotsecurity.model.Vulnerability;
import com.osint.iotsecurity.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringService {

    private final ScoringResultRepository scoringResultRepository;
    private final OsintSourceRepository osintSourceRepository;
    private final IoTDeviceRepository deviceRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final ScoringProperties properties;
    private final ObjectMapper objectMapper;

    private final Sinks.Many<ScoringResult> scoringSink =
            Sinks.many().multicast().onBackpressureBuffer(256);

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Запуск скоринга для всех активных источников.
     */
    public Flux<ScoringResult> runScoringForAll() {
        return osintSourceRepository.findByActiveTrue()
                .flatMap(source -> calculateAndSave(source.getId()))
                .doOnNext(result -> {
                    log.info("Scoring completed: source={}, total={}", result.getSourceId(), result.getTotalScore());
                    scoringSink.tryEmitNext(result);
                });
    }

    /**
     * Запуск скоринга для конкретного источника.
     */
    public Mono<ScoringResult> runScoringForSource(Long sourceId) {
        return calculateAndSave(sourceId)
                .doOnNext(result -> {
                    log.info("Scoring completed: source={}, total={}", result.getSourceId(), result.getTotalScore());
                    scoringSink.tryEmitNext(result);
                });
    }

    /**
     * SSE-стрим обновлений.
     */
    public Flux<ScoringResult> getScoringSink() {
        return scoringSink.asFlux();
    }

    /**
     * Обновить весовые коэффициенты.
     */
    public void updateWeights(ScoringWeightsDto dto) {
        if (dto.getReliabilityWeight() != null)  properties.getWeights().setReliability(dto.getReliabilityWeight());
        if (dto.getTimelinessWeight() != null)   properties.getWeights().setTimeliness(dto.getTimelinessWeight());
        if (dto.getCompletenessWeight() != null)  properties.getWeights().setCompleteness(dto.getCompletenessWeight());
        if (dto.getAccessibilityWeight() != null) properties.getWeights().setAccessibility(dto.getAccessibilityWeight());
        log.info("Weights updated: R={}, T={}, C={}, A={}",
                properties.getWeights().getReliability(),
                properties.getWeights().getTimeliness(),
                properties.getWeights().getCompleteness(),
                properties.getWeights().getAccessibility());
    }

    public Flux<ScoringResult> getAllResults() {
        return scoringResultRepository.findAllOrderByCalculatedAtDesc();
    }

    public Flux<ScoringResult> getResultsBySource(Long sourceId) {
        return scoringResultRepository.findBySourceId(sourceId);
    }

    public Flux<ScoringResult> getResultsByPeriod(LocalDateTime from, LocalDateTime to) {
        return scoringResultRepository.findByPeriod(from, to);
    }

    // -----------------------------------------------------------------------
    // Scoring calculation
    // -----------------------------------------------------------------------

    private Mono<ScoringResult> calculateAndSave(Long sourceId) {
        Mono<List<IoTDevice>> devicesMono = deviceRepository.findBySourceId(sourceId).collectList();
        Mono<List<Vulnerability>> vulnsMono = vulnerabilityRepository.findBySourceId(sourceId).collectList();
        Mono<List<IoTDevice>> allDevicesMono = deviceRepository.findAll().collectList();
        Mono<Optional<ScoringResult>> previousMono = scoringResultRepository.findLatestBySourceId(sourceId)
                .map(Optional::of).defaultIfEmpty(Optional.empty());

        return Mono.zip(devicesMono, vulnsMono, allDevicesMono, previousMono)
                .flatMap(tuple -> {
                    List<IoTDevice> devices = tuple.getT1();
                    List<Vulnerability> vulns = tuple.getT2();
                    List<IoTDevice> allDevices = tuple.getT3();
                    Optional<ScoringResult> previous = tuple.getT4();

                    Map<String, Double> params = computeParameters(devices, vulns, allDevices, sourceId);

                    double R = computeR(params);
                    double T = computeT(params);
                    double C = computeC(params);
                    double A = computeA(params);

                    var w = properties.getWeights();
                    double totalRaw = w.getReliability() * R
                                    + w.getTimeliness() * T
                                    + w.getCompleteness() * C
                                    + w.getAccessibility() * A;

                    // EMA smoothing
                    double total = previous
                            .map(prev -> properties.getEmaBeta() * totalRaw + (1 - properties.getEmaBeta()) * prev.getTotalScore())
                            .orElse(totalRaw);

                    String paramsJson;
                    try {
                        paramsJson = objectMapper.writeValueAsString(params);
                    } catch (JsonProcessingException e) {
                        paramsJson = "{}";
                    }

                    ScoringResult result = ScoringResult.builder()
                            .sourceId(sourceId)
                            .reliabilityScore(R)
                            .timelinessScore(T)
                            .completenessScore(C)
                            .accessibilityScore(A)
                            .totalScore(Math.round(total * 10000.0) / 10000.0)
                            .parameters(paramsJson)
                            .calculatedAt(LocalDateTime.now())
                            .build();

                    return scoringResultRepository.save(result);
                });
    }

    /**
     * Вычисляет все сырые параметры r1..a2.
     */
    private Map<String, Double> computeParameters(List<IoTDevice> devices,
                                                   List<Vulnerability> vulns,
                                                   List<IoTDevice> allDevices,
                                                   Long sourceId) {
        Map<String, Double> p = new LinkedHashMap<>();

        // --- Reliability ---
        // r1: точность (доля записей с заполненным IP и портом)
        long validCount = devices.stream()
                .filter(d -> d.getIpAddress() != null && d.getPort() != null && d.getPort() > 0)
                .count();
        p.put("r1", devices.isEmpty() ? 0.0 : (double) validCount / devices.size());

        // r2: согласованность (Jaccard с другими источниками)
        Set<String> thisIps = new HashSet<>();
        devices.forEach(d -> thisIps.add(d.getIpAddress()));
        Set<String> otherIps = new HashSet<>();
        allDevices.stream()
                .filter(d -> !sourceId.equals(d.getSourceId()))
                .forEach(d -> otherIps.add(d.getIpAddress()));
        Set<String> intersection = new HashSet<>(thisIps);
        intersection.retainAll(otherIps);
        Set<String> union = new HashSet<>(thisIps);
        union.addAll(otherIps);
        p.put("r2", union.isEmpty() ? 0.0 : (double) intersection.size() / union.size());

        // r3: верифицируемость (доля устройств с CVE)
        Set<Long> devicesWithCve = new HashSet<>();
        vulns.forEach(v -> devicesWithCve.add(v.getDeviceId()));
        long sourceDevicesWithCve = devices.stream()
                .filter(d -> devicesWithCve.contains(d.getId()))
                .count();
        p.put("r3", devices.isEmpty() ? 0.0 : (double) sourceDevicesWithCve / devices.size());

        // --- Timeliness ---
        // t1: задержка обновления (exp decay)
        double avgHoursAgo = devices.stream()
                .filter(d -> d.getDiscoveredAt() != null)
                .mapToDouble(d -> Duration.between(d.getDiscoveredAt(), LocalDateTime.now()).toHours())
                .average().orElse(24.0);
        p.put("t1", Math.exp(-properties.getLambdaDecay() * avgHoursAgo));

        // t2: частота (нормализованная — чем больше устройств, тем чаще сбор)
        p.put("t2", Math.min(1.0, devices.size() / 100.0));

        // --- Completeness ---
        // c1: полнота полей
        double fieldCompleteness = devices.stream()
                .mapToDouble(this::fieldFillRate)
                .average().orElse(0.0);
        p.put("c1", fieldCompleteness);

        // c2: покрытие типов устройств
        long distinctTypes = devices.stream()
                .map(IoTDevice::getDeviceType)
                .filter(Objects::nonNull)
                .distinct().count();
        p.put("c2", Math.min(1.0, distinctTypes / 6.0)); // 6 основных типов

        // c3: глубина (наличие CVE, баннеров, raw data)
        long deepRecords = devices.stream()
                .filter(d -> d.getRawData() != null && !d.getRawData().isBlank())
                .count();
        double cveDepth = devices.isEmpty() ? 0 : (double) sourceDevicesWithCve / devices.size();
        double rawDepth = devices.isEmpty() ? 0 : (double) deepRecords / devices.size();
        p.put("c3", (cveDepth + rawDepth) / 2.0);

        // --- Accessibility ---
        // a1: доступность API (имитация — для реальной системы нужен мониторинг)
        p.put("a1", 0.95);

        // a2: скорость ответа (имитация — нормализованная)
        p.put("a2", 0.88);

        return p;
    }

    private double fieldFillRate(IoTDevice d) {
        int total = 9;
        int filled = 0;
        if (d.getIpAddress() != null) filled++;
        if (d.getPort() != null) filled++;
        if (d.getProtocol() != null) filled++;
        if (d.getDeviceType() != null) filled++;
        if (d.getManufacturer() != null) filled++;
        if (d.getFirmwareVersion() != null) filled++;
        if (d.getCity() != null) filled++;
        if (d.getLatitude() != null) filled++;
        if (d.getLongitude() != null) filled++;
        return (double) filled / total;
    }

    // Sub-dimension aggregation (equal sub-weights by default)
    private double computeR(Map<String, Double> p) {
        return (p.getOrDefault("r1", 0.0) + p.getOrDefault("r2", 0.0) + p.getOrDefault("r3", 0.0)) / 3.0;
    }

    private double computeT(Map<String, Double> p) {
        return (p.getOrDefault("t1", 0.0) + p.getOrDefault("t2", 0.0)) / 2.0;
    }

    private double computeC(Map<String, Double> p) {
        return (p.getOrDefault("c1", 0.0) + p.getOrDefault("c2", 0.0) + p.getOrDefault("c3", 0.0)) / 3.0;
    }

    private double computeA(Map<String, Double> p) {
        return (p.getOrDefault("a1", 0.0) + p.getOrDefault("a2", 0.0)) / 2.0;
    }
}
