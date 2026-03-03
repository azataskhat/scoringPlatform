package com.osint.iotsecurity.repository;

import com.osint.iotsecurity.model.ScoringResult;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ScoringResultRepository extends ReactiveCrudRepository<ScoringResult, Long> {

    Flux<ScoringResult> findBySourceId(Long sourceId);

    @Query("SELECT * FROM scoring_results ORDER BY calculated_at DESC")
    Flux<ScoringResult> findAllOrderByCalculatedAtDesc();

    @Query("SELECT * FROM scoring_results WHERE source_id = :sourceId ORDER BY calculated_at DESC LIMIT 1")
    Mono<ScoringResult> findLatestBySourceId(Long sourceId);

    @Query("SELECT * FROM scoring_results WHERE calculated_at >= :from AND calculated_at <= :to ORDER BY calculated_at")
    Flux<ScoringResult> findByPeriod(java.time.LocalDateTime from, java.time.LocalDateTime to);
}
