package com.osint.iotsecurity.repository;

import com.osint.iotsecurity.model.IoTDevice;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IoTDeviceRepository extends ReactiveCrudRepository<IoTDevice, Long> {

    Flux<IoTDevice> findBySourceId(Long sourceId);

    Flux<IoTDevice> findByCity(String city);

    Flux<IoTDevice> findByDeviceType(String deviceType);

    @Query("SELECT * FROM iot_devices WHERE latitude IS NOT NULL AND longitude IS NOT NULL")
    Flux<IoTDevice> findAllWithCoordinates();

    @Query("SELECT COUNT(*) FROM iot_devices")
    Mono<Long> countAll();

    @Query("SELECT * FROM iot_devices WHERE city = :city AND device_type = :type")
    Flux<IoTDevice> findByCityAndType(String city, String type);
}
