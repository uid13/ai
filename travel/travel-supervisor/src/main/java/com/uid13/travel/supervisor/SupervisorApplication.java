package com.uid13.travel.supervisor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Supervisor 应用启动类
 * 作为 A2A Client，通过 Nacos 发现并调用远程 TravelAgent
 *
 * @author uid13
 */
@SpringBootApplication(exclude = {
        // 排除 A2A Server 端自动配置（Supervisor 是 Client，不需要 Server 功能）
        com.alibaba.cloud.ai.a2a.autoconfigure.server.A2aServerAutoConfiguration.class,
        com.alibaba.cloud.ai.a2a.autoconfigure.server.A2aServerAgentCardAutoConfiguration.class,
        // 排除 Nacos 注册中心自动配置（Supervisor 只需要 Discovery，不需要 Registry）
        com.alibaba.cloud.ai.a2a.autoconfigure.nacos.NacosA2aRegistryAutoConfiguration.class
})
@ComponentScan(basePackages = {"com.uid13.travel.supervisor", "com.uid13.travel.common"})
public class SupervisorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupervisorApplication.class, args);
    }
}
