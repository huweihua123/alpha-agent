package com.weihua.strategy.domain.service.rule.filter;

import com.weihua.strategy.domain.model.aggregate.VirtualAccountAggregate;
import com.weihua.strategy.domain.model.entity.TradeInstruction;
import com.weihua.strategy.domain.model.valobj.TradeAction;
import com.weihua.strategy.domain.service.rule.factory.TradeRuleFilterFactory;
import com.weihua.types.design.framework.link.model2.handler.ILogicHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 杠杆控制规则过滤器
 * 防止过度杠杆导致爆仓风险
 * 
 * 杠杆倍数 = 总持仓价值 / 账户权益
 * 
 * 如果当前杠杆 + 新增持仓后的杠杆 > 最大杠杆,则降低买入数量
 */
@Component
public class LeverageRuleFilter implements ILogicHandler<TradeInstruction, TradeRuleFilterFactory.DynamicContext, Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(LeverageRuleFilter.class);

    // 最大杠杆倍数 (可以从策略配置读取)
    private static final BigDecimal MAX_LEVERAGE = new BigDecimal("5.0"); // 5倍杠杆

    @Override
    public Boolean apply(TradeInstruction instruction, TradeRuleFilterFactory.DynamicContext context) {
        // 只检查买入操作 (买入会增加杠杆)
        if (instruction.getAction() != TradeAction.BUY) {
            return next(instruction, context);
        }

        VirtualAccountAggregate account = context.getAccount();
        BigDecimal equity = account.getBalance(); // 账户权益 (简化版,实际应该是 balance + unrealized PnL)
        
        if (equity.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Account equity is zero or negative: {}. Cannot calculate leverage.", equity);
            return stop(instruction, context, false);
        }

        // 计算当前总持仓价值
        BigDecimal currentPositionValue = calculateTotalPositionValue(
                context.getRunningPositions(), 
                context.getMarketData().getPrice()
        );

        // 计算新增持仓价值
        BigDecimal newPositionValue = instruction.getQuantity()
                .multiply(context.getMarketData().getPrice());

        // 计算新的总持仓价值
        BigDecimal projectedPositionValue = currentPositionValue.add(newPositionValue);

        // 计算预期杠杆
        BigDecimal projectedLeverage = projectedPositionValue.divide(equity, 4, RoundingMode.HALF_UP);

        logger.debug("Leverage check for {}: current={}, projected={}, max={}", 
                instruction.getSymbol(), 
                currentPositionValue.divide(equity, 2, RoundingMode.HALF_UP),
                projectedLeverage,
                MAX_LEVERAGE);

        // 检查是否超过最大杠杆
        if (projectedLeverage.compareTo(MAX_LEVERAGE) > 0) {
            logger.warn("Projected leverage {} exceeds max leverage {} for {}", 
                    projectedLeverage, MAX_LEVERAGE, instruction.getSymbol());

            // 计算最大允许的持仓价值
            BigDecimal maxAllowedPositionValue = equity.multiply(MAX_LEVERAGE);
            
            // 计算最大允许的新增价值
            BigDecimal maxNewPositionValue = maxAllowedPositionValue.subtract(currentPositionValue);
            
            if (maxNewPositionValue.compareTo(BigDecimal.ZERO) <= 0) {
                // 已经超过杠杆限制,拒绝买入
                logger.warn("Already at max leverage. Dropping BUY instruction for {}", 
                        instruction.getSymbol());
                return stop(instruction, context, false);
            }

            // 计算最大允许的买入数量
            BigDecimal maxQty = maxNewPositionValue.divide(
                    context.getMarketData().getPrice(), 
                    8, 
                    RoundingMode.DOWN
            );

            logger.info("Clamping BUY quantity due to leverage limit for {}: {} -> {}", 
                    instruction.getSymbol(), instruction.getQuantity(), maxQty);
            
            instruction.setQuantity(maxQty);
            instruction.setRationale(instruction.getRationale() + 
                    " [Rule: Clamped due to leverage limit]");
        }

        return next(instruction, context);
    }

    /**
     * 计算总持仓价值
     */
    private BigDecimal calculateTotalPositionValue(
            Map<String, BigDecimal> positions, 
            BigDecimal currentPrice) {
        
        BigDecimal total = BigDecimal.ZERO;
        
        for (Map.Entry<String, BigDecimal> entry : positions.entrySet()) {
            BigDecimal qty = entry.getValue();
            // 简化版: 假设所有持仓都用当前价格计算
            // 实际应该用各自的市场价格
            total = total.add(qty.multiply(currentPrice));
        }
        
        return total;
    }
}
