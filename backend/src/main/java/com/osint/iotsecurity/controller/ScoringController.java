package com.osint.iotsecurity.controller;

import com.osint.iotsecurity.dto.ScoringWeightsDto;
import com.osint.iotsecurity.model.ScoringResult;
import com.osint.iotsecurity.service.ScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/scoring")
@RequiredArgsConstructor
public class ScoringController {

    private final ScoringService scoringService;

    /**
     * SSE-стрим обновлений скоринга в реальном времени.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ScoringResult>> stream() {
        // Heartbeat каждые 15 секунд, чтобы соединение не рвалось
        Flux<ServerSentEvent<ScoringResult>> heartbeat = Flux.interval(Duration.ofSeconds(15))
                .map(i -> ServerSentEvent.<ScoringResult>builder()
                        .comment("heartbeat")
                        .build());

        Flux<ServerSentEvent<ScoringResult>> updates = scoringService.getScoringSink()
                .map(result -> ServerSentEvent.<ScoringResult>builder()
                        .id(String.valueOf(result.getId()))
                        .event("scoring-update")
                        .data(result)
                        .build());

        return Flux.merge(heartbeat, updates);
    }

    /**
     * Ручной запуск скоринга для всех активных источников.
     */
    @PostMapping("/run")
    public Flux<ScoringResult> runAll() {
        return scoringService.runScoringForAll();
    }

    /**
     * Запуск скоринга для конкретного источника.
     */
    @PostMapping("/run/{sourceId}")
    public Mono<ScoringResult> runForSource(@PathVariable Long sourceId) {
        return scoringService.runScoringForSource(sourceId);
    }

    /**
     * Все результаты скоринга.
     */
    @GetMapping("/results")
    public Flux<ScoringResult> getResults(
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        if (sourceId != null) return scoringService.getResultsBySource(sourceId);
        if (from != null && to != null) return scoringService.getResultsByPeriod(from, to);
        return scoringService.getAllResults();
    }

    /**
     * Обновление весовых коэффициентов.
     */
    @PutMapping("/weights")
    public Mono<Void> updateWeights(@RequestBody ScoringWeightsDto weights) {
        scoringService.updateWeights(weights);
        return Mono.empty();
    }
}
