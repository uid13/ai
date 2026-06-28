package com.uid13.travel.supervisor.config;

import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.uid13.travel.common.constant.AgentConstants;
import com.uid13.travel.common.service.NacosPromptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 出行助手 Supervisor Agent（主管）
 * 负责理解用户意图，分发任务给子 Agent（如 TravelAgent）
 *
 * 多 Agent 编排架构（A2A 分布式）：
 * - Supervisor：接收用户请求，分析意图，决定是否调用子 Agent
 * - TravelAgent：执行具体的出行规划任务（路线、POI、天气等）
 * - Supervisor 通过 A2A 协议远程调用 TravelAgent
 *
 * @author uid13
 */
@Slf4j
@Configuration
public class SupervisorAgentConfig {

    /**
     * 创建远程 TravelAgent
     * 通过 AgentCardProvider 从 Nacos 自动发现 travel-agent 的 AgentCard
     */
    @Bean
    public A2aRemoteAgent travelAgent(@Qualifier("nacosAgentCardProvider") AgentCardProvider agentCardProvider) {
        return A2aRemoteAgent.builder()
                .agentCardProvider(agentCardProvider)
                .name(AgentConstants.TRAVEL_AGENT_NAME)
                .description("出行规划专家，负责路线规划、POI 搜索、天气查询等")
                .build();
    }

    /**
     * 创建 Supervisor Agent
     */
    @Bean
    public LlmRoutingAgent supervisorAgent(ChatModel chatModel,
                                           A2aRemoteAgent travelAgent,
                                           NacosPromptService promptService,
                                           RedisSaver redisSaver) {
        // 从 Nacos Prompt 管理获取提示词
        String systemPrompt = promptService.getPrompt(AgentConstants.SUPERVISOR_AGENT_PROMPT);

        // 构建 LlmRoutingAgent 作为 Supervisor
        // 将远程 TravelAgent 注册为子 Agent
        return LlmRoutingAgent.builder()
                .name(AgentConstants.SUPERVISOR_AGENT_NAME)
                .model(chatModel)
                .systemPrompt(systemPrompt)
                .subAgents(List.of(travelAgent))
                .saver(redisSaver)
                .build();
    }
}
