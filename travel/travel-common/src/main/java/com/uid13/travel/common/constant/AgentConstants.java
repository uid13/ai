package com.uid13.travel.common.constant;

import lombok.experimental.UtilityClass;

/**
 * Agent 常量定义
 * 集中管理多 Agent 编排中的 Agent 名称和 Nacos Prompt Key
 * 包括 Supervisor（主管）和 TravelAgent（出行规划专家）
 *
 * @author uid13
 */
public final class AgentConstants {

    private AgentConstants() {
    }

    // ==================== Agent 名称 ====================

    /**
     * 出行规划 Agent 名称（子 Agent）
     * 负责执行具体的出行规划任务（路线、POI、天气等）
     */
    public static final String TRAVEL_AGENT_NAME = "travel-agent";

    /**
     * 主管 Agent 名称
     * 负责理解用户意图，协调子 Agent 完成任务
     */
    public static final String SUPERVISOR_AGENT_NAME = "travel-supervisor";

    // ==================== Prompt Key ====================

    /**
     * 出行规划 Agent 提示词 Key
     */
    public static final String TRAVEL_AGENT_PROMPT = "travel-agent-prompt";

    /**
     * 主管 Agent 提示词 Key
     */
    public static final String SUPERVISOR_AGENT_PROMPT = "supervisor-agent-prompt";

    // ==================== Agent State Key ====================

    /** Agent 执行结果输出 key（A2aRemoteAgent 默认 outputKey） */
    public static final String AGENT_OUTPUT_KEY = "output";

    // ==================== State Key ====================

    /** 对话历史 key（框架内置，用于积累 UserMessage / AssistantMessage 列表） */
    public static final String MESSAGES_KEY = "messages";


}