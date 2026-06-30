package com.uid13.demo.route;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.uid13.demo.state.StateConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 初始路由节点 - 使用 LLM 判断用户意图，决定进入哪个 Agent
 *
 * 参考官方 RouteInitialAction 模式：
 * 通过 LLM 对用户输入进行意图分类，将分类结果写入 active_agent 状态键，
 * 供后续条件边进行路由决策。
 */
@Slf4j
@RequiredArgsConstructor
public class RouteInitialAction {

    private final ChatModel chatModel;

    /**
     * 创建异步节点动作
     */
    public AsyncNodeAction create() {
        return node_async(this::route);
    }

    /**
     * 路由逻辑：调用 LLM 进行意图分类
     *
     * 分类结果：
     * - weather_agent：天气、日期、时间相关问题
     * - calculator_agent：数学计算、汇率换算相关问题
     * - chat_agent：闲聊、问候、其他通用问题
     */
    private Map<String, Object> route(OverAllState state) {
        String input = (String) state.value(StateConstants.INPUT).orElse("");

        // 使用 LLM 进行意图分类（轻量级 prompt）
        String prompt = """
                分析以下用户问题的意图，只回复一个类别标签：weather、calculator 或 chat。
                - weather：天气查询、日期时间
                - calculator：数学计算、汇率换算
                - chat：闲聊、问候、其他

                用户问题：%s
                类别标签：
                """.formatted(input);

        String rawIntent = chatModel.call(prompt).trim().toLowerCase();
        String intent = rawIntent.lines().findFirst().orElse("chat").trim();

        log.debug("[RouteInitial] LLM 原始返回: {}", rawIntent);

        // 映射到 Agent 节点名称
        String activeAgent = switch (intent) {
            case "weather" -> StateConstants.WEATHER_AGENT;
            case "calculator" -> StateConstants.CALCULATOR_AGENT;
            default -> StateConstants.CHAT_AGENT;
        };

        log.info("[RouteInitial] 输入=\"{}\", 意图={}, 路由到={}", input, intent, activeAgent);

        return Map.of(
                StateConstants.ACTIVE_AGENT, activeAgent,
                StateConstants.ROUTE_TRACE, "START→" + activeAgent
        );
    }
}
