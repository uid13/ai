package com.uid13.travel.agent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

/**
 * 出行规划 Agent（子 Agent）
 * 负责路线规划、POI 搜索、天气查询等出行相关任务
 * 调用高德地图 MCP 工具
 *
 * @author uid13
 */
@Component
public class TravelAgent {

    private final ReactAgent agent;

    public TravelAgent(ChatModel chatModel, ToolCallbackProvider amapTools) {
        this.agent = ReactAgent.builder()
                .name("travel-agent")
                .model(chatModel)
                .tools(amapTools.getToolCallbacks())
                .systemPrompt("""
                        你是出行规划专家，负责帮助用户规划出行路线、搜索地点、查询天气等。
                        你可以使用高德地图的工具来完成以下任务：
                        1. 路线规划（驾车、骑行、步行、公交）
                        2. POI 搜索（地点搜索、周边搜索）
                        3. 天气查询
                        4. 地理编码（地址转坐标、坐标转地址）
                        5. 距离测量
                        
                        请根据用户需求，选择合适的工具完成任务。
                        返回结果要简洁明了，便于用户理解。
                        """)
                .build();
    }

    /**
     * 获取 Agent 实例
     *
     * @return ReactAgent
     */
    public ReactAgent getAgent() {
        return agent;
    }
}
