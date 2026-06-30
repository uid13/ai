package com.uid13.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring AI Alibaba 混合架构演示应用
 *
 * 架构：Workflow (StateGraph) + Handoffs (ToolContextHelper)
 *
 * 功能：
 * 1. LLM 意图路由 - Router 节点自动分类用户意图
 * 2. 多智能体协作 - WeatherAgent / CalculatorAgent / ChatAgent 各司其职
 * 3. Handoffs 移交 - Agent 间通过 ToolContextHelper 动态移交控制权
 *
 * 启动前请配置环境变量：
 * export DEEPSEEK_API_KEY=your-api-key
 *
 * @author uid13
 */
@SpringBootApplication
public class DemoAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoAgentApplication.class, args);
    }
}
