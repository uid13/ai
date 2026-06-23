package com.uid13.travel.agent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * 出行助手 Supervisor Agent（主管）
 * 负责理解用户意图，分发任务给子 Agent
 *
 * @author uid13
 */
@Component
public class SupervisorAgent {

    private final ReactAgent agent;

    public SupervisorAgent(ChatModel chatModel, TravelAgent travelAgent) {
        this.agent = ReactAgent.builder()
                .name("travel-supervisor")
                .model(chatModel)
                .systemPrompt("""
                        你是出行助手的主管，负责理解用户的出行需求并协调子 Agent 完成任务。
                        
                        你的职责：
                        1. 理解用户意图（路线规划、地点搜索、天气查询等）
                        2. 将任务分发给合适的子 Agent
                        3. 整合子 Agent 的返回结果，给出最终回答
                        
                        子 Agent：
                        - travel-agent: 出行规划专家，负责路线规划、POI 搜索、天气查询等
                        
                        工作流程：
                        1. 分析用户需求
                        2. 调用 travel-agent 执行具体任务
                        3. 整合结果并返回给用户
                        
                        注意：
                        - 如果用户需求不明确，请主动询问澄清
                        - 返回结果要简洁、实用
                        - 对于复杂需求，可以分解为多个子任务
                        """)
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
