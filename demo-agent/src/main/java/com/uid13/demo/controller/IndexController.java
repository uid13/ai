package com.uid13.demo.controller;

import com.alibaba.cola.dto.SingleResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 首页控制器 - 提供 API 文档和使用说明
 */
@RestController
public class IndexController {

    @GetMapping("/")
    public SingleResponse<Map<String, Object>> index() {
        Map<String, Object> data = Map.of(
                "name", "Spring AI Alibaba Hybrid Demo",
                "version", "3.0.0 (Workflow + Handoffs)",
                "description", "混合架构：StateGraph 工作流编排 + Handoffs 智能体移交",
                "architecture", Map.of(
                        "Workflow 层", "StateGraph 定义节点和条件边（确定性骨架）",
                        "Handoffs 层", "@Tool(returnDirect=true) + ToolContextHelper 修改 active_agent（灵活性）",
                        "Router", "LLM 意图分类，决定初始路由",
                        "WeatherAgent", "天气/时间专家，可 Handoff 到 Calculator/Chat",
                        "CalculatorAgent", "计算/汇率专家，可 Handoff 到 Weather/Chat",
                        "ChatAgent", "通用聊天兜底，可 Handoff 到 Weather/Calculator"
                ),
                "endpoints", Map.of(
                        "执行工作流", "GET /api/workflow/run?message=北京天气怎么样"
                ),
                "test_cases", List.of(
                        "北京今天天气怎么样 → Router→WeatherAgent→END",
                        "100 美元换算成人民币 → Router→CalculatorAgent→END",
                        "你好 → Router→ChatAgent→END",
                        "今天天气怎么样，另外 125 乘 38 等于多少 → Router→WeatherAgent→Handoff→CalculatorAgent→END"
                )
        );
        return SingleResponse.of(data);
    }
}
