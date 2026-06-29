package com.uid13.travel.supervisor.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

/**
 * Travel Agent HTTP 客户端超时配置
 * 用于 DashScope API 调用
 * 参考官方解决方案：https://github.com/alibaba/spring-ai-alibaba/issues/3331
 *
 * @author uid13
 */
@Slf4j
@Configuration
public class RestWebClientConfig {

    /**
     * 配置 RestClient.Builder（同步请求）
     * 解决 DashScopeApi 内部 clone() 可能丢失 Customizer 配置的问题
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        log.info("=== TravelAgent RestClient.Builder 初始化，设置 5 分钟超时 ===");
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofMinutes(5).toMillis());
        factory.setReadTimeout((int) Duration.ofMinutes(5).toMillis());
        return RestClient.builder().requestFactory(factory);
    }

    /**
     * 配置 WebClient.Builder（流式请求）
     * 关键：必须添加 ReadTimeoutHandler，否则 Netty 默认 60 秒超时
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        log.info("=== TravelAgent WebClient.Builder 初始化，设置 5 分钟超时 ===");

        // 配置连接池
        ConnectionProvider provider = ConnectionProvider.builder("dashscope")
                .maxConnections(500)
                .maxIdleTime(Duration.ofMinutes(10))
                .maxLifeTime(Duration.ofMinutes(30))
                .evictInBackground(Duration.ofSeconds(60))
                .build();

        // 配置 HTTP 客户端
        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)  // 连接超时 10 秒
                .responseTimeout(Duration.ofMinutes(5))  // 响应超时 5 分钟
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(300))  // 读超时 5 分钟（关键！）
                        .addHandlerLast(new WriteTimeoutHandler(10))  // 写超时 10 秒
                );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    /**
     * 配置 Apache HttpClient 超时（A2A 通信使用）
     * A2aNodeActionWithConfig 内部使用 HttpClients.createDefault()
     * 通过自定义 Bean 覆盖默认配置
     */
    @Bean
    public CloseableHttpClient a2aHttpClient() {
        log.info("=== A2A Apache HttpClient 初始化，设置 5 分钟超时 ===");
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(10000)
                .setSocketTimeout(5 * 60 * 1000)  // 5 分钟
                .setConnectionRequestTimeout(10000)
                .build();
        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

}
