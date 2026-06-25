package com.uid13.travel.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.transport.WebClientStreamableHttpTransport;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.client.common.autoconfigure.NamedClientMcpTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * 自定义 MCP Transport 配置
 * 解决 Spring AI MCP Client 不支持 URL 查询参数的问题
 * 通过 WebClient Filter 在请求时动态添加 API Key 查询参数
 * 
 * @see <a href="https://github.com/spring-projects/spring-ai/issues/6505">Issue #6505</a>
 */
@Slf4j
@Configuration
public class CustomMcpTransportConfig {

    @Value("${amap.api.key}")
    private String amapApiKey;

    /**
     * 自定义高德地图 MCP Transport
     * 通过 WebClient Filter 在请求时动态添加 API Key 查询参数
     * 
     * @return NamedClientMcpTransport 高德地图 MCP 传输层
     */
    @Bean
    public NamedClientMcpTransport amapTransport() {
        log.info("Creating custom Amap MCP Transport with API Key: {}",
            amapApiKey != null ? "****" : "null");
        
        var webClientBuilder = WebClient.builder()
            .baseUrl("https://mcp.amap.com")
            .filter((request, next) -> {
                // 在请求 URL 中添加 API Key 查询参数
                URI newUri = UriComponentsBuilder.fromUri(request.url())
                    .queryParam("key", amapApiKey)
                    .build()
                    .toUri();
                
                log.debug("Adding API Key to request URL: {}", newUri);
                
                return next.exchange(ClientRequest.from(request)
                    .url(newUri)
                    .build());
            });
        
        var transport = WebClientStreamableHttpTransport.builder(webClientBuilder)
            .endpoint("/mcp")
            .jsonMapper(new JacksonMcpJsonMapper(new ObjectMapper()))
            .build();
        
        log.info("Custom Amap MCP Transport created successfully");
        
        return new NamedClientMcpTransport("amap", transport);
    }
}
