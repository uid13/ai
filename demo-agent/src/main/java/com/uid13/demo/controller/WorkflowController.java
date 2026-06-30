package com.uid13.demo.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cola.dto.SingleResponse;
import com.uid13.demo.state.StateConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 混合架构工作流控制器
 *
 * 驱动 StateGraph 执行，展示 Workflow + Handoffs 的完整流程。
 * 返回丰富的执行轨迹信息，便于观察 Handoff 路由过程。
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {

    private final CompiledGraph hybridGraph;

    public WorkflowController(@Qualifier("hybridGraph") CompiledGraph hybridGraph) {
        this.hybridGraph = hybridGraph;
    }

    /**
     * 执行混合架构工作流
     *
     * 流程：START → Router(LLM 意图分类) → Expert Agent(可能 Handoff) → ... → END
     *
     * 示例：
     * - GET /api/workflow/run?message=北京今天天气怎么样
     *   → Router 分类为 weather → WeatherAgent → END
     *
     * - GET /api/workflow/run?message=你好，帮我算下 125 乘以 38
     *   → Router 分类为 chat → ChatAgent → Handoff → CalculatorAgent → END
     *
     * - GET /api/workflow/run?message=今天天气怎么样，另外 100 美元换多少人民币
     *   → Router 分类为 weather → WeatherAgent → Handoff → CalculatorAgent → END
     */
    @GetMapping("/run")
    public SingleResponse<Map<String, Object>> runWorkflow(@RequestParam String message) {
        // 1. 初始化状态
        Map<String, Object> initialState = new HashMap<>();
        initialState.put(StateConstants.INPUT, message);
        initialState.put(StateConstants.ACTIVE_AGENT, "");
        initialState.put(StateConstants.ROUTE_TRACE, "");

        // 2. 执行图
        OverAllState finalState = hybridGraph.invoke(initialState).orElseThrow();

        // 3. 提取结果
        String result = (String) finalState.value(StateConstants.RESULT).orElse("");
        String routeTrace = (String) finalState.value(StateConstants.ROUTE_TRACE).orElse("");
        String activeAgent = (String) finalState.value(StateConstants.ACTIVE_AGENT).orElse("");

        // 4. 提取消息历史（用于调试）
        @SuppressWarnings("unchecked")
        List<Object> messages = (List<Object>) finalState.value("messages").orElse(List.of());

        Map<String, Object> data = new HashMap<>();
        data.put("type", "hybrid_workflow");
        data.put("input", message);
        data.put("response", result.isEmpty() ? extractLastMessage(messages) : result);
        data.put("route_trace", routeTrace);
        data.put("final_active_agent", activeAgent);
        data.put("messages_count", messages.size());

        log.info("[Workflow] 输入={}, 路由轨迹={}, 最终 Agent={}", message, routeTrace, activeAgent);

        return SingleResponse.of(data);
    }

    /**
     * 从消息历史中提取最后一条助手消息作为响应
     */
    private String extractLastMessage(List<Object> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof AssistantMessage am) {
                return am.getText();
            }
        }
        return "无响应";
    }
}
