package com.uid13.travel.controller;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cola.dto.SingleResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import com.uid13.travel.agent.SupervisorAgent;
import com.uid13.travel.constant.AgentConstants;
import com.uid13.travel.dto.ChatDTO;
import com.uid13.travel.dto.HealthDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * 出行助手 REST 控制器
 * 提供聊天接口，用户通过此接口与 Supervisor Agent 交互
 * Supervisor Agent 会根据用户意图决定是否调用 TravelAgent 执行具体任务
 *
 * @author uid13
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TravelController {

    private final SupervisorAgent supervisorAgent;

    /**
     * 聊天接口（支持多轮对话）
     * 用户发送消息，Supervisor Agent 处理并返回结果
     * 请求体可选传入 threadId 以关联历史会话；未传时服务端自动生成
     *
     * @param request 请求体，包含 message 和可选的 threadId 字段
     * @return COLA 标准化响应，包含聊天数据和会话 ID
     */
    @PostMapping("/chat")
    public SingleResponse<ChatDTO> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.isBlank()) {
            return SingleResponse.buildFailure("PARAM_ERROR", "message 不能为空");
        }

        // 提取或生成会话 ID
        String threadId = request.get("threadId");
        if (threadId == null || threadId.isBlank()) {
            threadId = UUID.randomUUID().toString();
        }

        try {
            ReactAgent agent = supervisorAgent.getAgent();
            UserMessage userMsg = new UserMessage(message);

            // 构建带 threadId 的运行配置，实现多轮对话上下文保持
            RunnableConfig config = RunnableConfig.builder()
                    .threadId(threadId)
                    .build();

            AssistantMessage response = agent.call(userMsg, config);

            ChatDTO chatDTO = new ChatDTO();
            chatDTO.setMessage(response != null ? response.getText() : "无响应");
            chatDTO.setAgent(AgentConstants.SUPERVISOR_AGENT_NAME);
            chatDTO.setThreadId(threadId);

            return SingleResponse.of(chatDTO);
        } catch (GraphRunnerException e) {
            return SingleResponse.buildFailure("AGENT_ERROR", "Agent 执行失败：" + e.getMessage());
        }
    }

    /**
     * 健康检查接口
     *
     * @return COLA 标准化响应，包含健康检查数据
     */
    @GetMapping("/health")
    public SingleResponse<HealthDTO> health() {
        HealthDTO healthDTO = new HealthDTO();
        healthDTO.setStatus("UP");
        healthDTO.setService("travel-assistant");
        healthDTO.setVersion("1.0.0");

        return SingleResponse.of(healthDTO);
    }
}
