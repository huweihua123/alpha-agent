package com.weihua.infrastructure.dao.po;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 决策日志持久化对象
 */
@Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class DecisionLogPO {
    /** 主键ID */
    private Long id;
    /** 关联的策略实例ID */
    private Long strategyInstanceId;
    /** 决策循环ID(UUID) */
    private String cycleId;
    /** 发送给LLM的完整Prompt */
    private String promptSnapshot;
    /** LLM返回的原始JSON */
    private String llmResponseJson;
    /** LLM给出的决策理由 */
    private String rationale;
    /** 决策指令JSON */
    private String instructionsJson;
    /** 决策时间 */
    private LocalDateTime decisionTime;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
