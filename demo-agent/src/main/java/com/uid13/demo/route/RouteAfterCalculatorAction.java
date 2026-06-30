package com.uid13.demo.route;

import com.uid13.demo.state.StateConstants;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;

/**
 * 计算节点后的条件路由
 *
 * 检查 active_agent 状态，决定路由到下一个 Agent 还是结束。
 *
 * 参考官方 RouteAfterSupportAction 模式
 */
@Slf4j
public class RouteAfterCalculatorAction {

    public static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction create() {
        return edge_async(state -> {
            String activeAgent = (String) state.value(StateConstants.ACTIVE_AGENT).orElse(END);

            if (StateConstants.WEATHER_AGENT.equals(activeAgent)) {
                log.info("[RouteAfterCalculator] 检测到 Handoff → WeatherAgent");
                return StateConstants.WEATHER_AGENT;
            }
            if (StateConstants.CHAT_AGENT.equals(activeAgent)) {
                log.info("[RouteAfterCalculator] 检测到 Handoff → ChatAgent");
                return StateConstants.CHAT_AGENT;
            }

            log.info("[RouteAfterCalculator] 任务完成 → END");
            return END;
        });
    }

    public static Map<String, String> mappings() {
        return Map.of(
                StateConstants.WEATHER_AGENT, StateConstants.WEATHER_AGENT,
                StateConstants.CHAT_AGENT, StateConstants.CHAT_AGENT,
                END, END
        );
    }
}
