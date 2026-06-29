package com.uid13.travel.supervisor.controller;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cola.dto.SingleResponse;

import com.uid13.travel.common.constant.AgentConstants;
import com.uid13.travel.common.dto.ChatDTO;
import com.uid13.travel.common.dto.HealthDTO;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private final LlmRoutingAgent supervisorAgent;

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
            UserMessage userMsg = new UserMessage(message);

            // 构建带 threadId 的运行配置，实现多轮对话上下文保持
            RunnableConfig config = RunnableConfig.builder()
                    .threadId(threadId)
                    .build();

            // 调用 SupervisorAgent 处理请求
            Optional<OverAllState> result = supervisorAgent.invoke(userMsg, config);

            // 从状态中提取 Agent 输出（A2aRemoteAgent 默认 outputKey = "output"）
            String responseText = result
                    .flatMap(state -> state.value(AgentConstants.AGENT_OUTPUT_KEY))
                    .map(Object::toString)
                    .orElse("无响应");

            ChatDTO chatDTO = new ChatDTO();
            chatDTO.setMessage(responseText);
            chatDTO.setAgent(AgentConstants.SUPERVISOR_AGENT_NAME);
            chatDTO.setRole(MessageType.ASSISTANT.getValue());
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
        healthDTO.setService("travel-supervisor");
        healthDTO.setVersion("1.0.0");

        return SingleResponse.of(healthDTO);
    }

    /**
     * 获取会话历史消息
     * 从 Redis checkpoint 中读取当前 threadId 的完整消息列表
     *
     * @param threadId 会话 ID（必填）
     * @return COLA 标准化响应，包含消息列表
     */
    @GetMapping("/chat/history")
    public SingleResponse<List<ChatDTO>> chatHistory(@RequestParam String threadId) {
        if (threadId == null || threadId.isBlank()) {
            return SingleResponse.buildFailure("PARAM_ERROR", "threadId 不能为空");
        }

        try {
            RunnableConfig config = RunnableConfig.builder()
                    .threadId(threadId)
                    .build();

            // 从 Redis 加载 state，不重新执行 Agent
            StateSnapshot snapshot = supervisorAgent.getCurrentState(config);
            OverAllState state = snapshot.state();
            Optional<Object> messagesOpt = state.value(AgentConstants.MESSAGES_KEY);

            if (messagesOpt.isEmpty() || !(messagesOpt.get() instanceof List)) {
                return SingleResponse.of(List.of());
            }

            List<?> messages = (List<?>) messagesOpt.get();
            List<ChatDTO> history = new ArrayList<>();

            for (Object msg : messages) {
                if (msg instanceof UserMessage userMsg) {
                    ChatDTO dto = new ChatDTO();
                    dto.setMessage(userMsg.getText());
                    dto.setRole(MessageType.USER.getValue());
                    dto.setThreadId(threadId);
                    history.add(dto);
                } else if (msg instanceof AssistantMessage assistantMsg) {
                    ChatDTO dto = new ChatDTO();
                    dto.setMessage(assistantMsg.getText());
                    dto.setAgent(AgentConstants.SUPERVISOR_AGENT_NAME);
                    dto.setRole(MessageType.ASSISTANT.getValue());
                    dto.setThreadId(threadId);
                    history.add(dto);
                }
            }

            return SingleResponse.of(history);
        } catch (Exception e) {
            return SingleResponse.buildFailure("STATE_ERROR", "获取历史消息失败：" + e.getMessage());
        }
    }
}