package org.paismart.pai_test.Config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class ObjectMapperConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                // 遇到未知字段时不报错
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // 如果 JSON 里 primitive 类型字段是 null，不直接失败
                // 例如 boolean isPublic 遇到 null 时不直接反序列化异常
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)

                .build();
    }
}