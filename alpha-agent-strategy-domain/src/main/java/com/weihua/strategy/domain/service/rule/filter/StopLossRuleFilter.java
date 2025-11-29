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

/**
 * 止损规则过滤器
 * 
 * 功能:
 * 1. 监控持仓的盈亏百分比
 * 2. 当亏损超过阈值时,触发强制平仓
 * 3. 当盈利超过阈值时,触发止盈平仓 (可选)
 * 
 * 止损/止盈是最高优先级的风控规则,一旦触发会强制执行
 */
@Component
public class StopLossRuleFilter implements ILogicHandler<TradeInstruction, TradeRuleFilterFactory.DynamicContext, Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(StopLossRuleFilter.class);

    // 止损阈值: -5% (亏损5%时触发)
    private static final BigDecimal STOP_LOSS_THRESHOLD = new BigDecimal("-0.05");
    
    // 止盈阈值: +20% (盈利20%时触发) - 可选
    private static final BigDecimal STOP_PROFIT_THRESHOLD = new BigDecimal("0.20");
    
    // 是否启用止盈
    private static final boolean ENABLE_STOP_PROFIT = false;

    @Override
    public Boolean apply(TradeInstruction instruction, TradeRuleFilterFactory.DynamicContext context) {
        VirtualAccountAggregate account = context.getAccount();
        String symbol = instruction.getSymbol();
        
        // 获取当前持仓
        VirtualAccountAggregate.Position position = account.getPositions().get(symbol);
        
        // 如果没有持仓,不需要检查止损
        if (position == null || position.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            return next(instruction, context);
        }

        // 获取当前市场价格
        BigDecimal currentPrice = context.getMarketData().getPrice();
        BigDecimal entryPrice = position.getAvgPrice();
        
        // 计算盈亏百分比
        // pnl% = (currentPrice - entryPrice) / entryPrice
        BigDecimal pnlPercent = currentPrice.subtract(entryPrice)
                .divide(entryPrice, 6, RoundingMode.HALF_UP);

        logger.debug("Stop loss check for {}: entry={}, current={}, pnl={}%", 
                symbol, entryPrice, currentPrice, pnlPercent.multiply(new BigDecimal("100")));

        // 检查止损
        if (pnlPercent.compareTo(STOP_LOSS_THRESHOLD) < 0) {
            logger.warn("🚨 STOP LOSS TRIGGERED for {}: pnl={}% < threshold={}%", 
                    symbol, 
                    pnlPercent.multiply(new BigDecimal("100")), 
                    STOP_LOSS_THRESHOLD.multiply(new BigDecimal("100")));

            // 强制平仓: 修改指令为卖出全部持仓
            instruction.setAction(TradeAction.SELL);
            instruction.setSymbol(symbol);
            instruction.setQuantity(position.getQuantity());
            instruction.setRationale("STOP LOSS TRIGGERED - Forced liquidation at " + 
                    pnlPercent.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "% loss");

            logger.info("Forced SELL instruction created: symbol={}, qty={}", 
                    symbol, position.getQuantity());

            // 止损是强制执行,跳过后续规则
            return stop(instruction, context, true);
        }

        // 检查止盈 (可选)
        if (ENABLE_STOP_PROFIT && pnlPercent.compareTo(STOP_PROFIT_THRESHOLD) > 0) {
            logger.info("🎉 STOP PROFIT TRIGGERED for {}: pnl={}% > threshold={}%", 
                    symbol, 
                    pnlPercent.multiply(new BigDecimal("100")), 
                    STOP_PROFIT_THRESHOLD.multiply(new BigDecimal("100")));

            // 强制平仓: 锁定利润
            instruction.setAction(TradeAction.SELL);
            instruction.setSymbol(symbol);
            instruction.setQuantity(position.getQuantity());
            instruction.setRationale("STOP PROFIT TRIGGERED - Profit taking at " + 
                    pnlPercent.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "% gain");

            logger.info("Forced SELL instruction created for profit taking: symbol={}, qty={}", 
                    symbol, position.getQuantity());

            // 止盈也是强制执行
            return stop(instruction, context, true);
        }

        return next(instruction, context);
    }
}
