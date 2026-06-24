# Travel Assistant - Agent 开发指南

## 项目概述

Travel Assistant 是一个基于 Spring AI Alibaba AgentScope 的 Multi-Agent 出行助手应用，提供路线规划、POI 搜索、天气查询等出行相关服务。支持多轮对话，通过 Redis 实现会话状态持久化。

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 21 | LTS 版本 |
| Spring Boot | 3.5.8 | Web 框架 |
| Spring AI | 1.1.2 | AI 集成框架 |
| Spring AI Alibaba | 1.1.2.2 | 阿里云 AI 组件 |
| Nacos Client | 3.2.2 | AI 注册中心 SDK |
| COLA DTO | 5.0.0 | 标准化响应体 |
| Redisson | 3.51.0 | Redis 客户端，多轮对话状态持久化 |
| Jasypt | 4.0.4 | 配置加密（API Key 等敏感信息） |
| Testcontainers | 1.21.2 | 测试容器（Redis 等） |
| Gradle | 8.14.5 | 构建工具 |
| 模型 | qwen3.7-max | 通义千问大模型 |

## 架构设计

### Multi-Agent 架构

```
Supervisor Agent (主管)
    │
    ├── AgentTool.getFunctionToolCallback()
    │   └── Travel Agent (出行规划)
    │       ├── 路线规划（驾车、骑行、步行、公交）
    │       ├── POI 搜索（地点搜索、周边搜索）
    │       ├── 天气查询
    │       ├── 地理编码
    │       └── 距离测量
    │
    └── [未来扩展] 其他子 Agent
        ├── 酒店预订 Agent
        ├── 机票查询 Agent
        └── ...
```

### 多 Agent 编排机制

使用 `AgentTool.getFunctionToolCallback()` 将子 Agent 注册为工具：

```java
// SupervisorAgent 构造函数
public SupervisorAgent(ChatModel chatModel, TravelAgent travelAgent,
                       NacosPromptService promptService, RedisSaver redisSaver) {
    String systemPrompt = promptService.getPrompt(AgentConstants.SUPERVISOR_AGENT_PROMPT);

    // 将 TravelAgent 注册为子 Agent 工具，实现多 Agent 编排
    this.agent = ReactAgent.builder()
            .name(AgentConstants.SUPERVISOR_AGENT_NAME)
            .model(chatModel)
            .systemPrompt(systemPrompt)
            .saver(redisSaver)
            .tools(AgentTool.getFunctionToolCallback(travelAgent.getAgent()))
            .build();
}
```

**工作流程**：
1. 用户发送请求 → Supervisor Agent 接收
2. Supervisor 分析意图 → 决定是否调用 TravelAgent
3. 调用 TravelAgent → 执行具体任务（路线、POI、天气等）
4. TravelAgent 返回结果 → Supervisor 整合并返回给用户

### 多轮对话机制

通过 `RedisSaver` 实现 Agent 状态持久化，使用 `RunnableConfig.threadId` 关联会话：

1. 客户端发送请求时可选传入 `threadId`
2. 服务端未收到 `threadId` 时自动生成 UUID
3. Agent 调用时传入 `RunnableConfig(threadId)` 保持上下文
4. 响应中返回 `threadId` 供客户端后续请求复用

### 工具集成

- **高德地图 MCP**: 15 个地图服务工具
- **自定义 Transport**: 通过 WebClient Filter 动态注入 API Key（解决 Spring AI 不支持 URL 查询参数的问题）
- **Nacos AI 注册中心**: Prompt 管理、MCP 服务发现与注册
- **Redis**: 多轮对话状态持久化

### 提示词管理

Agent 提示词通过 Nacos AI 注册中心的 Prompt 管理功能集中维护：

| Prompt Key | 用途 | 常量 |
|------------|------|------|
| `travel-agent-prompt` | 出行规划 Agent 提示词 | `AgentConstants.TRAVEL_AGENT_PROMPT` |
| `supervisor-agent-prompt` | 主管 Agent 提示词 | `AgentConstants.SUPERVISOR_AGENT_PROMPT` |

提示词内容使用 Markdown 格式，在 Nacos 控制台 **AI 注册中心 → Prompt 管理** 中编辑和发布。

## 项目结构

```
travel/
├── build.gradle
├── settings.gradle
├── LICENSE
├── README.md
├── AGENTS.md
├── docs/                                    # 文档目录
│   ├── supervisor-agent-prompt.md          # Supervisor Agent 提示词备份
│   └── travel-agent-prompt.md              # Travel Agent 提示词备份
└── src/
    ├── main/
    │   ├── java/com/uid13/travel/
    │   │   ├── TravelApplication.java          # 主入口
    │   │   ├── agent/
    │   │   │   ├── SupervisorAgent.java        # 主管 Agent（多 Agent 编排）
    │   │   │   └── TravelAgent.java            # 出行规划 Agent
    │   │   ├── config/
    │   │   │   ├── CustomMcpTransportConfig.java  # 自定义 MCP Transport
    │   │   │   ├── GlobalExceptionHandler.java    # 全局异常处理器
    │   │   │   └── RedisConfig.java            # Redis 配置
    │   │   ├── constant/
    │   │   │   └── AgentConstants.java         # Agent 名称和 Prompt Key 常量
    │   │   ├── controller/
    │   │   │   └── TravelController.java       # REST 控制器
    │   │   ├── dto/
    │   │   │   ├── ChatDTO.java                # 聊天响应数据传输对象
    │   │   │   └── HealthDTO.java              # 健康检查响应数据传输对象
    │   │   └── service/
    │   │       └── NacosPromptService.java     # Nacos Prompt 服务
    │   └── resources/
    │       ├── application.yml                 # 主配置文件
    │       ├── application-redis.yml           # Redis 配置
    │       └── logback-spring.xml              # 日志配置
    └── test/
        └── java/com/uid13/travel/
            └── TravelApplicationTests.java
```

## 开发规范

### 1. 代码规范

- 使用 Java 21 语法特性
- 遵循 Spring Boot 最佳实践
- 所有类添加 Javadoc 注释
- Agent 使用 `@Component` 注解管理
- 使用 Lombok 简化代码（`@RequiredArgsConstructor`、`@Slf4j`）

### 2. 常量管理

所有 Agent 名称和 Prompt Key 必须定义在 `AgentConstants` 常量类中，禁止硬编码：

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

### 3. 添加新 Agent

#### 步骤 1: 在 `AgentConstants` 中定义常量

```java
public static final String HOTEL_AGENT_NAME = "hotel-agent";
public static final String HOTEL_AGENT_PROMPT = "hotel-agent-prompt";
```

#### 步骤 2: 创建 Agent 类

在 `com.uid13.travel.agent` 包下创建新 Agent：

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class HotelAgent {

    private final ChatModel chatModel;
    private final NacosPromptService promptService;
    private final ReactAgent agent;

    @PostConstruct
    public void init() {
        String systemPrompt = promptService.getPrompt(AgentConstants.HOTEL_AGENT_PROMPT);
        this.agent = ReactAgent.builder()
                .name(AgentConstants.HOTEL_AGENT_NAME)
                .model(chatModel)
                .systemPrompt(systemPrompt)
                .build();
    }

    public ReactAgent getAgent() {
        return agent;
    }
}
```

#### 步骤 3: 在 Supervisor 中注册为工具

修改 `SupervisorAgent.java`，将新 Agent 注册为工具：

```java
@Component
public class SupervisorAgent {

    private final ReactAgent agent;

    public SupervisorAgent(ChatModel chatModel, 
                           TravelAgent travelAgent,
                           HotelAgent hotelAgent,  // 新增
                           NacosPromptService promptService, 
                           RedisSaver redisSaver) {
        String systemPrompt = promptService.getPrompt(AgentConstants.SUPERVISOR_AGENT_PROMPT);

        // 将多个子 Agent 注册为工具
        this.agent = ReactAgent.builder()
                .name(AgentConstants.SUPERVISOR_AGENT_NAME)
                .model(chatModel)
                .systemPrompt(systemPrompt)
                .saver(redisSaver)
                .tools(
                    AgentTool.getFunctionToolCallback(travelAgent.getAgent()),
                    AgentTool.getFunctionToolCallback(hotelAgent.getAgent())
                )
                .build();
    }
}
```

#### 步骤 4: 更新 Supervisor 提示词

在 Nacos Prompt 管理中更新 `supervisor-agent-prompt`，添加新 Agent 的描述和调用逻辑：

```markdown
你是出行助手的主管，负责理解用户的出行需求并协调子 Agent 完成任务。

你的职责：
1. 理解用户意图（路线规划、地点搜索、天气查询、酒店预订等）
2. 将任务分发给合适的子 Agent
3. 整合子 Agent 的返回结果，给出最终回答

子 Agent：
- travel-agent：出行规划专家，负责路线规划、POI 搜索、天气查询等
- hotel-agent：酒店预订专家，负责酒店搜索、预订、价格查询等

工作流程：
1. 分析用户需求
2. 调用合适的子 Agent 执行具体任务
3. 整合结果并返回给用户

注意：
- 如果用户需求不明确，请主动询问澄清
- 返回结果要简洁、实用
- 对于复杂需求，可以分解为多个子任务
```

### 4. Nacos Prompt 管理

提示词通过 Nacos AI SDK 获取：

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class NacosPromptService {

    private final NacosMcpProperties nacosMcpProperties;
    private AiService aiService;

    @PostConstruct
    public void init() throws NacosException {
        this.aiService = AiFactory.createAiService(nacosMcpProperties.getNacosProperties());
    }

    public String getPrompt(String promptKey) {
        Prompt prompt = aiService.getPrompt(promptKey);
        return prompt.getTemplate();
    }
}
```

配置复用 `spring.ai.alibaba.mcp.nacos.*`，与 MCP 注册共用同一套 Nacos 连接配置。

### 5. 配置说明

项目使用 Profile 机制管理配置，Redis 配置独立为 `application-redis.yml`。

#### 主配置文件（application.yml）

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `spring.profiles.include` | 包含的 Profile | redis |
| `spring.ai.dashscope.api-key` | 百炼 API Key（Jasypt 加密） | - |
| `spring.ai.dashscope.chat.options.model` | 模型名称 | qwen3.7-max |
| `spring.ai.mcp.client.streamable-http.connections` | MCP 连接配置（禁用自动配置） | {} |
| `spring.ai.alibaba.mcp.nacos.server-addr` | Nacos 地址 | 192.168.0.100:8848 |
| `spring.ai.alibaba.mcp.nacos.namespace` | 命名空间 | public |
| `spring.ai.alibaba.mcp.nacos.username` | 用户名 | nacos |
| `spring.ai.alibaba.mcp.nacos.password` | 密码 | nacos |
| `amap.api.key` | 高德 API Key（Jasypt 加密） | - |
| `jasypt.encryptor.algorithm` | Jasypt 加密算法 | PBEWITHHMACSHA512ANDAES_256 |
| `jasypt.encryptor.password` | Jasypt 密码（环境变量） | ${JASYPT_ENCRYPTOR_PASSWORD} |
| `server.port` | 服务端口 | 4081 |

#### Redis 配置文件（application-redis.yml）

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `spring.data.redis.host` | Redis 地址 | localhost |
| `spring.data.redis.port` | Redis 端口 | 6379 |

### 6. 构建与测试

```bash
# 构建项目
./gradlew build

# 运行测试
./gradlew test

# 启动应用（需要设置 Jasypt 密码环境变量）
export JASYPT_ENCRYPTOR_PASSWORD=your-password
./gradlew bootRun

# 清理构建
./gradlew clean
```

### 7. 依赖管理

项目使用 BOM 管理依赖版本：

- `spring-ai-bom:1.1.2`
- `spring-ai-alibaba-bom:1.1.2.2`

添加新依赖时，优先使用 BOM 中已定义的版本。

### 8. Maven 仓库配置

项目使用阿里云 Maven 镜像加速依赖下载：

- `https://maven.aliyun.com/repository/public/` - 公共仓库
- `https://maven.aliyun.com/repository/spring/` - Spring 仓库

## API 接口

### POST /api/chat

聊天接口（支持多轮对话），用户发送消息，Agent 处理并返回结果。

**请求体**:
```json
{
  "message": "帮我规划从北京到上海的路线",
  "threadId": "可选，会话 ID"
}
```

**响应**（COLA SingleResponse 格式）:
```json
{
  "success": true,
  "errCode": null,
  "errMessage": null,
  "data": {
    "message": "从北京到上海的驾车路线...",
    "agent": "travel-supervisor",
    "threadId": "会话 ID"
  }
}
```

### GET /api/health

健康检查接口。

**响应**（COLA SingleResponse 格式）:
```json
{
  "success": true,
  "errCode": null,
  "errMessage": null,
  "data": {
    "status": "UP",
    "service": "travel-assistant",
    "version": "1.0.0"
  }
}
```

## 常见问题

### Q: 如何调试 Agent？

A: 使用 `@Slf4j` 记录日志，或在 Nacos Prompt 管理中修改提示词后重启应用。

### Q: 如何添加新的 MCP 工具？

A: 在 Nacos MCP 管理中注册新的 MCP Server，应用会自动发现并注入工具。

### Q: 如何切换模型？

A: 修改 `application.yml` 中的 `spring.ai.dashscope.chat.options.model` 配置项。

### Q: Prompt 修改后需要重启应用吗？

A: 当前实现需要重启。如需热更新，可使用 `aiService.subscribePrompt()` 订阅 Prompt 变更事件。

### Q: 多轮对话如何工作？

A: 客户端在请求中传入 `threadId`（或让服务端自动生成），Agent 通过 `RunnableConfig(threadId)` 从 Redis 加载历史状态，实现上下文保持。不同 `threadId` 之间会话完全隔离。

### Q: Redis 不可用时会影响服务吗？

A: 当前实现中 Redis 是必需的，不可用时 Agent 初始化会失败。后续可考虑降级为内存模式（`MemorySaver`）。

### Q: 为什么需要自定义 MCP Transport？

A: Spring AI MCP Client 不支持 URL 查询参数，而高德 MCP 需要通过 URL 传递 API Key。通过自定义 `CustomMcpTransportConfig`，使用 WebClient Filter 在请求时动态添加 API Key。

### Q: 如何加密 API Key？

A: 使用 Jasypt 加密。创建临时测试类加密 API Key，将加密后的值填入配置文件，运行时通过环境变量 `JASYPT_ENCRYPTOR_PASSWORD` 提供解密密钥。

### Q: 配置文件如何按环境区分？

A: 使用 Spring Profile 机制。主配置文件 `application.yml` 通过 `spring.profiles.include: redis` 自动加载 `application-redis.yml`。可以为不同环境创建不同的配置文件（如 `application-dev.yml`、`application-prod.yml`）。

## 参考文档

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Spring AI Alibaba 文档](https://java2ai.com/)
- [AgentScope Java 文档](https://github.com/alibaba/spring-ai-alibaba)
- [高德地图 MCP 文档](https://lbs.amap.com/api/mcp-server)
- [Nacos Prompt 管理文档](https://nacos.io/docs/latest/manual/user/ai/prompt-registry/)
- [Nacos AI SDK 文档](https://nacos.io/docs/latest/manual/user/java-sdk/usage/)
- [Jasypt Spring Boot 文档](https://github.com/ulisesbocchio/jasypt-spring-boot)

## 许可证

MIT License
