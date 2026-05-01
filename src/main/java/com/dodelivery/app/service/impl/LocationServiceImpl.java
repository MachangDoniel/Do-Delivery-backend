package com.dodelivery.app.service.impl;

import com.dodelivery.app.dto.response.RiderLocationResponse;
import com.dodelivery.app.service.LocationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages rider live-location in Redis.
 *
 * <p>Key format: {@code rider_location:{riderId}}
 * <p>Value: JSON-serialized {@link RiderLocationResponse}
 * <p>TTL: {@code app.redis.rider-location-ttl-seconds} (default 300s = 5 min)
 * — auto-expires when the rider stops sending updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private static final String KEY_PREFIX = "rider_location:";

    private final StringRedisTemplate redisTemplate;

    @Value("${app.redis.rider-location-ttl-seconds}")
    private long locationTtlSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public void saveRiderLocation(UUID riderId, double lat, double lng) {
        RiderLocationResponse location = new RiderLocationResponse(riderId, lat, lng, Instant.now());
        try {
            String json = objectMapper.writeValueAsString(location);
            redisTemplate.opsForValue()
                    .set(KEY_PREFIX + riderId, json, Duration.ofSeconds(locationTtlSeconds));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize rider location for riderId={}", riderId, e);
        }
    }

    @Override
    public Optional<RiderLocationResponse> getRiderLocation(UUID riderId) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + riderId);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, RiderLocationResponse.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize rider location for riderId={}", riderId, e);
            return Optional.empty();
        }
    }

    @Override
    public void removeRiderLocation(UUID riderId) {
        redisTemplate.delete(KEY_PREFIX + riderId);
    }
}
