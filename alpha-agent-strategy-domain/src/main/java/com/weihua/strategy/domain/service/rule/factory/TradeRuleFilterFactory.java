package com.weihua.strategy.domain.service.rule.factory;

import com.weihua.strategy.domain.model.aggregate.VirtualAccountAggregate;
import com.weihua.strategy.domain.model.entity.MarketSnapshotEntity;
import com.weihua.strategy.domain.model.entity.TradeInstruction;
import com.weihua.strategy.domain.service.rule.filter.ComplianceRuleFilter;
import com.weihua.strategy.domain.service.rule.filter.FundAvailabilityRuleFilter;
import com.weihua.strategy.domain.service.rule.filter.LeverageRuleFilter;
import com.weihua.strategy.domain.service.rule.filter.PositionCheckRuleFilter;
import com.weihua.strategy.domain.service.rule.filter.StopLossRuleFilter;
import com.weihua.types.design.framework.link.model2.chain.BusinessLinkedList;
import com.weihua.types.design.framework.link.model2.chain.LinkedList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 交易规则过滤器工厂
 */
@Service
public class TradeRuleFilterFactory {

    @Bean("tradeRuleFilter")
    public BusinessLinkedList<TradeInstruction, DynamicContext, Boolean> tradeRuleFilter(
            ComplianceRuleFilter complianceRuleFilter,
            PositionCheckRuleFilter positionCheckRuleFilter,
            LeverageRuleFilter leverageRuleFilter,
            FundAvailabilityRuleFilter fundAvailabilityRuleFilter,
            StopLossRuleFilter stopLossRuleFilter) {
        
        // 手动组装链条 (按执行顺序)
        // 1. Compliance: 快速拒绝不合规的指令
        // 2. Position: 检查持仓是否充足
        // 3. Leverage: 检查杠杆限制
        // 4. FundAvailability: 检查资金是否充足
        // 5. StopLoss: 最高优先级,触发时强制平仓
        BusinessLinkedList<TradeInstruction, DynamicContext, Boolean> link = 
                new BusinessLinkedList<>("tradeRuleFilter");
        
        link.add(complianceRuleFilter);
        link.add(positionCheckRuleFilter);
        link.add(leverageRuleFilter);
        link.add(fundAvailabilityRuleFilter);
        link.add(stopLossRuleFilter);
        
        return link;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DynamicContext extends com.weihua.types.design.framework.link.model2.DynamicContext {
        private VirtualAccountAggregate account;
        private MarketSnapshotEntity marketData;
        
        /**
         * 运行时余额 (随着买单的确认而减少)
         */
        private BigDecimal runningBalance;
        
        /**
         * 运行时持仓 (随着卖单的确认而减少)
         */
        private Map<String, BigDecimal> runningPositions;

        public static DynamicContext create(VirtualAccountAggregate account, MarketSnapshotEntity marketData) {
            DynamicContext context = DynamicContext.builder()
                    .account(account)
                    .marketData(marketData)
                    .runningBalance(account.getBalance())
                    .runningPositions(new HashMap<>())
                    .build();
            
            if (account.getPositions() != null) {
                account.getPositions().forEach((k, v) -> 
                    context.runningPositions.put(k, v.getQuantity())
                );
            }
            return context;
        }
    }
}
