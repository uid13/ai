package com.uid13.travel.agent.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.uid13.travel.common.constant.AgentConstants;
import com.uid13.travel.common.service.NacosPromptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 出行规划 Agent 配置
 * 负责路线规划、POI 搜索、天气查询等出行相关任务
 * 通过高德地图 MCP 工具提供能力
 *
 * @author uid13
 */
@Slf4j
@Configuration
public class TravelAgentConfig {

    /**
     * 创建并注册 ReactAgent Bean
     * A2A 自动注册要求 Bean 类型为 BaseAgent（ReactAgent 实现了该接口）
     *
     * @param chatModel     聊天模型
     * @param amapTools     高德地图 MCP 工具提供者
     * @param promptService Nacos 提示词服务
     * @return ReactAgent 实例
     */
    @Bean("travel-agent")
    public ReactAgent travelAgent(ChatModel chatModel,
                                  ToolCallbackProvider amapTools,
                                  NacosPromptService promptService) {
        // 从 Nacos 获取提示词
        String systemPrompt = promptService.getPrompt(AgentConstants.TRAVEL_AGENT_PROMPT);
        log.info("初始化 TravelAgent，系统提示词: {}", systemPrompt);

        // 构建 ReactAgent
        ReactAgent agent = ReactAgent.builder()
                .name(AgentConstants.TRAVEL_AGENT_NAME)
                .model(chatModel)
                .systemPrompt(systemPrompt)
                .tools(amapTools.getToolCallbacks())
                .build();

        log.info("TravelAgent 初始化完成");
        return agent;
    }
}
