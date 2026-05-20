package com.collab.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

@Configuration
public class RedisConfig {

    /**
     * 配置 RedisTemplate，设置序列化器
     * @param factory Redis 连接工厂
     * @return 配置好的 RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory factory){

        // 创建 RedisTemplate 实例
        RedisTemplate<String,Object> redisTemplate = new RedisTemplate<>();

        // 设置 Redis 连接工厂
        redisTemplate.setConnectionFactory(factory);

        // key 序列化器：使用 String 序列化，保证 key 可读性
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // value 序列化器：使用 JSON 序列化，支持复杂对象存储
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        // 设置普通 key 的序列化方式
        redisTemplate.setKeySerializer(stringRedisSerializer);

        // 设置 Hash 结构中 field 的序列化方式
        redisTemplate.setHashKeySerializer(stringRedisSerializer);

        // 设置普通 value 的序列化方式
        redisTemplate.setValueSerializer(jsonSerializer);

        // 设置 Hash 结构中 value 的序列化方式
        redisTemplate.setHashValueSerializer(jsonSerializer);

        // 初始化模板，确保配置生效
        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }

}