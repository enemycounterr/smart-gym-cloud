package com.sprint.smartgymcore.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
//        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
//                .allowIfBaseType(Object.class)
//                .build();
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType("com.sprint.smartgymcore")
                .allowIfSubType("com.sprint.smartgymcore")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.lang.")
                .build();

        JsonMapper jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL_AND_RECORDS, JsonTypeInfo.As.PROPERTY)
                .build();

        GenericJacksonJsonRedisSerializer jsonSerializer = new GenericJacksonJsonRedisSerializer(jsonMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
//                .computePrefixWith(cacheName -> cacheName + "::") REMOVED IT BECAUSE IT ALREADY PRE-DEFINED
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                "zones", defaultConfig.entryTtl(Duration.ofHours(24)),
                "clientStats", defaultConfig.entryTtl(Duration.ofMinutes(15))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}