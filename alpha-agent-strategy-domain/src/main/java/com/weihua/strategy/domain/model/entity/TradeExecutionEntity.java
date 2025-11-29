package com.weihua.strategy.domain.model.entity;

import com.weihua.strategy.domain.model.valobj.ExecutionStatus;
import com.weihua.strategy.domain.model.valobj.TradeAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeExecutionEntity {
    private String instructionId;
    private TradeAction action;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal price;  // 执行价格
    private BigDecimal fee;    // 手续费
    private ExecutionStatus status;
    private String executionResultJson;
    private String errorMessage;
    private LocalDateTime executionTime;  // 执行时间
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
