package com.osint.iotsecurity.controller;

import com.osint.iotsecurity.model.OsintSource;
import com.osint.iotsecurity.repository.OsintSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/sources")
@RequiredArgsConstructor
public class SourceController {

    private final OsintSourceRepository repository;

    @GetMapping
    public Flux<OsintSource> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Mono<OsintSource> getById(@PathVariable Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<OsintSource> create(@RequestBody OsintSource source) {
        source.setCreatedAt(LocalDateTime.now());
        source.setUpdatedAt(LocalDateTime.now());
        return repository.save(source);
    }

    @PutMapping("/{id}")
    public Mono<OsintSource> update(@PathVariable Long id, @RequestBody OsintSource updated) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .flatMap(existing -> {
                    if (updated.getName() != null)    existing.setName(updated.getName());
                    if (updated.getType() != null)    existing.setType(updated.getType());
                    if (updated.getBaseUrl() != null)  existing.setBaseUrl(updated.getBaseUrl());
                    if (updated.getActive() != null)   existing.setActive(updated.getActive());
                    if (updated.getUpdateIntervalMinutes() != null)
                        existing.setUpdateIntervalMinutes(updated.getUpdateIntervalMinutes());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return repository.save(existing);
                });
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable Long id) {
        return repository.deleteById(id);
    }
}
