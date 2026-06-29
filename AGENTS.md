# Travel Assistant - 开发指南

## 项目概述

Travel Assistant 是一个基于 Spring AI Alibaba A2A 协议的分布式多 Agent 出行助手应用，提供路线规划、POI 搜索、天气查询等出行相关服务。

- **travel-supervisor**：A2A Client，主管 Agent，负责理解用户意图、分发任务（端口 8081）
- **travel-agent**：A2A Server，出行规划 Agent，执行具体任务（端口 8082）
- **travel-common**：公共模块，存放 DTO、常量、共享服务

## 架构设计

### A2A 分布式多 Agent 架构

```
SupervisorAgent (LlmRoutingAgent, travel-supervisor)
  │
  │ A2A 协议（通过 Nacos Agent Registry 发现）
  ▼
A2aRemoteAgent → TravelAgent (ReactAgent, travel-agent)
  │
  │ MCP Streamable-HTTP（自定义 Transport）
  ▼
高德地图 MCP Server（15 个工具）
```

**关键类映射**：

| 角色 | 类名 | 模块 | 说明 |
|------|------|------|------|
| 主管 | `SupervisorAgentConfig` | supervisor | 定义 LlmRoutingAgent + A2aRemoteAgent |
| 子 Agent（远程） | `A2aRemoteAgent` | supervisor | 从 Nacos 发现远程 travel-agent |
| 子 Agent（实现） | `TravelAgentConfig` | agent | 定义 ReactAgent，调用 MCP 工具 |
| MCP 工具 | `CustomMcpTransportConfig` | agent | 解决 Spring AI URL 查询参数问题 |

### 多 Agent 编排机制

Supervisor 使用 `LlmRoutingAgent` 实现意图路由：

```java
// SupervisorAgent 构造函数
this.agent = LlmRoutingAgent.builder()
        .name(AgentConstants.SUPERVISOR_AGENT_NAME)
        .model(chatModel)
        .systemPrompt(systemPrompt)
        .subAgents(List.of(travelAgent)) // A2aRemoteAgent
        .build();
```

`LlmRoutingAgent` 会根据系统提示词中描述的子 Agent 能力，自动决定将请求路由给哪个子 Agent。意图识别由 LLM 驱动，无需硬编码关键词。

**工作流程**：
1. 用户发送请求 → `TravelController.chat()` 接收
2. `SupervisorAgent.getAgent().invoke()` → LlmRoutingAgent 分析意图
3. LLM 决定调用 travel-agent → 通过 A2A 协议远程调用
4. travel-agent 的 `TravelAgent` 接收 → ReAct 循环调用 MCP 工具
5. 结果返回 Supervisor → 整合并返回给用户

### 多轮对话机制

通过 `RedisSaver` 实现 Agent 状态持久化，使用 `RunnableConfig.threadId` 关联会话：

1. 客户端发送请求时可选传入 `threadId`
2. 服务端未收到 `threadId` 时自动生成 UUID
3. Agent 调用时传入 `RunnableConfig(threadId)` 保持上下文
4. 响应中返回 `threadId` 供客户端后续请求复用

### 模块扫描

由于 `travel-common` 中的 `NacosPromptService` 需要在两个应用模块中被扫描到，启动类需要配置 `@ComponentScan`：

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.uid13.travel.supervisor", "com.uid13.travel.common"})
public class SupervisorApplication { ... }
```

## 项目结构

```
travel/
├── build.gradle                          # 父项目（公共配置）
├── settings.gradle                       # 多模块配置
├── travel-common/                        # 公共模块
│   ├── build.gradle
│   └── src/main/java/com/uid13/travel/common/
│       ├── constant/AgentConstants.java  # Agent 名称 + Prompt Key 常量
│       ├── dto/ChatDTO.java             # 聊天响应
│       ├── dto/HealthDTO.java           # 健康检查响应
│       └── service/NacosPromptService.java  # Nacos Prompt 服务
├── travel-agent/                         # A2A Server
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/uid13/travel/agent/
│       │   ├── TravelAgentApplication.java
│       │   └── config/
│       │       ├── CustomMcpTransportConfig.java
│       │       └── TravelAgentConfig.java    # ReactAgent
│           ├── application.yml
│           └── logback-spring.xml
├── travel-supervisor/                    # A2A Client
│   ├── build.gradle
│   └── src/
│       ├── main/
│       │   ├── java/com/uid13/travel/supervisor/
│       │   │   ├── SupervisorApplication.java
│       │   │   ├── config/
│       │   │   │   ├── GlobalExceptionHandler.java
│       │   │   │   ├── RedisConfig.java
│       │   │   │   └── SupervisorAgentConfig.java  # LlmRoutingAgent + A2aRemoteAgent
│       │   │   └── controller/
│       │   │       └── TravelController.java
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-redis.yml
│       │       └── logback-spring.xml
│       └── test/http/travel-chat.http
└── docs/
    ├── A2A-MIGRATION.md
    ├── supervisor-agent-prompt.md
    └── travel-agent-prompt.md
```

## 开发规范

### 1. 常量管理

所有 Agent 名称和 Prompt Key 必须定义在 `travel-common` 的 `AgentConstants` 中：

```java
public final class AgentConstants {
    // Agent 名称
    public static final String TRAVEL_AGENT_NAME = "travel-agent";
    public static final String SUPERVISOR_AGENT_NAME = "travel-supervisor";

    // Prompt Key
    public static final String TRAVEL_AGENT_PROMPT = "travel-agent-prompt";
    public static final String SUPERVISOR_AGENT_PROMPT = "supervisor-agent-prompt";
}
```

### 2. 添加新的子 Agent

以下以添加 "酒店预订 Agent" 为例：

#### 步骤 1: 在 `AgentConstants` 中定义常量

```java
public static final String HOTEL_AGENT_NAME = "hotel-agent";
public static final String HOTEL_AGENT_PROMPT = "hotel-agent-prompt";
```

#### 步骤 2: 创建新子模块 `travel-hotel`

创建独立子模块作为 A2A Server，包含：
- `HotelAgent.java` — ReactAgent 实现
- 对应的 MCP 工具配置
- `application.yml` 注册到 Nacos Agent Registry

#### 步骤 3: 在 Supervisor 中配置 A2A Client

```java
@Bean
public A2aRemoteAgent hotelAgent(AgentCardProvider agentCardProvider) {
    return A2aRemoteAgent.builder()
            .agentCardProvider(agentCardProvider)
            .name(AgentConstants.HOTEL_AGENT_NAME)
            .description("酒店预订专家")
            .build();
}
```

#### 步骤 4: 注册到 SupervisorAgent

```java
public SupervisorAgent(ChatModel chatModel,
                       A2aRemoteAgent travelAgent,
                       A2aRemoteAgent hotelAgent,
                       NacosPromptService promptService) {
    this.agent = LlmRoutingAgent.builder()
            .name(AgentConstants.SUPERVISOR_AGENT_NAME)
            .model(chatModel)
            .systemPrompt(systemPrompt)
            .subAgents(List.of(travelAgent, hotelAgent))
            .build();
}
```

#### 步骤 5: 在 Nacos 中配置提示词

在 Nacos 控制台 **AI 注册中心 → Prompt 管理** 中发布 `hotel-agent-prompt`，并更新 `supervisor-agent-prompt` 添加新 Agent 描述。

### 3. 依赖管理

- `travel-common` 使用 `api` 配置传递依赖（如 nacos-client、cola-component-dto）
- `travel-agent` / `travel-supervisor` 使用 `implementation` 引用 `:travel-common`
- Spring Boot 插件（`org.springframework.boot`）仅在应用模块中 apply
- Lombok 版本统一由父 `build.gradle` 的 `ext` 管理

### 4. 配置说明

- **Jasypt 加密**：敏感信息（API Key）使用 `ENC(...)` 格式存储，运行时通过环境变量 `JASYPT_ENCRYPTOR_PASSWORD` 解密
- **Redis 配置**：独立为 `application-redis.yml`，通过 `spring.profiles.include: redis` 加载
- **日志配置**：`logback-spring.xml` 支持 `<springProfile>` 区分 dev/prod 环境
- **MCP Client**：禁用 Spring AI 自动配置连接（`connections: {}`），使用 `CustomMcpTransportConfig` 手动创建

### 5. 构建命令

```bash
# 构建所有模块
./gradlew build

# 仅构建 travel-agent
./gradlew :travel-agent:build

# 仅构建 travel-supervisor
./gradlew :travel-supervisor:build

# 运行测试
./gradlew test

# 启动应用
cd travel-agent && ../gradlew bootRun
cd travel-supervisor && ../gradlew bootRun
```

### 6. 代码规范

- 使用 Java 21 语法特性
- 所有类添加 Javadoc 中文注释
- 使用 `@Slf4j` 记录日志
- Agent 使用 `@Component` 注解管理
- 使用 Lombok 简化代码（`@Slf4j`、`@RequiredArgsConstructor`）
- 禁止硬编码 Agent 名称和 Prompt Key，必须使用 `AgentConstants`

## 常见问题

### Q: 为什么需要两个独立应用？

A: A2A 分布式架构允许多个 Agent 独立部署、独立扩展。travel-agent 可以作为独立服务被多个 Supervisor 调用，未来还可以接入更多类型的 Agent。

### Q: 自定义 MCP Transport 的作用？

A: Spring AI MCP Client 不支持 URL 查询参数，而高德 MCP 需要通过 URL 传递 API Key。`CustomMcpTransportConfig` 通过 WebClient Filter 动态注入 API Key。参见 [Issue #6505](https://github.com/spring-projects/spring-ai/issues/6505)。

### Q: Nacos 中需要配置什么？

A: 需要配置：
1. **Agent Registry**：travel-agent 的 A2A 服务注册（自动）
2. **Prompt 管理**：`travel-agent-prompt` 和 `supervisor-agent-prompt` 两个提示词

### Q: Redis 不可用会怎样？

A: Supervisor 依赖 Redis 实现多轮对话状态持久化，不可用时应用无法正常运行。Redis 仅 Supervisor 端需要。

## 参考文档

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Spring AI Alibaba 文档](https://java2ai.com/)
- [Spring AI Alibaba A2A 文档](https://java2ai.com/docs/frameworks/agent-framework/advanced/a2a)
- [Nacos Agent Registry](https://nacos.io/docs/latest/manual/user/ai/agent-registry)
- [高德地图 MCP 文档](https://lbs.amap.com/api/mcp-server)
- [Nacos Prompt 管理文档](https://nacos.io/docs/latest/manual/user/ai/prompt-registry/)
- [Spring AI MCP Issue #6505](https://github.com/spring-projects/spring-ai/issues/6505)
- [Jasypt Spring Boot 文档](https://github.com/ulisesbocchio/jasypt-spring-boot)
