package com.uid13.travel.agent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.uid13.travel.common.constant.AgentConstants;
import com.uid13.travel.common.service.NacosPromptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

/**
 * 出行规划 Agent（子 Agent）
 * 负责路线规划、POI 搜索、天气查询等出行相关任务
 * 通过高德地图 MCP 工具提供能力
 *
 * @author uid13
 */
@Slf4j
@Component
public class TravelAgent {

    private final ReactAgent agent;

    /**
     * 构造函数
     *
     * @param chatModel     聊天模型
     * @param amapTools     高德地图 MCP 工具提供者
     * @param promptService Nacos 提示词服务
     */
    public TravelAgent(ChatModel chatModel,
                       ToolCallbackProvider amapTools,
                       NacosPromptService promptService) {
        // 从 Nacos 获取提示词
        String systemPrompt = promptService.getPrompt(AgentConstants.TRAVEL_AGENT_PROMPT);
        log.info("初始化 TravelAgent，系统提示词: {}", systemPrompt);

        // 构建 ReactAgent
        this.agent = ReactAgent.builder()
                .name(AgentConstants.TRAVEL_AGENT_NAME)
                .model(chatModel)
                .systemPrompt(systemPrompt)
                .tools(amapTools.getToolCallbacks())
                .build();

        log.info("TravelAgent 初始化完成");
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
