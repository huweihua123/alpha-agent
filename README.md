# AlphaAgent Strategy (策略智能体)

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-Alibaba-green)](https://github.com/alibaba/spring-ai-alibaba)
[![DDD](https://img.shields.io/badge/Architecture-DDD-orange)](https://en.wikipedia.org/wiki/Domain-driven_design)

**AlphaAgent Strategy** 是 [AlphaAgent](https://github.com/huweihua123/alpha-agent) 智能交易系统中的核心子服务，专注于**量化策略的生成、执行与风控**。

它采用标准的 **DDD (领域驱动设计)** 架构，结合 **Spring AI Alibaba** 和 **Reactive 响应式编程**，旨在为企业级 AI 交易提供高性能、高可维护性的解决方案。

---

## 🌟 核心特性

*   **🧠 智能决策引擎**: 基于 LLM (大语言模型) 动态生成交易计划，支持自然语言策略描述。
*   **🛡️ 严谨的风控体系**: 内置多层风控过滤器（资金检查、持仓限制、风险等级评估），确保 AI 决策的安全性。
*   **🏗️ 标准 DDD 架构**: 清晰划分 Application、Domain、Infrastructure 层，业务逻辑纯净，易于扩展。
*   **⚡ 响应式流处理**: 使用 Project Reactor 实现全链路异步非阻塞处理，支持高并发交易循环。
*   **🔌 开放生态**: 无缝对接 [Stock-MCP](https://github.com/huweihua123/stock-mcp) 工具集，获取实时行情与技术指标。

---

## 🏛️ 架构设计 (Architecture)

本项目严格遵循 **领域驱动设计 (DDD)** 原则：

### 分层视图

*   **Application Layer (应用层)**: `TradingCycleAppService`
    *   负责编排交易循环流程，协调领域服务，不包含业务规则。
*   **Domain Layer (领域层)**: 核心业务逻辑
    *   **Aggregates**: `StrategyInstance` (策略实例), `VirtualAccount` (虚拟账户), `TradingCycle` (交易周期)。
    *   **Domain Services**:
        *   `MarketAnalysisDomainService`: 行情分析与上下文准备。
        *   `StrategyComposerDomainService`: 策略决策（LLM + 风控）。
        *   `TradeExecutionDomainService`: 交易执行与资金结算。
*   **Infrastructure Layer (基础设施层)**:
    *   实现 Repository 接口 (MySQL/MyBatis)。
    *   实现 Port 接口 (LLM Client, Market Data Client)。
*   **Interface Layer (接口层)**:
    *   REST Controller, 定时任务调度。

---

## 🛠️ 技术栈 (Tech Stack)

*   **核心框架**: Spring Boot 3.5+, Spring Cloud Alibaba 2025.x
*   **AI 框架**: **Spring AI Alibaba** (接入通义千问等大模型)
*   **编程范式**: Reactive Programming (Project Reactor, WebFlux)
*   **数据库**: MySQL 8.0, MyBatis-Plus
*   **工具协议**: **MCP (Model Context Protocol)** - 用于对接 Python 数据服务
*   **配置中心**: Nacos (可选)

---

## 🔌 与 Stock-MCP 对接

本项目依赖 [Stock-MCP](https://github.com/huweihua123/stock-mcp) 提供底层数据支持。Stock-MCP 是一个基于 MCP 协议的 Python 服务，封装了 Pandas/TA-Lib 等强大的数据分析库。

### 集成方式
Strategy Agent 通过 **Spring AI MCP Client** 与 Stock-MCP 进行通信：
1.  **行情获取**: 调用 Stock-MCP 的 `fetch_prices` 工具获取实时/历史 K 线。
2.  **指标计算**: 调用 Stock-MCP 的 `calculate_indicators` 工具计算 MACD, RSI, Bollinger Bands 等技术指标。

### 部署建议
建议将 Stock-MCP 作为 Sidecar 或独立微服务部署，并在 `application.yml` 中配置连接地址。

---

## 🚀 快速开始

### 前置要求
*   JDK 21+
*   Maven 3.8+
*   MySQL 8.0
*   [Stock-MCP](https://github.com/huweihua123/stock-mcp) 服务已启动

### 1. 数据库初始化
SQL 脚本位于: `docs/dev-ops/mysql/sql/alpha-strategy.sql`
请在 MySQL 中执行该脚本以初始化表结构。

### 2. 配置修改
修改 `alpha-agent-strategy-app/src/main/resources/application-dev.yml`，配置数据库连接和 LLM API Key。
**注意**: 敏感信息（如 API Key、数据库密码）建议通过环境变量注入，不要直接提交到代码仓库。

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
```

### 3. 编译与运行
```bash
mvn clean install
java -jar alpha-agent-strategy-app/target/alpha-agent-strategy-app.jar
```

---

## 🤝 贡献

欢迎提交 Issue 和 PR！

## 📄 License

Apache License 2.0
