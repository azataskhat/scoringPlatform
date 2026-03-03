package com.osint.iotsecurity.repository;

import com.osint.iotsecurity.model.SecurityEvent;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SecurityEventRepository extends ReactiveCrudRepository<SecurityEvent, Long> {

    Flux<SecurityEvent> findByDeviceId(Long deviceId);

    Flux<SecurityEvent> findByEventType(String eventType);

    @Query("SELECT * FROM security_events ORDER BY event_time DESC LIMIT :limit")
    Flux<SecurityEvent> findLatest(int limit);

    @Query("SELECT COUNT(*) FROM security_events")
    Mono<Long> countAll();
}
