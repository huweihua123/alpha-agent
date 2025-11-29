package com.weihua.strategy.domain.model.aggregate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 虚拟账户聚合根
 * 负责管理策略的资金和持仓，并执行模拟交易撮合
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirtualAccountAggregate {
    private String strategyId;
    private String currency; // e.g. USDT
    private BigDecimal balance;
    private BigDecimal frozen;
    private Map<String, Position> positions; // Symbol -> Position

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position {
        private String symbol;
        private BigDecimal quantity;
        private BigDecimal avgPrice;
    }

    /**
     * 模拟买入
     */
    public void buy(String symbol, BigDecimal quantity, BigDecimal price, BigDecimal fee) {
        BigDecimal cost = quantity.multiply(price);
        BigDecimal totalCost = cost.add(fee); // 成本 = 市值 + 手续费
        
        if (balance.compareTo(totalCost) < 0) {
            throw new IllegalStateException("Insufficient balance: " + balance + " < " + totalCost);
        }
        
        // 扣减余额
        this.balance = this.balance.subtract(totalCost);
        
        // 更新持仓
        Position position = positions.getOrDefault(symbol, Position.builder()
                .symbol(symbol)
                .quantity(BigDecimal.ZERO)
                .avgPrice(BigDecimal.ZERO)
                .build());
        
        // 计算新均价: (旧数量*旧均价 + 新数量*新价格 + 手续费) / (旧数量+新数量)
        // 注意：这里将买入手续费摊入持仓成本
        BigDecimal currentPositionCost = position.getQuantity().multiply(position.getAvgPrice());
        BigDecimal newTotalCost = currentPositionCost.add(cost).add(fee);
        BigDecimal newQuantity = position.getQuantity().add(quantity);
        BigDecimal newAvgPrice = newTotalCost.divide(newQuantity, 8, BigDecimal.ROUND_HALF_UP);
        
        position.setQuantity(newQuantity);
        position.setAvgPrice(newAvgPrice);
        
        positions.put(symbol, position);
    }

    /**
     * 模拟卖出
     */
    public void sell(String symbol, BigDecimal quantity, BigDecimal price, BigDecimal fee) {
        Position position = positions.get(symbol);
        if (position == null || position.getQuantity().compareTo(quantity) < 0) {
            throw new IllegalStateException("Insufficient position for " + symbol);
        }
        
        // 增加余额: 收入 = 市值 - 手续费
        BigDecimal revenue = quantity.multiply(price);
        BigDecimal netRevenue = revenue.subtract(fee);
        
        this.balance = this.balance.add(netRevenue);
        
        // 更新持仓数量
        BigDecimal newQuantity = position.getQuantity().subtract(quantity);
        position.setQuantity(newQuantity);
        
        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            positions.remove(symbol);
        }
    }
}
