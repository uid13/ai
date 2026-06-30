package com.uid13.demo.route;

import com.uid13.demo.state.StateConstants;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;

/**
 * 天气节点后的条件路由
 *
 * 检查 active_agent 状态：
 * - 如果被 Handoff 工具修改为其他 Agent → 路由到对应节点
 * - 如果仍为 weather_agent 或为空 → 结束（任务已完成）
 *
 * 参考官方 RouteAfterSalesAction 模式
 */
@Slf4j
public class RouteAfterWeatherAction {

    /**
     * 创建条件边动作
     */
    public static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction create() {
        return edge_async(state -> {
            String activeAgent = (String) state.value(StateConstants.ACTIVE_AGENT).orElse(StateConstants.END);

            // 如果 active_agent 被 handoff 工具修改为目标 Agent，则路由过去
            if (StateConstants.CALCULATOR_AGENT.equals(activeAgent)) {
                log.info("[RouteAfterWeather] 检测到 Handoff → CalculatorAgent");
                return StateConstants.CALCULATOR_AGENT;
            }
            if (StateConstants.CHAT_AGENT.equals(activeAgent)) {
                log.info("[RouteAfterWeather] 检测到 Handoff → ChatAgent");
                return StateConstants.CHAT_AGENT;
            }

            // 否则结束
            log.info("[RouteAfterWeather] 任务完成 → END");
            return StateConstants.END;
        });
    }

    /**
     * 条件边的映射关系
     */
    public static Map<String, String> mappings() {
        return Map.of(
                StateConstants.CALCULATOR_AGENT, StateConstants.CALCULATOR_AGENT,
                StateConstants.CHAT_AGENT, StateConstants.CHAT_AGENT,
                END, END
        );
    }
}
