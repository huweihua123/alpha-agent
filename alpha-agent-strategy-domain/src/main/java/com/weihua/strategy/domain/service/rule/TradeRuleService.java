package com.weihua.strategy.domain.service.rule;

import com.weihua.strategy.domain.model.aggregate.VirtualAccountAggregate;
import com.weihua.strategy.domain.model.entity.DecisionEntity;
import com.weihua.strategy.domain.model.entity.MarketSnapshotEntity;
import com.weihua.strategy.domain.model.entity.TradeInstruction;
import com.weihua.strategy.domain.model.entity.TradePlan;
import com.weihua.strategy.domain.service.rule.factory.TradeRuleFilterFactory;
import com.weihua.types.design.framework.link.model2.chain.BusinessLinkedList;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 交易规则领域服务
 * 负责执行交易规则过滤链，将原始计划转化为可执行决策
 */
@Service
public class TradeRuleService {

    private static final Logger logger = LoggerFactory.getLogger(TradeRuleService.class);

    @Resource(name = "tradeRuleFilter")
    private BusinessLinkedList<TradeInstruction, TradeRuleFilterFactory.DynamicContext, Boolean> tradeRuleFilter;

    /**
     * 过滤并规范化交易计划
     */
    public DecisionEntity filter(TradePlan plan, VirtualAccountAggregate account, MarketSnapshotEntity market) {
        TradeRuleFilterFactory.DynamicContext context = TradeRuleFilterFactory.DynamicContext.create(account, market);
        List<TradeInstruction> validInstructions = new ArrayList<>();

        if (plan.getInstructions() != null) {
            for (TradeInstruction instruction : plan.getInstructions()) {
                try {
                    // 默认通过
                    Boolean passed = true;
                    
                    // 执行责任链
                    // 如果 Handler 调用 next() -> proceed=true, return null -> 循环继续 -> 最后返回 null
                    // 如果 Handler 调用 stop() -> proceed=false, return result (false) -> 循环结束 -> 返回 false
                    Object result = tradeRuleFilter.apply(instruction, context);
                    
                    // 如果 result 是 Boolean.FALSE，说明被拦截
                    if (Boolean.FALSE.equals(result)) {
                        passed = false;
                    }
                    
                    if (passed) {
                        validInstructions.add(instruction);
                    }
                } catch (Exception e) {
                    logger.error("Trade rule filter failed for instruction: {}", instruction, e);
                    // 异常情况下，保守起见，丢弃该指令
                }
            }
        }

        return DecisionEntity.builder()
                .rationale(plan.getRationale())
                .instructions(validInstructions)
                .rawPlan(plan)
                .build();
    }
}
