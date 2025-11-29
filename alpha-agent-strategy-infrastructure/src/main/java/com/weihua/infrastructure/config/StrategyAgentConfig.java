package com.weihua.infrastructure.config;

import com.alibaba.cloud.ai.agent.nacos.NacosAgentPromptBuilder;
import com.alibaba.cloud.ai.agent.nacos.NacosOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class StrategyAgentConfig {
    private static final Logger logger = LoggerFactory.getLogger(StrategyAgentConfig.class);

    @Autowired(required = false)
    private StrategyAgentPromptConfig promptConfig;

    @Autowired(required = false)
    private NacosOptions nacosOptions;

    @Bean
    public ReactAgent strategyAgentBean(ChatModel chatModel,
                                        // 1. 注入标准 MCP 客户端工具提供者 (对应 application.yml 中的 spring.ai.mcp.client)
                                        @Autowired(required = false) @Qualifier("mcpToolCallbacks") ToolCallbackProvider toolsProvider,

                                        // 2. 注入 Nacos MCP 客户端工具提供者 (对应 spring.ai.alibaba.mcp)
                                        @Autowired(required = false) @Qualifier("distributedAsyncToolCallback") ToolCallbackProvider nacosToolsProvider)
            throws Exception {

        List<ToolCallback> tools = new ArrayList<>();

        // --- 处理标准直连 MCP 工具 (例如 FastMCP Python 服务) ---
        if (toolsProvider != null) {
            ToolCallback[] callbacks = toolsProvider.getToolCallbacks();
            logger.info("🔌 Standard MCP Client ready, found {} tools from SSE.", callbacks.length);

            for (ToolCallback toolCallback : callbacks) {
                String toolName = toolCallback.getToolDefinition().name();
                // 策略 Agent 需要交易和行情相关的工具
                if (toolName.contains("trade") || toolName.contains("asset") || toolName.contains("price")) {
                    logger.info("✅ strategy_agent add tool from SSE: {}", toolName);
                    tools.add(toolCallback);
                }
            }
        } else {
            logger.warn("⚠️ Standard MCP Client (mcpToolCallbacks) is null. No direct SSE MCP tools available.");
        }

        // --- 处理 Nacos 发现的 MCP 工具 (如果有) ---
        if (nacosToolsProvider != null) {
            ToolCallback[] callbacks = nacosToolsProvider.getToolCallbacks();
            logger.info("🌐 Nacos MCP Client ready, found {} tools from Nacos.", callbacks.length);

            for (ToolCallback toolCallback : callbacks) {
                String toolName = toolCallback.getToolDefinition().name();
                // 也可以从 Nacos 发现其他服务提供的工具
                if (toolName.contains("trade") || toolName.contains("asset")) {
                    logger.info("✅ strategy_agent add tool from Nacos: {}", toolName);
                    tools.add(toolCallback);
                }
            }
        }

        logger.info("🚀 Creating Strategy Agent with {} tools.", tools.size());

        // --- 使用 Nacos 管理 Prompt (如果配置了 NacosOptions) ---
        if (nacosOptions != null && nacosOptions.getPromptKey() != null) {
            logger.info("📝 Using Nacos for Prompt management. PromptKey: {}", nacosOptions.getPromptKey());

            ReactAgent agent = new NacosAgentPromptBuilder()
                    .nacosOptions(nacosOptions)
                    .name("strategy_agent")
                    .description("策略交易智能体，负责根据市场数据和策略配置执行自动交易")
                    .model(chatModel)
                    .tools(tools)
                    .build();

            logger.info("✅ Strategy Agent Bean created with Nacos Prompt: {}", agent.name());
            return agent;
        }

        // --- 降级到本地文件 Prompt ---
        logger.warn("⚠️ NacosOptions not configured, falling back to local file prompt.");

        String instruction = "";
        if (promptConfig != null && promptConfig.getStrategyAgentInstruction() != null) {
            instruction = promptConfig.getStrategyAgentInstruction()
                    .getContentAsString(StandardCharsets.UTF_8);
            logger.info("📄 Loaded prompt from local file.");
        } else {
            logger.warn("⚠️ Strategy Agent instruction prompt is null! Using default.");
            instruction = "You are a helpful trading strategy assistant.";
        }

        ReactAgent agent = ReactAgent.builder()
                .name("strategy_agent")
                .model(chatModel)
                .description("策略交易智能体，负责根据市场数据和策略配置执行自动交易")
                .instruction(instruction)
                .tools(tools)
                .saver(new com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver())
                .build();

        logger.info("✅ Strategy Agent Bean created with local prompt: {}", agent.name());
        return agent;
    }
}
