package com.uid13.demo.config;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.uid13.demo.route.RouteAfterCalculatorAction;
import com.uid13.demo.route.RouteAfterChatAction;
import com.uid13.demo.route.RouteAfterWeatherAction;
import com.uid13.demo.route.RouteInitialAction;
import com.uid13.demo.state.StateConstants;
import com.uid13.demo.tools.CalculatorTool;
import com.uid13.demo.tools.TransferToCalculatorTool;
import com.uid13.demo.tools.TransferToChatTool;
import com.uid13.demo.tools.TransferToWeatherTool;
import com.uid13.demo.tools.WeatherTool;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

/**
 * 混合架构配置：Workflow (StateGraph) + Handoffs (ToolContextHelper)
 *
 * 架构说明（参考官方 handoffs 示例）：
 *
 * <pre>
 *                          ┌─────────────────────────────────────┐
 *                          │           StateGraph (Workflow)      │
 *                          │                                     │
 *  START ──→ [Router] ──→ [WeatherAgent] ──(条件边)──→ END       │
 *              │              ↑  ↓ handoff                       │
 *              │           [CalculatorAgent] ──(条件边)──→ END    │
 *              │              ↑  ↓ handoff                       │
 *              │           [ChatAgent] ──────(条件边)──→ END      │
 *                          │                                     │
 *                          └─────────────────────────────────────┘
 *
 * Workflow 层（确定性）：StateGraph 定义节点和条件边的拓扑结构
 * Handoffs 层（灵活性）：Agent 内部的 @Tool(returnDirect=true) 修改 active_agent 状态，
 *                       触发条件边路由到下一个 Agent
 * </pre>
 *
 * 关键设计决策（来自官方文档）：
 * 1. returnDirect = true：Handoff 工具执行后立即退出当前 Agent，防止 LLM 继续生成
 * 2. ToolContextHelper.getStateForUpdate()：修改图状态，节点完成后合并到 OverAllState
 * 3. KeyStrategyFactory：声明 active_agent 使用 ReplaceStrategy，messages 使用 AppendStrategy
 */
@Configuration
public class HybridGraphConfig {

    // ==================== 专家 Agent 定义 ====================

    /**
     * 天气专家 Agent
     * 拥有：WeatherTool（领域工具）+ TransferToCalculatorTool/TransferToChatTool（移交工具）
     */
    @Bean
    public ReactAgent weatherAgent(ChatModel chatModel,
                                   WeatherTool weatherTool,
                                   TransferToCalculatorTool transferToCalculator,
                                   TransferToChatTool transferToChat) {
        return ReactAgent.builder()
                .name(StateConstants.WEATHER_AGENT)
                .model(chatModel)
                .instruction("""
                        你是天气专家助手。你的职责是回答天气查询、日期和时间相关问题。
                        你可以使用 getWeather 和 getCurrentDateTime 工具获取信息。
                        如果用户的问题涉及数学计算或汇率换算，请使用 transfer_to_calculator 工具移交计算专家。
                        如果用户的问题是闲聊或与天气/计算无关的通用问题，请使用 transfer_to_chat 工具移交聊天助手。
                        回答要简洁准确。
                        """)
                .tools(ToolCallbacks.from(weatherTool, transferToCalculator, transferToChat))
                .build();
    }

    /**
     * 计算专家 Agent
     * 拥有：CalculatorTool（领域工具）+ TransferToWeatherTool/TransferToChatTool（移交工具）
     */
    @Bean
    public ReactAgent calculatorAgent(ChatModel chatModel,
                                      CalculatorTool calculatorTool,
                                      TransferToWeatherTool transferToWeather,
                                      TransferToChatTool transferToChat) {
        return ReactAgent.builder()
                .name(StateConstants.CALCULATOR_AGENT)
                .model(chatModel)
                .instruction("""
                        你是计算专家助手。你的职责是处理数学计算和汇率换算问题。
                        你可以使用 calculate 和 currencyExchange 工具进行精确计算。
                        如果用户的问题涉及天气查询或日期时间，请使用 transfer_to_weather 工具移交天气专家。
                        如果用户的问题是闲聊或与计算/天气无关的通用问题，请使用 transfer_to_chat 工具移交聊天助手。
                        回答要简洁准确，给出计算过程。
                        """)
                .tools(ToolCallbacks.from(calculatorTool, transferToWeather, transferToChat))
                .build();
    }

    /**
     * 通用聊天 Agent（兜底）
     * 拥有：TransferToWeatherTool/TransferToCalculatorTool（移交工具）
     * 没有领域工具，纯对话能力
     */
    @Bean
    public ReactAgent chatAgent(ChatModel chatModel,
                                TransferToWeatherTool transferToWeather,
                                TransferToCalculatorTool transferToCalculator) {
        return ReactAgent.builder()
                .name(StateConstants.CHAT_AGENT)
                .model(chatModel)
                .instruction("""
                        你是通用聊天助手。你可以回答各种通用问题、闲聊和问候。
                        如果用户的问题涉及天气查询或日期时间，请使用 transfer_to_weather 工具移交天气专家。
                        如果用户的问题涉及数学计算或汇率换算，请使用 transfer_to_calculator 工具移交计算专家。
                        对于你能回答的通用问题，直接友好地回答。
                        """)
                .tools(ToolCallbacks.from(transferToWeather, transferToCalculator))
                .build();
    }

    // ==================== StateGraph 工作流编排 ====================

    /**
     * 构建混合架构工作流图
     *
     * 核心流程：
     * 1. START → Router（LLM 意图分类，写入 active_agent）
     * 2. Router → 条件边 → 对应 Agent 节点
     * 3. Agent 节点内部：LLM 推理 + Tool 调用（含 Handoff 工具修改 active_agent）
     * 4. Agent 节点完成 → 条件边检查 active_agent → 路由到下一个 Agent 或 END
     */
    @Bean
    public CompiledGraph hybridGraph(ChatModel chatModel,
                                     ReactAgent weatherAgent,
                                     ReactAgent calculatorAgent,
                                     ReactAgent chatAgent) throws GraphStateException {

        // 1. 定义 KeyStrategyFactory（参考官方 handoffs 示例）
        // active_agent 使用 ReplaceStrategy（Handoff 工具覆盖值）
        // messages 使用 AppendStrategy（累积对话历史）
        // route_trace 使用 ReplaceStrategy（记录路由轨迹）
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put(StateConstants.ACTIVE_AGENT, new ReplaceStrategy());
            strategies.put(StateConstants.INPUT, new ReplaceStrategy());
            strategies.put(StateConstants.RESULT, new ReplaceStrategy());
            strategies.put(StateConstants.ROUTE_TRACE, new ReplaceStrategy());
            strategies.put("messages", new AppendStrategy(false));
            return strategies;
        };

        // 2. 创建 StateGraph
        StateGraph graph = new StateGraph("demo-agent", keyStrategyFactory);

        // 3. 添加节点
        // Router 节点：LLM 意图分类
        graph.addNode("router", new RouteInitialAction(chatModel).create());

        // 专家 Agent 节点：使用 getAndCompileGraph() 将 ReactAgent 作为子图嵌入
        // 这是官方推荐的方式（参考 RoutingGraphConfig）
        graph.addNode(StateConstants.WEATHER_AGENT, weatherAgent.getAndCompileGraph());
        graph.addNode(StateConstants.CALCULATOR_AGENT, calculatorAgent.getAndCompileGraph());
        graph.addNode(StateConstants.CHAT_AGENT, chatAgent.getAndCompileGraph());

        // 4. 定义边
        // START → Router
        graph.addEdge(START, "router");

        // Router → 条件边 → 根据 active_agent 路由到对应 Agent
        graph.addConditionalEdges("router",
                com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async(state ->
                        (String) state.value(StateConstants.ACTIVE_AGENT).orElse(END)),
                Map.of(
                        StateConstants.WEATHER_AGENT, StateConstants.WEATHER_AGENT,
                        StateConstants.CALCULATOR_AGENT, StateConstants.CALCULATOR_AGENT,
                        StateConstants.CHAT_AGENT, StateConstants.CHAT_AGENT,
                        END, END
                ));

        // 各 Agent 节点 → 条件边 → 检查 Handoff 结果
        // 如果 active_agent 被修改，路由到下一个 Agent；否则结束
        graph.addConditionalEdges(StateConstants.WEATHER_AGENT,
                RouteAfterWeatherAction.create(),
                RouteAfterWeatherAction.mappings());

        graph.addConditionalEdges(StateConstants.CALCULATOR_AGENT,
                RouteAfterCalculatorAction.create(),
                RouteAfterCalculatorAction.mappings());

        graph.addConditionalEdges(StateConstants.CHAT_AGENT,
                RouteAfterChatAction.create(),
                RouteAfterChatAction.mappings());

        // 5. 编译图
        return graph.compile();
    }
}
