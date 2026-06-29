package com.uid13.travel.agent.config;

import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;

/**
 * Redis 配置类
 * 提供 RedissonClient 和 RedisSaver Bean，用于 Agent 多轮对话状态持久化
 * RedisSaver 为 TravelAgent 的 ReactAgent 提供 checkpoint 持久化，支持通过 A2A 协议的多轮对话上下文保持
 *
 * @author uid13
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    private RedissonClient redissonClient;

    /**
     * 创建 Redisson 客户端
     * 使用单机模式连接 Redis
     *
     * @return RedissonClient 实例
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort);
        redissonClient = Redisson.create(config);
        return redissonClient;
    }

    /**
     * 创建 RedisSaver
     * 用于 ReactAgent 的 checkpoint 持久化，支持多轮对话
     *
     * @param redissonClient Redisson 客户端
     * @return RedisSaver 实例
     */
    @Bean
    public RedisSaver redisSaver(RedissonClient redissonClient) {
        return RedisSaver.builder().redisson(redissonClient).build();
    }

    /**
     * 应用关闭前释放 Redisson 资源
     */
    @PreDestroy
    public void shutdown() {
        if (redissonClient != null && !redissonClient.isShutdown()) {
            redissonClient.shutdown();
        }
    }
}
