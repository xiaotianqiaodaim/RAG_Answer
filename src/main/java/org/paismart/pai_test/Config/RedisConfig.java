package org.paismart.pai_test.Config;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/*
* 配置RedisTemplate类，完成依赖的注入
* */
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // 当前如果主要用 bitmap，可以先用 StringRedisSerializer
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();

        return template;
    }
}
