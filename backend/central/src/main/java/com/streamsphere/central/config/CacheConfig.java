package com.streamsphere.central.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis-backed cache-aside configuration.
 *
 * Cache names map to keys as: streamsphere:central:{cacheName}::{key}
 * Database remains the source of truth; entries are invalidated on writes.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** Stable, human-readable serialized cache values (no JDK binary payloads). */
    GenericJackson2JsonRedisSerializer jsonSerializer() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(om);
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(Duration.ofMinutes(10))
                .prefixCacheNameWith("streamsphere:central:")
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer()));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        // Latest-videos feed: volatile, changes on every upload -> short TTL.
        perCache.put("video:feed", base.entryTtl(Duration.ofMinutes(5)));
        // Single video metadata: moderately volatile.
        perCache.put("video:byId", base.entryTtl(Duration.ofMinutes(30)));
        // Channel summaries: infrequently change.
        perCache.put("channel:byUserEmail", base.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base)
                .withInitialCacheConfigurations(perCache)
                .build();
    }

    /**
     * Redis outage policy: treat cache failures as a silent miss/store-skip and
     * continue against the database (cache is NOT the source of truth). Hard
     * failures remain visible via cache error logs and can be alerted later.
     */
    public static class LenientCacheErrorHandler implements CacheErrorHandler {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LenientCacheErrorHandler.class);

        @Override public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
            log.warn("Cache GET failed for cache={} key={}: {}", cache.getName(), key, e.toString());
        }
        @Override public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
            log.warn("Cache PUT failed for cache={} key={}: {}", cache.getName(), key, e.toString());
        }
        @Override public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
            log.warn("Cache EVICT failed for cache={} key={}: {}", cache.getName(), key, e.toString());
        }
        @Override public void handleCacheClearError(RuntimeException e, Cache cache) {
            log.warn("Cache CLEAR failed for cache={}: {}", cache.getName(), e.toString());
        }
    }

    @Bean
    public CachingConfigurer streamsphereCachingConfigurer() {
        return new CachingConfigurer() {
            @Override
            public CacheErrorHandler errorHandler() {
                return new LenientCacheErrorHandler();
            }
        };
    }
}

