package com.weihua.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
@ConfigurationProperties(prefix = "agent.prompts")
@Data
public class StrategyAgentPromptConfig {
    private Resource strategyAgentInstruction;
}
