package com.uid13.demo.tools;

import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import com.uid13.demo.state.StateConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 移交工具：计算专家 → 天气专家
 *
 * 当 CalculatorAgent 遇到天气查询或时间问题时，调用此工具将控制权移交给 WeatherAgent。
 */
@Slf4j
@Component
public class TransferToWeatherTool {

    @Tool(
            name = "transfer_to_weather",
            description = "当用户问题涉及天气查询、日期时间时，移交天气专家处理",
            returnDirect = true
    )
    public String transferToWeather(
            @ToolParam(description = "移交原因，说明为什么需要天气专家") String reason,
            ToolContext toolContext) {

        log.info("[Handoff] CalculatorAgent → WeatherAgent, 原因：{}", reason);

        ToolContextHelper.getStateForUpdate(toolContext).ifPresent(update ->
                update.put(StateConstants.ACTIVE_AGENT, StateConstants.WEATHER_AGENT));

        return "已移交天气专家：" + reason;
    }
}
