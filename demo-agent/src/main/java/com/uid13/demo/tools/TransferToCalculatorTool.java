package com.uid13.demo.tools;

import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import com.uid13.demo.state.StateConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 移交工具：天气专家 → 计算专家
 *
 * 当 WeatherAgent 遇到数学计算或汇率换算问题时，调用此工具将控制权移交给 CalculatorAgent。
 *
 * 核心机制（参考官方 handoffs 示例）：
 * 1. returnDirect = true：工具执行后立即退出当前 Agent，不再让 LLM 继续生成
 * 2. ToolContextHelper.getStateForUpdate()：修改图的 active_agent 状态
 * 3. 图的条件边检测到 active_agent 变化后，路由到目标 Agent 节点
 */
@Slf4j
@Component
public class TransferToCalculatorTool {

    @Tool(
            name = "transfer_to_calculator",
            description = "当用户问题涉及数学计算、汇率换算或数字运算时，移交计算专家处理",
            returnDirect = true
    )
    public String transferToCalculator(
            @ToolParam(description = "移交原因，说明为什么需要计算专家") String reason,
            ToolContext toolContext) {

        log.info("[Handoff] WeatherAgent → CalculatorAgent, 原因：{}", reason);

        ToolContextHelper.getStateForUpdate(toolContext).ifPresent(update ->
                update.put(StateConstants.ACTIVE_AGENT, StateConstants.CALCULATOR_AGENT));

        return "已移交计算专家：" + reason;
    }
}
