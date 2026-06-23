package com.uid13.travel.config;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP 工具配置类
 * 自动注入高德 MCP Client 的工具
 *
 * @author uid13
 */
@Configuration
public class McpToolConfig {

    /**
     * 高德 MCP 工具提供者
     * 从 Nacos 发现的高德 MCP Server 自动注入
     *
     * @param amapTools 高德 MCP 工具回调
     * @return ToolCallbackProvider
     */
    @Bean
    public ToolCallbackProvider amapTools(ToolCallbackProvider amapTools) {
        return amapTools;
    }
}
