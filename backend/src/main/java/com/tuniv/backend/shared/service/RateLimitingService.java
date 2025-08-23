package com.tuniv.backend.shared.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    // A map to store a bucket for each user, identified by their username.
    private final Map<String, Bucket> userBuckets = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String key) {
        // Get the bucket for the user, or create a new one if it doesn't exist.
        return userBuckets.computeIfAbsent(key, this::newBucket);
    }

    private Bucket newBucket(String key) {
        // This configures the bucket: Allow 20 actions per minute.
        Bandwidth limit = Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}