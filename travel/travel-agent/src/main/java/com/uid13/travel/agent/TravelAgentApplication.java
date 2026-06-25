package com.uid13.travel.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Travel Agent 应用启动类
 * 作为 A2A Server，提供出行规划能力
 *
 * @author uid13
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.uid13.travel.agent", "com.uid13.travel.common"})
public class TravelAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelAgentApplication.class, args);
    }
}
