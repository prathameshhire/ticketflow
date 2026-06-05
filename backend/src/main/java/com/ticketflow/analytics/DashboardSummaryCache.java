package com.ticketflow.analytics;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ticketflow.analytics.dto.DashboardSummaryResponse;

@Component
public class DashboardSummaryCache {

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Duration ttl;

    public DashboardSummaryCache(@Value("${ticketflow.dashboard.cache-ttl-seconds:30}") long ttlSeconds) {
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public DashboardSummaryResponse getOrCompute(String key, Supplier<DashboardSummaryResponse> supplier) {
        Instant now = Instant.now();
        CacheEntry cached = cache.get(key);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.summary();
        }

        DashboardSummaryResponse summary = supplier.get();
        cache.put(key, new CacheEntry(summary, now.plus(ttl)));
        return summary;
    }

    public void invalidateAll() {
        cache.clear();
    }

    private record CacheEntry(DashboardSummaryResponse summary, Instant expiresAt) {
    }
}

