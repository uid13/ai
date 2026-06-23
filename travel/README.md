# Travel Assistant - 出行助手

基于 Spring AI Alibaba AgentScope 的 Multi-Agent 出行助手应用。

## 技术栈

| 组件 | 版本 |
|------|------|
| Java | 21 |
| Spring Boot | 3.5.8 |
| Spring AI | 1.1.2 |
| Spring AI Alibaba | 1.1.2.2 |
| Gradle | 8.14.5 |
| 模型 | 通义千问 qwen3.7-max |

## 架构设计

```
┌─────────────────────────────────────────────────────────┐
│  Travel Assistant (出行助手)                              │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Supervisor Agent (主管)                          │   │
│  │  - 理解用户意图                                    │   │
│  │  - 分发任务给子 Agent                              │   │
│  └──────────────────┬───────────────────────────────┘   │
│                     │                                    │
│  ┌──────────────────▼───────────────────────────────┐   │
│  │  Travel Agent (子 Agent)                          │   │
│  │  - 路线规划、POI 搜索、天气查询                      │   │
│  │  - 调用高德 MCP 工具                               │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
         │
         │ 通过 Nacos MCP 管理发现
         ▼
─────────────────────────────────────────────────────────┐
│  高德地图 MCP Server                                      │
│  - 路线规划、POI 搜索、天气查询、地理编码等 15 个工具         │
└─────────────────────────────────────────────────────────┘
```

## 快速开始

### 环境要求

- JDK 21+
- Gradle 8.14.5 (已内置 Wrapper)
- 阿里云百炼 API Key（已配置）
- Nacos 3.2.2（用于 MCP 服务注册）

### 构建项目

```bash
./gradlew build
```

### 运行应用

```bash
./gradlew bootRun
```

应用启动后，访问 `http://localhost:4081`

### API 接口

#### 聊天接口

```bash
curl -X POST http://localhost:4081/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "帮我规划从北京到上海的路线"}'
```

#### 健康检查

```bash
curl http://localhost:4081/api/health
```

## 配置说明

主要配置项 (`application.yml`):

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}  # 百炼 API Key
      chat:
        options:
          model: qwen3.7-max         # 模型名称
    mcp:
      client:
        streamable-http:
          connections:
            amap:                    # 高德地图 MCP
              url: https://mcp.amap.com
              endpoint: /mcp?key=xxx
    alibaba:
      mcp:
        nacos:
          server-addr: 192.168.0.100:8848  # Nacos 地址
          register:
            enabled: true                  # 自动注册到 Nacos

server:
  port: 4081                         # 服务端口
```

## 项目结构

```
travel/
├── build.gradle                    # Gradle 构建配置
├── settings.gradle                 # Gradle 设置
├── src/
│   ├── main/
│   │   ├── java/com/uid13/travel/
│   │   │   ├── TravelApplication.java            # 主入口
│   │   │   ├── agent/
│   │   │   │   ├── SupervisorAgent.java          # 主管 Agent
│   │   │   │   └── TravelAgent.java              # 出行规划 Agent
│   │   │   ├── config/
│   │   │   │   └── McpToolConfig.java            # MCP 工具配置
│   │   │   └── controller/
│   │   │       └── TravelController.java         # REST 控制器
│   │   └── resources/
│   │       └── application.yml                   # 应用配置
│   └── test/
│       ── java/com/uid13/travel/
│           └── TravelApplicationTests.java       # 测试
└── README.md
```

## 许可证

Apache License 2.0
