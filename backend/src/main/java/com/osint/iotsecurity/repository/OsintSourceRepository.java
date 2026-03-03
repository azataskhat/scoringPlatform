package com.osint.iotsecurity.repository;

import com.osint.iotsecurity.model.OsintSource;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OsintSourceRepository extends ReactiveCrudRepository<OsintSource, Long> {

    Flux<OsintSource> findByActiveTrue();

    Mono<OsintSource> findByNameIgnoreCase(String name);

    @Query("SELECT * FROM osint_sources WHERE type = :type")
    Flux<OsintSource> findByType(String type);
}
