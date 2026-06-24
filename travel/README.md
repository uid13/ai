# Travel Assistant - 出行助手

基于 Spring AI Alibaba AgentScope 的 Multi-Agent 出行助手应用。

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

```
┌──────────────────────────────────────────────────────────┐
│  Travel Assistant (出行助手)                               │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │  Supervisor Agent (主管)                            │  │
│  │  - 从 Nacos Prompt 管理获取提示词                     │  │
│  │  - 理解用户意图，通过 AgentTool 调用子 Agent          │  │
│  │  - 支持多轮对话（Redis 状态持久化）                     │  │
│  └─────────────────────┬──────────────────────────────┘  │
│                        │ AgentTool                        │
│  ┌─────────────────────▼──────────────────────────────┐  │
│  │  Travel Agent (子 Agent)                            │  │
│  │  - 从 Nacos Prompt 管理获取提示词                     │  │
│  │  - 路线规划、POI 搜索、天气查询                        │  │
│  │  - 调用高德 MCP 工具                                 │  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
         │
         │ Nacos AI SDK (AiService)
         ▼
┌──────────────────────────────────────────────────────────┐
│  Nacos 3.2.2 AI 注册中心                                   │
│  - Prompt 管理（提示词版本管理、标签管理）                    │
│  - MCP 管理（高德 MCP Server 注册与发现）                    │
└──────────────────────────────────────────────────────────┘
         │
         │ MCP Streamable-HTTP (自定义 Transport)
         ▼
──────────────────────────────────────────────────────────┐
│  高德地图 MCP Server                                       │
│  - 路线规划、POI 搜索、天气查询、地理编码等 15 个工具          │
│  - 通过 WebClient Filter 动态注入 API Key                  │
└──────────────────────────────────────────────────────────┘
         │
         │ Redis 7.x
         ▼
┌──────────────────────────────────────────────────────────┐
│  Redis                                                    │
│  - 多轮对话状态持久化（Checkpoint）                          │
│  - 会话隔离（通过 threadId 区分）                            │
└──────────────────────────────────────────────────────────┘
```

### 多 Agent 编排机制

使用 `AgentTool.getFunctionToolCallback()` 将子 Agent 注册为工具：

```java
// SupervisorAgent 构造函数
this.agent = ReactAgent.builder()
    .name(AgentConstants.SUPERVISOR_AGENT_NAME)
    .model(chatModel)
    .systemPrompt(systemPrompt)
    .saver(redisSaver)
    .tools(AgentTool.getFunctionToolCallback(travelAgent.getAgent()))
    .build();
```

**工作流程**：
1. 用户发送请求 → Supervisor Agent 接收
2. Supervisor 分析意图 → 决定是否调用 TravelAgent
3. 调用 TravelAgent → 执行具体任务（路线、POI、天气等）
4. TravelAgent 返回结果 → Supervisor 整合并返回给用户

## 快速开始

### 环境要求

- JDK 21+
- Gradle 8.14.5（已内置 Wrapper）
- Nacos 3.2.2（用于 Prompt 管理和 MCP 服务注册）
- Redis 7.x（用于多轮对话状态持久化）
- 阿里云百炼 API Key
- 高德地图 API Key

### 配置敏感信息

项目使用 Jasypt 加密敏感配置（API Key 等），需要先加密配置：

1. 创建临时测试类加密 API Key：

```java
@Test
public void encryptApiKeys() {
    StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
    SimpleStringPBEConfig config = new SimpleStringPBEConfig();
    config.setPassword("your-password");
    config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
    config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
    encryptor.setConfig(config);

    System.out.println("DashScope Key: ENC(" + encryptor.encrypt("your-dashscope-key") + ")");
    System.out.println("Amap Key: ENC(" + encryptor.encrypt("your-amap-key") + ")");
}
```

2. 将加密后的值填入 `application.yml`：

```yaml
spring:
  ai:
    dashscope:
      api-key: ENC(加密后的值)

amap:
  api:
    key: ENC(加密后的值)

jasypt:
  encryptor:
    algorithm: PBEWITHHMACSHA512ANDAES_256
    iv-generator-classname: org.jasypt.iv.RandomIvGenerator
    password: ${JASYPT_ENCRYPTOR_PASSWORD}
```

### 构建项目

```bash
./gradlew build
```

### 运行应用

```bash
# 设置 Jasypt 密码环境变量
export JASYPT_ENCRYPTOR_PASSWORD=your-password

# 启动应用
./gradlew bootRun
```

应用启动后，访问 `http://localhost:4081`

### API 接口

#### 聊天接口（支持多轮对话）

**请求体**:
```json
{
  "message": "帮我规划从北京到上海的路线",
  "threadId": "可选，会话 ID，用于多轮对话上下文关联"
}
```

**请求示例**:
```bash
# 首轮对话（不传 threadId，服务端自动生成）
curl -X POST http://localhost:4081/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "帮我规划从北京到上海的路线"}'

# 后续对话（使用返回的 threadId 保持上下文）
curl -X POST http://localhost:4081/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "中途想在南京停留一天", "threadId": "上一步返回的 threadId"}'
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
    "threadId": "生成的会话 ID"
  }
}
```

#### 健康检查

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

## 配置说明

项目使用 Profile 机制管理配置，Redis 配置独立为 `application-redis.yml`。

### 主配置文件（application.yml）

```yaml
spring:
  profiles:
    include: redis  # 自动加载 application-redis.yml
  ai:
    dashscope:
      api-key: ENC(加密后的百炼 API Key)
      chat:
        options:
          model: qwen3.7-max
    mcp:
      client:
        enabled: true
        streamable-http:
          connections: {}  # 禁用自动配置，使用自定义 Transport
    alibaba:
      mcp:
        nacos:
          server-addr: 192.168.0.100:8848
          namespace: public
          username: nacos
          password: nacos
          register:
            enabled: true

server:
  port: 4081

# 高德地图 API Key（使用 Jasypt 加密）
amap:
  api:
    key: ENC(加密后的高德 API Key)

# Jasypt 加密配置
jasypt:
  encryptor:
    algorithm: PBEWITHHMACSHA512ANDAES_256
    iv-generator-classname: org.jasypt.iv.RandomIvGenerator
    password: ${JASYPT_ENCRYPTOR_PASSWORD}
```

### Redis 配置文件（application-redis.yml）

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

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

## Nacos Prompt 管理

Agent 提示词通过 Nacos AI 注册中心的 Prompt 管理功能集中维护，支持版本控制和标签管理。

| Prompt Key | 用途 |
|------------|------|
| `travel-agent-prompt` | 出行规划 Agent 提示词 |
| `supervisor-agent-prompt` | 主管 Agent 提示词 |

提示词内容使用 Markdown 格式，在 Nacos 控制台 **AI 注册中心 → Prompt 管理** 中编辑和发布。

## 许可证

MIT License
