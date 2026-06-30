package com.uid13.demo.tools;

import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import com.uid13.demo.state.StateConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 移交工具：任意专家 → 通用聊天 Agent
 *
 * 当专家 Agent 遇到无法处理的闲聊或通用问题时，调用此工具将控制权移交给 ChatAgent。
 */
@Slf4j
@Component
public class TransferToChatTool {

    @Tool(
            name = "transfer_to_chat",
            description = "当用户问题是闲聊、问候或与天气/计算无关的通用问题时，移交聊天助手处理",
            returnDirect = true
    )
    public String transferToChat(
            @ToolParam(description = "移交原因，说明为什么需要聊天助手") String reason,
            ToolContext toolContext) {

        log.info("[Handoff] Expert → ChatAgent, 原因：{}", reason);

        ToolContextHelper.getStateForUpdate(toolContext).ifPresent(update ->
                update.put(StateConstants.ACTIVE_AGENT, StateConstants.CHAT_AGENT));

        return "已移交聊天助手：" + reason;
    }
}
