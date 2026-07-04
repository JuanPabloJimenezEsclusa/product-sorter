package dev.jpje.productsorter.adapter.persistence.cache;

import java.time.Duration;
import java.util.List;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder;
import io.lettuce.core.metrics.MicrometerOptions;
import io.lettuce.core.resource.ClientResources;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

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
    final var manager = new CaffeineCacheManager("productCache");
    manager.setCaffeine(Caffeine.newBuilder()
      .maximumSize(100)
      .expireAfterWrite(Duration.ofSeconds(30))
      .recordStats());
    manager.setAllowNullValues(false);
    return manager;
  }

  @Bean
  @ConditionalOnProperty(name = "cache.redis.enabled", havingValue = "true")
  public RedisConnectionFactory redisConnectionFactory(
      @Value("${spring.data.redis.host:localhost}") final String host,
      @Value("${spring.data.redis.port:6379}") final int port,
      @Value("${spring.data.redis.username:}") final String username,
      @Value("${spring.data.redis.password:}") final String password,
      final MeterRegistry meterRegistry) {
    final var config = new RedisStandaloneConfiguration(host, port);

    if (!username.isBlank()) {
      config.setUsername(username);
    }
    if (!password.isBlank()) {
      config.setPassword(password);
    }

    final var clientResources = ClientResources.builder()
      .commandLatencyRecorder(new MicrometerCommandLatencyRecorder(meterRegistry, MicrometerOptions.create()))
      .build();
    final var clientConfig = LettuceClientConfiguration.builder()
      .clientResources(clientResources)
      .build();
    return new LettuceConnectionFactory(config, clientConfig);
  }

  @Bean
  @ConditionalOnProperty(name = "cache.redis.enabled", havingValue = "true")
  public CacheManager redisCacheManager(final RedisConnectionFactory connectionFactory,
                                         @Value("${cache.redis.ttl:300}") final long ttlSeconds) {
    final var config = RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(Duration.ofSeconds(ttlSeconds))
      .disableCachingNullValues()
      .serializeValuesWith(
        RedisSerializationContext.SerializationPair.fromSerializer(
          RedisSerializer.java()));
    return RedisCacheManager.builder(connectionFactory)
      .withCacheConfiguration("productCache", config)
      .enableStatistics()
      .build();
  }
}
