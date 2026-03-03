package com.osint.iotsecurity.controller;

import com.osint.iotsecurity.model.IoTDevice;
import com.osint.iotsecurity.repository.IoTDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final IoTDeviceRepository repository;

    @GetMapping
    public Flux<IoTDevice> getAll(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long sourceId) {
        if (city != null && type != null) {
            return repository.findByCityAndType(city, type);
        }
        if (city != null) return repository.findByCity(city);
        if (type != null) return repository.findByDeviceType(type);
        if (sourceId != null) return repository.findBySourceId(sourceId);
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Mono<IoTDevice> getById(@PathVariable Long id) {
        return repository.findById(id);
    }

    @GetMapping("/map")
    public Flux<IoTDevice> getForMap() {
        return repository.findAllWithCoordinates();
    }
}
