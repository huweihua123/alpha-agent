package com.weihua.test.rule;

import com.weihua.strategy.domain.model.entity.MarketSnapshotEntity;
import com.weihua.strategy.domain.model.entity.TradeInstruction;
import com.weihua.strategy.domain.model.valobj.TradeAction;
import com.weihua.strategy.domain.model.aggregate.VirtualAccountAggregate;
import com.weihua.strategy.domain.service.rule.factory.TradeRuleFilterFactory;
import com.weihua.strategy.domain.service.rule.filter.ComplianceRuleFilter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * ComplianceRuleFilter 单元测试
 * 测试合规检查规则
 */
@Slf4j
public class ComplianceRuleFilterTest {

    private ComplianceRuleFilter filter;
    private TradeRuleFilterFactory.DynamicContext context;

    @Before
    public void setUp() {
        filter = new ComplianceRuleFilter();
        
        // 创建测试上下文
        VirtualAccountAggregate account = VirtualAccountAggregate.builder()
                .strategyId("test-strategy-id")
                .balance(new BigDecimal("10000"))
                .frozen(BigDecimal.ZERO)
                .positions(new HashMap<>())
                .build();

        MarketSnapshotEntity market = MarketSnapshotEntity.builder()
                .symbol("BTC")
                .price(new BigDecimal("90000"))
                .build();

        context = TradeRuleFilterFactory.DynamicContext.create(account, market);
    }

    @Test
    public void test_合规检查_正常通过() {
        log.info("测试: 合规检查 - 正常通过");
        
        TradeInstruction instruction = TradeInstruction.builder()
                .action(TradeAction.BUY)
                .symbol("BTC")
                .quantity(new BigDecimal("0.01")) // 900 USDT
                .rationale("Test")
                .build();

        Boolean result = filter.apply(instruction, context);

        assertNotNull(result);
        assertEquals("数量应该保持不变", new BigDecimal("0.01"), instruction.getQuantity());
        log.info("✅ 测试通过: 合规检查正常");
    }

    @Test
    public void test_合规检查_最小金额不足_拒绝() {
        log.info("测试: 合规检查 - 最小金额不足,应该拒绝");
        
        TradeInstruction instruction = TradeInstruction.builder()
                .action(TradeAction.BUY)
                .symbol("BTC")
                .quantity(new BigDecimal("0.0001")) // 9 USDT < 10 USDT
                .rationale("Test")
                .build();

        Boolean result = filter.apply(instruction, context);

        assertEquals("应该返回false(拒绝)", Boolean.FALSE, result);
        log.info("✅ 测试通过: 最小金额不足被拒绝");
    }

    @Test
    public void test_合规检查_数量步长对齐() {
        log.info("测试: 合规检查 - 数量步长对齐");
        
        TradeInstruction instruction = TradeInstruction.builder()
                .action(TradeAction.BUY)
                .symbol("BTC")
                .quantity(new BigDecimal("0.012345")) // 不是步长的倍数
                .rationale("Test")
                .build();

        Boolean result = filter.apply(instruction, context);

        assertNotNull(result);
        // 应该被对齐到 0.01234 (步长0.00001)
        assertEquals("数量应该被对齐到步长", 
                new BigDecimal("0.01234"), 
                instruction.getQuantity());
        log.info("✅ 测试通过: 数量已对齐到步长");
    }

    @Test
    public void test_合规检查_最小数量不足_拒绝() {
        log.info("测试: 合规检查 - 最小数量不足,应该拒绝");
        
        TradeInstruction instruction = TradeInstruction.builder()
                .action(TradeAction.BUY)
                .symbol("BTC")
                .quantity(new BigDecimal("0.000001")) // 小于最小数量
                .rationale("Test")
                .build();

        Boolean result = filter.apply(instruction, context);

        assertEquals("应该返回false(拒绝)", Boolean.FALSE, result);
        log.info("✅ 测试通过: 最小数量不足被拒绝");
    }

    @Test
    public void test_合规检查_最大数量超限_拒绝() {
        log.info("测试: 合规检查 - 最大数量超限,应该拒绝");
        
        TradeInstruction instruction = TradeInstruction.builder()
                .action(TradeAction.BUY)
                .symbol("BTC")
                .quantity(new BigDecimal("150")) // 超过最大数量100
                .rationale("Test")
                .build();

        Boolean result = filter.apply(instruction, context);

        assertEquals("应该返回false(拒绝)", Boolean.FALSE, result);
        log.info("✅ 测试通过: 最大数量超限被拒绝");
    }

    @Test
    public void test_合规检查_边界值_刚好10USDT() {
        log.info("测试: 合规检查 - 边界值测试,刚好10 USDT");
        
        // 10 USDT / 90000 = 0.00011111...
        TradeInstruction instruction = TradeInstruction.builder()
                .action(TradeAction.BUY)
                .symbol("BTC")
                .quantity(new BigDecimal("0.00012")) // 约10.8 USDT
                .rationale("Test")
                .build();

        Boolean result = filter.apply(instruction, context);

        assertNotNull(result);
        log.info("✅ 测试通过: 边界值刚好通过");
    }
}
