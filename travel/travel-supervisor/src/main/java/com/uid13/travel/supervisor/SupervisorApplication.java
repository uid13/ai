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
@SpringBootApplication
@ComponentScan(basePackages = {"com.uid13.travel.supervisor", "com.uid13.travel.common"})
public class SupervisorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupervisorApplication.class, args);
    }
}
