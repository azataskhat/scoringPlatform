package com.osint.iotsecurity.controller;

import com.osint.iotsecurity.dto.IngestRequest;
import com.osint.iotsecurity.dto.IngestResponse;
import com.osint.iotsecurity.service.IngestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/ingest")
@RequiredArgsConstructor
public class IngestController {

    private final IngestService ingestService;

    @PostMapping
    public Mono<IngestResponse> ingest(@RequestBody IngestRequest request) {
        return ingestService.ingest(request);
    }
}
