package com.weihua.strategy.domain.service.rule.filter;

import com.weihua.strategy.domain.model.entity.TradeInstruction;
import com.weihua.strategy.domain.service.rule.factory.TradeRuleFilterFactory;
import com.weihua.types.design.framework.link.model2.handler.ILogicHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 合规检查规则过滤器
 * 检查交易是否符合交易所规则:
 * 1. 最小交易金额 (min_notional)
 * 2. 数量步长 (quantity_step)
 * 3. 最小交易数量 (min_trade_qty)
 * 4. 最大单笔数量 (max_order_qty)
 */
@Component
public class ComplianceRuleFilter implements ILogicHandler<TradeInstruction, TradeRuleFilterFactory.DynamicContext, Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(ComplianceRuleFilter.class);

    // 配置参数 (可以从配置文件或数据库读取)
    private static final BigDecimal MIN_NOTIONAL = new BigDecimal("10.0"); // 最小交易金额 10 USDT
    private static final BigDecimal QUANTITY_STEP = new BigDecimal("0.00001"); // 数量步长
    private static final BigDecimal MIN_TRADE_QTY = new BigDecimal("0.00001"); // 最小交易数量
    private static final BigDecimal MAX_ORDER_QTY = new BigDecimal("100.0"); // 最大单笔数量

    @Override
    public Boolean apply(TradeInstruction instruction, TradeRuleFilterFactory.DynamicContext context) {
        BigDecimal price = context.getMarketData().getPrice();
        BigDecimal quantity = instruction.getQuantity();
        String symbol = instruction.getSymbol();

        // 1. 检查最大单笔数量
        if (quantity.compareTo(MAX_ORDER_QTY) > 0) {
            logger.warn("Order quantity exceeds max limit for {}: {} > {}", 
                    symbol, quantity, MAX_ORDER_QTY);
            return stop(instruction, context, false);
        }

        // 2. 对齐数量步长 (向下取整)
        if (QUANTITY_STEP.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal originalQty = quantity;
            // quantity = floor(quantity / step) * step
            quantity = quantity.divide(QUANTITY_STEP, 0, RoundingMode.DOWN)
                              .multiply(QUANTITY_STEP);
            
            if (!quantity.equals(originalQty)) {
                logger.info("Quantity aligned to step for {}: {} -> {}", 
                        symbol, originalQty, quantity);
                instruction.setQuantity(quantity);
            }
        }

        // 3. 检查最小交易数量
        if (quantity.compareTo(MIN_TRADE_QTY) < 0) {
            logger.warn("Quantity below minimum for {}: {} < {}", 
                    symbol, quantity, MIN_TRADE_QTY);
            return stop(instruction, context, false);
        }

        // 4. 检查最小交易金额
        BigDecimal notional = quantity.multiply(price);
        if (notional.compareTo(MIN_NOTIONAL) < 0) {
            logger.warn("Notional value below minimum for {}: {} USDT < {} USDT", 
                    symbol, notional, MIN_NOTIONAL);
            return stop(instruction, context, false);
        }

        logger.debug("Compliance check passed for {}: qty={}, notional={}", 
                symbol, quantity, notional);

        return next(instruction, context);
    }
}
