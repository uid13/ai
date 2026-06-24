package com.uid13.travel.agent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.uid13.travel.constant.AgentConstants;
import com.uid13.travel.service.NacosPromptService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

/**
 * 出行规划 Agent（子 Agent）
 * 负责路线规划、POI 搜索、天气查询等出行相关任务
 * 调用高德地图 MCP 工具
 * 注：子 Agent 状态通过 Supervisor 的 graph checkpoint 间接保持，无需独立配置 RedisSaver
 *
 * @author uid13
 */
@Component
public class TravelAgent {

    private final ReactAgent agent;

    public TravelAgent(ChatModel chatModel, ToolCallbackProvider amapTools,
                       NacosPromptService promptService) {
        // 从 Nacos Prompt 管理获取提示词
        // amapTools 由 CustomMcpTransportConfig 创建，聚合高德 MCP 工具
        String systemPrompt = promptService.getPrompt(AgentConstants.TRAVEL_AGENT_PROMPT);

        this.agent = ReactAgent.builder()
                .name(AgentConstants.TRAVEL_AGENT_NAME)
                .model(chatModel)
                .tools(amapTools.getToolCallbacks())
                .systemPrompt(systemPrompt)
                .build();
    }

    /**
     * 获取 Agent 实例
     *
     * @return ReactAgent
     */
    public ReactAgent getAgent() {
        return agent;
    }
}
