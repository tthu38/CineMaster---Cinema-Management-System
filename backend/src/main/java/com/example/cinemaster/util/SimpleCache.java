package com.example.cinemaster.util;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * SimpleCache: cache tạm dữ liệu 1–5 phút trong bộ nhớ JVM (không cần Redis).
 */
public class SimpleCache<T> {
    private final Map<String, CacheEntry<T>> cache = new HashMap<>();
    private final long ttlMillis; // time-to-live

    public SimpleCache(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    private static class CacheEntry<T> {
        T value;
        Instant expiry;
    }

    /**
     * Lấy dữ liệu từ cache, nếu hết hạn thì gọi supplier để refresh.
     */
    public T get(String key, Supplier<T> supplier) {
        CacheEntry<T> entry = cache.get(key);
        if (entry == null || Instant.now().isAfter(entry.expiry)) {
            try {
                T value = supplier.get();
                entry = new CacheEntry<>();
                entry.value = value;
                entry.expiry = Instant.now().plusMillis(ttlMillis);
                cache.put(key, entry);
            } catch (Exception e) {
                System.err.println("⚠️ Lỗi khi nạp lại cache key=" + key + ": " + e.getMessage());
                if (entry != null) return entry.value; // fallback dùng cache cũ nếu có
                return null;
            }
        }
        return entry.value;
    }

    public void clear(String key) {
        cache.remove(key);
    }

    public void clearAll() {
        cache.clear();
    }
}
