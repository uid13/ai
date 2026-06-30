package com.uid13.demo.route;

import com.uid13.demo.state.StateConstants;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;

/**
 * 聊天节点后的条件路由
 *
 * ChatAgent 是兜底 Agent，通常不会再移交，直接结束。
 * 但保留 Handoff 能力以支持未来扩展。
 */
@Slf4j
public class RouteAfterChatAction {

    public static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction create() {
        return edge_async(state -> {
            String activeAgent = (String) state.value(StateConstants.ACTIVE_AGENT).orElse(END);

            if (StateConstants.WEATHER_AGENT.equals(activeAgent)) {
                log.info("[RouteAfterChat] 检测到 Handoff → WeatherAgent");
                return StateConstants.WEATHER_AGENT;
            }
            if (StateConstants.CALCULATOR_AGENT.equals(activeAgent)) {
                log.info("[RouteAfterChat] 检测到 Handoff → CalculatorAgent");
                return StateConstants.CALCULATOR_AGENT;
            }

            log.info("[RouteAfterChat] 任务完成 → END");
            return END;
        });
    }

    public static Map<String, String> mappings() {
        return Map.of(
                StateConstants.WEATHER_AGENT, StateConstants.WEATHER_AGENT,
                StateConstants.CALCULATOR_AGENT, StateConstants.CALCULATOR_AGENT,
                END, END
        );
    }
}
