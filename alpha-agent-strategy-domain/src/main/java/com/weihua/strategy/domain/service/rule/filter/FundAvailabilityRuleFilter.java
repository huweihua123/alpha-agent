package com.weihua.strategy.domain.service.rule.filter;

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
 * 资金可用性规则过滤器 (增强版)
 * 
 * 功能:
 * 1. 检查余额是否充足,不足则进行截断 (Clamping)
 * 2. 滑点缓冲 (Slippage Buffer): 使用更高的有效价格计算,预留25bps的滑点空间
 * 3. 分段逻辑 (Piecewise Logic): 区分"增仓"和"减仓"操作
 *    - 同向增仓: 严格检查购买力
 *    - 反向减仓: 允许2倍当前仓位 + 剩余购买力 (因为减仓会释放资金)
 */
@Component
public class FundAvailabilityRuleFilter implements ILogicHandler<TradeInstruction, TradeRuleFilterFactory.DynamicContext, Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(FundAvailabilityRuleFilter.class);

    // 滑点缓冲: 25个基点 (0.25%)
    private static final BigDecimal SLIPPAGE_BPS = new BigDecimal("25");
    private static final BigDecimal BPS_DIVISOR = new BigDecimal("10000");
    
    // 手续费缓冲: 0.1%
    private static final BigDecimal FEE_BUFFER = new BigDecimal("0.999");

    @Override
    public Boolean apply(TradeInstruction instruction, TradeRuleFilterFactory.DynamicContext context) {
        // 只检查买入操作
        if (instruction.getAction() != TradeAction.BUY) {
            return next(instruction, context);
        }

        String symbol = instruction.getSymbol();
        BigDecimal price = context.getMarketData().getPrice();
        BigDecimal quantity = instruction.getQuantity();
        BigDecimal balance = context.getRunningBalance();

        // 获取当前持仓 (用于分段逻辑)
        BigDecimal currentPosition = context.getRunningPositions()
                .getOrDefault(symbol, BigDecimal.ZERO);

        // 计算有效价格 (加入滑点缓冲)
        // effectivePrice = price * (1 + slippage_bps / 10000)
        BigDecimal slippageMultiplier = BigDecimal.ONE.add(SLIPPAGE_BPS.divide(BPS_DIVISOR, 6, RoundingMode.HALF_UP));
        BigDecimal effectivePrice = price.multiply(slippageMultiplier);
        
        logger.debug("Effective price for {} with slippage: {} -> {}", 
                symbol, price, effectivePrice);

        // 计算所需成本 (使用有效价格)
        BigDecimal requiredCost = quantity.multiply(effectivePrice);

        // 分段逻辑: 判断是否为"反向减仓"
        boolean isReduction = currentPosition.compareTo(BigDecimal.ZERO) < 0; // 当前是空头,买入是减仓
        
        BigDecimal maxAffordableQty;
        
        if (isReduction) {
            // 反向减仓: 允许更宽松的购买力
            // 允许数量 = 2倍当前仓位 + 剩余购买力
            BigDecimal currentAbsPosition = currentPosition.abs();
            BigDecimal allowedByPosition = currentAbsPosition.multiply(new BigDecimal("2"));
            
            // 剩余购买力可买数量
            BigDecimal safeBalance = balance.multiply(FEE_BUFFER);
            BigDecimal allowedByBalance = safeBalance.divide(effectivePrice, 8, RoundingMode.DOWN);
            
            maxAffordableQty = allowedByPosition.add(allowedByBalance);
            
            logger.debug("Reduction mode for {}: current_position={}, allowed_by_position={}, allowed_by_balance={}, total={}", 
                    symbol, currentPosition, allowedByPosition, allowedByBalance, maxAffordableQty);
        } else {
            // 同向增仓: 严格检查购买力
            BigDecimal safeBalance = balance.multiply(FEE_BUFFER);
            maxAffordableQty = safeBalance.divide(effectivePrice, 8, RoundingMode.DOWN);
            
            logger.debug("Increase mode for {}: balance={}, max_affordable={}", 
                    symbol, balance, maxAffordableQty);
        }

        // 检查余额是否充足
        if (balance.compareTo(requiredCost) < 0 || quantity.compareTo(maxAffordableQty) > 0) {
            logger.warn("Insufficient funds for BUY {}: required={}, available={}, max_affordable={}", 
                    symbol, requiredCost, balance, maxAffordableQty);
            
            if (maxAffordableQty.compareTo(BigDecimal.ZERO) <= 0) {
                logger.warn("Cannot afford any quantity. Dropping instruction.");
                return stop(instruction, context, false);
            }
            
            logger.info("Clamping quantity for {} from {} to {}", 
                    symbol, quantity, maxAffordableQty);
            
            // 修改指令
            instruction.setQuantity(maxAffordableQty);
            instruction.setRationale(instruction.getRationale() + 
                    " [Rule: Clamped due to insufficient funds with slippage buffer]");
            
            quantity = maxAffordableQty;
        }

        // 计算实际消耗 (使用有效价格)
        BigDecimal actualCost = quantity.multiply(effectivePrice);
        
        // 扣减运行时余额
        context.setRunningBalance(balance.subtract(actualCost));
        
        // 更新运行时持仓 (买入增加持仓)
        BigDecimal newPosition = currentPosition.add(quantity);
        context.getRunningPositions().put(symbol, newPosition);

        logger.debug("Fund check passed for BUY {}: qty={}, cost={}, remaining_balance={}", 
                symbol, quantity, actualCost, context.getRunningBalance());

        return next(instruction, context);
    }
}
