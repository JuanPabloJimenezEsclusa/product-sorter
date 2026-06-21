package com.acidtango.productsorter.infrastructure.cache;

import java.time.Duration;
import java.util.List;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  @Primary
  public CacheManager multiTierCacheManager(final List<CacheManager> cacheManagers) {
    return new CompositeCacheManager(cacheManagers);
  }

  @Bean
  public CacheManager caffeineCacheManager() {
    final var manager = new CaffeineCacheManager("productScores", "productCache");
    manager.setCaffeine(Caffeine.newBuilder()
      .maximumSize(100)
      .expireAfterWrite(Duration.ofSeconds(30)));
    manager.setAllowNullValues(false);
    return manager;
  }

  @Bean
  public CacheManager redisCacheManager(final RedisConnectionFactory connectionFactory) {
    final var config = RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(Duration.ofMinutes(5))
      .disableCachingNullValues();
    return RedisCacheManager.builder(connectionFactory)
      .withCacheConfiguration("productScores", config)
      .withCacheConfiguration("productCache", config)
      .build();
  }
}
