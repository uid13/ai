package com.uid13.travel.agent;

import com.alibaba.cloud.ai.graph.agent.AgentTool;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.uid13.travel.constant.AgentConstants;
import com.uid13.travel.service.NacosPromptService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * 出行助手 Supervisor Agent（主管）
 * 负责理解用户意图，分发任务给子 Agent（如 TravelAgent）
 *
 * 多 Agent 编排架构：
 * - Supervisor：接收用户请求，分析意图，决定是否调用子 Agent
 * - TravelAgent：执行具体的出行规划任务（路线、POI、天气等）
 * - Supervisor 通过 AgentTool 将 TravelAgent 注册为工具，实现动态调用
 *
 * @author uid13
 */
@Component
public class SupervisorAgent {

    private final ReactAgent agent;

    public SupervisorAgent(ChatModel chatModel, TravelAgent travelAgent,
                           NacosPromptService promptService, RedisSaver redisSaver) {
        // 从 Nacos Prompt 管理获取提示词
        String systemPrompt = promptService.getPrompt(AgentConstants.SUPERVISOR_AGENT_PROMPT);

        // 将 TravelAgent 注册为子 Agent 工具，实现多 Agent 编排
        // Supervisor 可以根据用户意图决定是否调用 TravelAgent
        this.agent = ReactAgent.builder()
                .name(AgentConstants.SUPERVISOR_AGENT_NAME)
                .model(chatModel)
                .systemPrompt(systemPrompt)
                .saver(redisSaver)
                .tools(AgentTool.getFunctionToolCallback(travelAgent.getAgent()))
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
