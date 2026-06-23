# Travel Assistant - Agent 开发指南

## 项目概述

Travel Assistant 是一个基于 Spring AI Alibaba AgentScope 的 Multi-Agent 出行助手应用，提供路线规划、POI 搜索、天气查询等出行相关服务。

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 21 | LTS 版本 |
| Spring Boot | 3.5.8 | Web 框架 |
| Spring AI | 1.1.2 | AI 集成框架 |
| Spring AI Alibaba | 1.1.2.2 | 阿里云 AI 组件 |
| Gradle | 8.14.5 | 构建工具 |
| 模型 | qwen3.7-max | 通义千问大模型 |

## 架构设计

### Multi-Agent 架构

```
Supervisor Agent (主管)
    │
    ├── Travel Agent (出行规划)
    │   ├── 路线规划（驾车、骑行、步行、公交）
    │   ├── POI 搜索（地点搜索、周边搜索）
    │   ├── 天气查询
    │   ├── 地理编码
    │   └── 距离测量
    │
    └── [未来扩展] 其他子 Agent
        ├── 酒店预订 Agent
        ├── 机票查询 Agent
        └── ...
```

### 工具集成

- **高德地图 MCP**: 15 个地图服务工具
- **Nacos MCP 管理**: 服务发现与注册

## 开发规范

### 1. 代码规范

- 使用 Java 21 语法特性
- 遵循 Spring Boot 最佳实践
- 所有类添加 Javadoc 注释
- Agent 使用 `@Component` 注解管理

### 2. 添加新 Agent

#### 步骤 1: 创建 Agent 类

在 `com.uid13.travel.agent` 包下创建新 Agent：

```java
@Component
public class HotelAgent {
    
    private final ReactAgent agent;
    
    public HotelAgent(ChatModel chatModel) {
        this.agent = ReactAgent.builder()
                .name("hotel-agent")
                .model(chatModel)
                .systemPrompt("你是酒店预订专家...")
                .build();
    }
    
    public ReactAgent getAgent() {
        return agent;
    }
}
```

#### 步骤 2: 在 Supervisor 中注册

修改 `SupervisorAgent.java`，添加新 Agent 的描述和调用逻辑。

### 3. 配置说明

主要配置项位于 `application.yml`:

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `spring.ai.dashscope.api-key` | 百炼 API Key | - |
| `spring.ai.dashscope.chat.options.model` | 模型名称 | qwen3.7-max |
| `spring.ai.mcp.client.streamable-http.connections.amap.url` | 高德 MCP 地址 | https://mcp.amap.com |
| `spring.ai.alibaba.mcp.nacos.server-addr` | Nacos 地址 | 192.168.0.100:8848 |
| `server.port` | 服务端口 | 4081 |

### 4. 构建与测试

```bash
# 构建项目
./gradlew build

# 运行测试
./gradlew test

# 启动应用
./gradlew bootRun

# 清理构建
./gradlew clean
```

### 5. 依赖管理

项目使用 BOM 管理依赖版本：

- `spring-ai-bom:1.1.2`
- `spring-ai-alibaba-bom:1.1.2.2`

添加新依赖时，优先使用 BOM 中已定义的版本。

### 6. Maven 仓库配置

项目使用阿里云 Maven 镜像加速依赖下载：

- `https://maven.aliyun.com/repository/public/` - 公共仓库
- `https://maven.aliyun.com/repository/spring/` - Spring 仓库

## API 接口

### POST /api/chat

聊天接口，用户发送消息，Agent 处理并返回结果。

**请求体**:
```json
{
  "message": "帮我规划从北京到上海的路线"
}
```

**响应**:
```json
{
  "message": "从北京到上海的驾车路线...",
  "agent": "travel-supervisor"
}
```

### GET /api/health

健康检查接口。

**响应**:
```json
{
  "status": "UP",
  "service": "travel-assistant",
  "version": "1.0.0"
}
```

## 常见问题

### Q: 如何调试 Agent？

A: 在 Agent 的 systemPrompt 中添加调试指令，或使用 `@Slf4j` 记录日志。

### Q: 如何添加新的 MCP 工具？

A: 在 Nacos MCP 管理中注册新的 MCP Server，应用会自动发现并注入工具。

### Q: 如何切换模型？

A: 修改 `application.yml` 中的 `spring.ai.dashscope.chat.options.model` 配置项。

## 参考文档

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Spring AI Alibaba 文档](https://java2ai.com/)
- [AgentScope Java 文档](https://github.com/alibaba/spring-ai-alibaba)
- [高德地图 MCP 文档](https://lbs.amap.com/api/mcp-server)
- [Nacos 文档](https://nacos.io/docs/latest/overview/)
