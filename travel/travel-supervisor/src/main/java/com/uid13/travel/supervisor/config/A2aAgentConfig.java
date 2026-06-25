package com.uid13.travel.supervisor.config;

import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.uid13.travel.common.constant.AgentConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A2A Agent 配置
 * 配置从 Nacos 发现的远程 TravelAgent
 *
 * @author uid13
 */
@Configuration
public class A2aAgentConfig {

    /**
     * 创建远程 TravelAgent
     * 通过 AgentCardProvider 从 Nacos 自动发现 travel-agent 的 AgentCard
     *
     * @param agentCardProvider Nacos 自动注入的 AgentCardProvider
     * @return A2aRemoteAgent 远程 Agent 实例
     */
    @Bean
    public A2aRemoteAgent travelAgent(AgentCardProvider agentCardProvider) {
        return A2aRemoteAgent.builder()
                .agentCardProvider(agentCardProvider)
                .name(AgentConstants.TRAVEL_AGENT_NAME)
                .description("出行规划专家，负责路线规划、POI 搜索、天气查询等")
                .build();
    }
}
