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
