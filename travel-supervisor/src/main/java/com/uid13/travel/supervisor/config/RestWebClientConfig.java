package com.uid13.travel.supervisor.config;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionProperties;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

/**
 * Supervisor HTTP 客户端超时配置
 * 用于 DashScope API 调用
 *
 * 根本原因：
 * 1. DashScopeConnectionProperties.readTimeout 配置没有被 DashScopeChatAutoConfiguration 使用
 * 2. DashScopeApi.Builder 内部硬编码了 60 秒超时（createDefaultWebClientBuilder）
 * 3. spring.main.allow-bean-definition-overriding=true 会导致自动配置覆盖我们的 @Primary Bean
 *
 * 解决方案：
 * 1. exclude DashScopeChatAutoConfiguration 自动配置
 * 2. 手动 @EnableConfigurationProperties 注册 DashScopeConnectionProperties
 * 3. 直接定义 DashScopeApi Bean（@Primary），复用已配置的 RestClient.Builder 和 WebClient.Builder
 *
 * @author uid13
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(DashScopeConnectionProperties.class)
public class RestWebClientConfig {

    /**
     * 自定义 DashScopeApi Bean，复用已配置的 HTTP 客户端
     */
    @Bean
    @Primary
    public DashScopeApi dashScopeApi(
            DashScopeConnectionProperties properties,
            RestClient.Builder restClientBuilder,
            WebClient.Builder webClientBuilder) {

        log.info("=== 自定义 DashScopeApi 初始化，复用已配置的 HTTP 客户端（5 分钟超时）===");

        return DashScopeApi.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .restClientBuilder(restClientBuilder)
                .webClientBuilder(webClientBuilder)
                .build();
    }

    /**
     * 自定义 DashScopeChatModel Bean，覆盖自动配置的 ChatModel
     */
    @Bean
    @Primary
    public ChatModel dashScopeChatModel(DashScopeApi dashScopeApi) {
        log.info("=== 自定义 DashScopeChatModel 初始化 ===");

        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel("qwen3.7-max")
                        .build())
                .build();
    }

    /**
     * 配置 RestClient.Builder（同步请求）- 5 分钟超时
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        log.info("=== Supervisor RestClient.Builder 初始化，设置 5 分钟超时 ===");
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofMinutes(5).toMillis());
        factory.setReadTimeout((int) Duration.ofMinutes(5).toMillis());
        return RestClient.builder().requestFactory(factory);
    }

    /**
     * 配置 WebClient.Builder（流式请求）- 5 分钟超时
     * 关键：必须添加 ReadTimeoutHandler，否则 Netty 默认 60 秒超时
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        log.info("=== Supervisor WebClient.Builder 初始化，设置 5 分钟超时 ===");

        ConnectionProvider provider = ConnectionProvider.builder("dashscope")
                .maxConnections(500)
                .maxIdleTime(Duration.ofMinutes(10))
                .maxLifeTime(Duration.ofMinutes(30))
                .evictInBackground(Duration.ofSeconds(60))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .responseTimeout(Duration.ofMinutes(5))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(300))
                        .addHandlerLast(new WriteTimeoutHandler(10))
                );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    /**
     * 配置 Apache HttpClient 超时（A2A 通信使用）
     * A2aNodeActionWithConfig 内部使用 HttpClients.createDefault()
     */
    @Bean
    public CloseableHttpClient a2aHttpClient() {
        log.info("=== A2A Apache HttpClient 初始化，设置 5 分钟超时 ===");
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(10000)
                .setSocketTimeout(5 * 60 * 1000)
                .setConnectionRequestTimeout(10000)
                .build();
        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

}
