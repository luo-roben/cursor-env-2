package com.review.module.llm.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PromptCacheService {

    private static final Duration DEFAULT_TTL = Duration.ofHours(1);
    private static final Duration SYSTEM_PROMPT_TTL = Duration.ofHours(24);

    private static final String PREFIX_SYSTEM = "prompt:sys:";
    private static final String PREFIX_LAW = "prompt:law:";
    private static final String PREFIX_RULES = "prompt:rules:";

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    private final ConcurrentHashMap<String, CacheEntry> inMemoryCache = new ConcurrentHashMap<>();
    private volatile boolean redisAvailable = true;

    public String getCachedResponse(String promptHash) {
        if (redisAvailable && stringRedisTemplate != null) {
            try {
                return stringRedisTemplate.opsForValue().get(promptHash);
            } catch (Exception e) {
                log.warn("Redis unavailable for read, falling back to in-memory cache: {}", e.getMessage());
                redisAvailable = false;
            }
        }
        CacheEntry entry = inMemoryCache.get(promptHash);
        if (entry != null && !entry.isExpired()) {
            return entry.value;
        }
        if (entry != null) {
            inMemoryCache.remove(promptHash);
        }
        return null;
    }

    public void cacheResponse(String promptHash, String response, Duration ttl) {
        Duration effectiveTtl = ttl != null ? ttl : DEFAULT_TTL;
        if (redisAvailable && stringRedisTemplate != null) {
            try {
                stringRedisTemplate.opsForValue().set(promptHash, response, effectiveTtl);
                return;
            } catch (Exception e) {
                log.warn("Redis unavailable for write, falling back to in-memory cache: {}", e.getMessage());
                redisAvailable = false;
            }
        }
        inMemoryCache.put(promptHash, new CacheEntry(response, System.currentTimeMillis() + effectiveTtl.toMillis()));
    }

    public String computeHash(String prompt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(prompt.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public String getSystemPromptCacheKey(String systemPromptHash) {
        return PREFIX_SYSTEM + systemPromptHash;
    }

    public String getLawContextCacheKey(String contentType, String productType) {
        String combined = (contentType != null ? contentType : "") + ":" + (productType != null ? productType : "");
        return PREFIX_LAW + computeHash(combined);
    }

    public String getTenantRulesCacheKey(Long tenantId, String rulesHash) {
        return PREFIX_RULES + tenantId + ":" + rulesHash;
    }

    public Duration getSystemPromptTtl() {
        return SYSTEM_PROMPT_TTL;
    }

    public Duration getDefaultTtl() {
        return DEFAULT_TTL;
    }

    private static class CacheEntry {
        final String value;
        final long expiresAt;

        CacheEntry(String value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
