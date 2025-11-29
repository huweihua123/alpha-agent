package com.weihua.strategy.domain.service.rule.filter;

import com.weihua.strategy.domain.model.entity.TradeInstruction;
import com.weihua.strategy.domain.model.valobj.TradeAction;
import com.weihua.strategy.domain.service.rule.factory.TradeRuleFilterFactory;
import com.weihua.types.design.framework.link.model2.handler.ILogicHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 持仓检查规则过滤器
 * 检查卖出操作时持仓是否充足
 * 如果持仓不足,自动截断到当前持仓数量
 */
@Component
public class PositionCheckRuleFilter implements ILogicHandler<TradeInstruction, TradeRuleFilterFactory.DynamicContext, Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(PositionCheckRuleFilter.class);

    @Override
    public Boolean apply(TradeInstruction instruction, TradeRuleFilterFactory.DynamicContext context) {
        // 只检查卖出操作
        if (instruction.getAction() != TradeAction.SELL) {
            return next(instruction, context);
        }

        String symbol = instruction.getSymbol();
        BigDecimal requestedQty = instruction.getQuantity();
        
        // 获取运行时持仓 (考虑之前已确认的卖单)
        BigDecimal currentPosition = context.getRunningPositions()
                .getOrDefault(symbol, BigDecimal.ZERO);

        // 检查持仓是否充足
        if (currentPosition.compareTo(requestedQty) < 0) {
            logger.warn("Insufficient position for SELL {}: requested={}, available={}", 
                    symbol, requestedQty, currentPosition);
            
            if (currentPosition.compareTo(BigDecimal.ZERO) <= 0) {
                // 没有持仓,直接拒绝
                logger.warn("No position to sell for {}. Dropping instruction.", symbol);
                return stop(instruction, context, false);
            }
            
            // 截断到当前持仓
            logger.info("Clamping sell quantity for {} from {} to {}", 
                    symbol, requestedQty, currentPosition);
            instruction.setQuantity(currentPosition);
            instruction.setRationale(instruction.getRationale() + 
                    " [Rule: Clamped to available position]");
        }

        // 更新运行时持仓 (扣减卖出数量)
        BigDecimal newPosition = currentPosition.subtract(instruction.getQuantity());
        context.getRunningPositions().put(symbol, newPosition);
        
        logger.debug("Position check passed for SELL {}: qty={}, remaining={}", 
                symbol, instruction.getQuantity(), newPosition);

        return next(instruction, context);
    }
}
