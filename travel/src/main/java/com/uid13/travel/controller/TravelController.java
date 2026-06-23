package com.uid13.travel.controller;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import com.uid13.travel.agent.SupervisorAgent;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 出行助手 REST 控制器
 * 提供聊天接口，用户通过此接口与 Supervisor Agent 交互
 *
 * @author uid13
 */
@RestController
@RequestMapping("/api")
public class TravelController {

    private final SupervisorAgent supervisorAgent;

    public TravelController(SupervisorAgent supervisorAgent) {
        this.supervisorAgent = supervisorAgent;
    }

    /**
     * 聊天接口
     * 用户发送消息，Supervisor Agent 处理并返回结果
     *
     * @param request 请求体，包含 message 字段
     * @return Agent 响应
     */
    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.isBlank()) {
            return Map.of("error", "message 不能为空");
        }

        try {
            ReactAgent agent = supervisorAgent.getAgent();
            UserMessage userMsg = new UserMessage(message);

            AssistantMessage response = agent.call(userMsg);

            return Map.of(
                    "message", response != null ? response.getText() : "无响应",
                    "agent", "travel-supervisor"
            );
        } catch (GraphRunnerException e) {
            return Map.of("error", "Agent 执行失败：" + e.getMessage());
        }
    }

    /**
     * 健康检查接口
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "service", "travel-assistant",
                "version", "1.0.0"
        );
    }
}
