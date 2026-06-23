package com.uid13.travel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 出行助手应用入口
 * 基于 Spring AI Alibaba AgentScope 的 Multi-Agent 架构
 *
 * @author uid13
 */
@SpringBootApplication
public class TravelApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelApplication.class, args);
    }
}
