package com.uid13.demo.state;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 状态键常量 - 定义 StateGraph 中使用的状态键和 Agent 名称
 *
 * 参考官方示例：AgentScopeStateConstants
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StateConstants {

    // ==================== 状态键 ====================

    /** 当前活跃 Agent 标识，用于 Handoffs 路由 */
    public static final String ACTIVE_AGENT = "active_agent";

    /** 用户输入消息 */
    public static final String INPUT = OverAllState.DEFAULT_INPUT_KEY;

    /** 最终响应结果 */
    public static final String RESULT = "result";

    /** 路由轨迹（记录经过的节点，用于调试） */
    public static final String ROUTE_TRACE = "route_trace";

    // ==================== Agent 节点名称 ====================

    /** 天气专家 Agent */
    public static final String WEATHER_AGENT = "weather_agent";

    /** 计算专家 Agent */
    public static final String CALCULATOR_AGENT = "calculator_agent";

    /** 通用聊天 Agent */
    public static final String CHAT_AGENT = "chat_agent";

    /** 结束标识（与 StateGraph.END 对应） */
    public static final String END = StateGraph.END;
}
