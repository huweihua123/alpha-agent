package com.weihua.test.rule;

import com.weihua.strategy.domain.model.aggregate.VirtualAccountAggregate;
import com.weihua.strategy.domain.model.entity.MarketSnapshotEntity;
import com.weihua.strategy.domain.model.entity.TradeInstruction;
import com.weihua.strategy.domain.model.valobj.TradeAction;
import com.weihua.strategy.domain.service.rule.factory.TradeRuleFilterFactory;
import com.weihua.strategy.domain.service.rule.filter.PositionCheckRuleFilter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * PositionCheckRuleFilter 单元测试
 * 测试持仓检查规则
 */
@Slf4j
public class PositionCheckRuleFilterTest {

    private PositionCheckRuleFilter filter;
    private TradeRuleFilterFactory.DynamicContext context;

    @Before
    public void setUp() {
        filter = new PositionCheckRuleFilter();
        
        // 创建测试上下文 - 有持仓
        VirtualAccountAggregate account = VirtualAccountAggregate.builder()
                .strategyId("test-strategy-id")
                .balance(new BigDecimal("10000"))
                .frozen(BigDecimal.ZERO)
                .positions(new HashMap<>())
                .build();

        // 添加持仓
        VirtualAccountAggregate.Position position = VirtualAccountAggregate.Position.builder()
                .symbol("BTC")
                .quantity(new BigDecimal("0.5"))
                .avgPrice(new BigDecimal("90000"))
                .build();
        account.getPositions().put("BTC", position);

        MarketSnapshotEntity market = MarketSnapshotEntity.builder()
                .symbol("BTC")
                .price(new BigDecimal("92000"))
                .build();

        context = TradeRuleFilterFactory.DynamicContext.create(account, market);
    }

    @Test
    public void test_持仓检查_买入操作_跳过检查() {
        log.info("测试: 持仓检查 - 买入操作应该跳过检查");
        
        TradeInstruction instruction = TradeInstruction.builder()
                .action(TradeAction.BUY)
                .symbol("BTC")
                .quantity(new BigDecimal("0.1"))
                .rationale("Test")
                .build();

        Boolean result = filter.apply(instruction, context);

        assertNotNull(result);
        assertEquals("买入数量应该保持不变", new BigDecimal("0.1"), instruction.getQuantity());
        log.info("✅ 测试通过: 买入操作跳过持仓检查");
    }

    @Test
    public void test_持仓检查_卖出正常_持仓充足() {
        log.info("测试: 持仓检查 - 卖出正常,持仓充足");
        
        TradeInstruction instruction = TradeInstruction.builder()
                .action(TradeAction.SELL)
                .symbol("BTC")
                .quantity(new BigDecimal("0.3")) // 小于持仓0.5
                .rationale("Test")
                .build();

        Boolean result = filter.apply(instruction, context);

        assertNotNull(result);
        assertEquals("卖出数量应该保持不变", new BigDecimal("0.3"), instruction.getQuantity());
        
        // 检查运行时持仓已更新
        BigDecimal remainingPosition = context.getRunningPositions().get("BTC");
        assertEquals("运行时持仓应该减少", new BigDecimal("0.2"), remainingPosition);
        
        log.info("✅ 测试通过: 持仓充足,卖出正常");
    }

    @Test
    public void test_持仓检查_卖出超限_自动截断() {
        log.info("测试: 持仓检查 - 卖出超限,应该自动截断");
        
        TradeInstruction instruction = TradeInstruction.builder()
                .action(TradeAction.SELL)
                .symbol("BTC")
                .quantity(new BigDecimal("1.0")) // 大于持仓0.5
                .rationale("Test")
                .build();

        Boolean result = filter.apply(instruction, context);

        assertNotNull(result);
        assertEquals("卖出数量应该被截断到持仓", new BigDecimal("0.5"), instruction.getQuantity());
        assertTrue("理由中应该包含截断说明", 
                instruction.getRationale().contains("Clamped to available position"));
        
        log.info("✅ 测试通过: 卖出超限被截断");
    }

    @Test
    public void test_持仓检查_无持仓_拒绝卖出() {
        log.info("测试: 持仓检查 - 无持仓,应该拒绝卖出");
        
        TradeInstruction instruction = TradeInstruction.builder()
                .action(TradeAction.SELL)
                .symbol("ETH") // 没有ETH持仓
                .quantity(new BigDecimal("1.0"))
                .rationale("Test")
                .build();

        Boolean result = filter.apply(instruction, context);

        assertEquals("应该返回false(拒绝)", Boolean.FALSE, result);
        log.info("✅ 测试通过: 无持仓拒绝卖出");
    }

    @Test
    public void test_持仓检查_多次卖出_运行时持仓追踪() {
        log.info("测试: 持仓检查 - 多次卖出,运行时持仓追踪");
        
        // 第一次卖出
        TradeInstruction instruction1 = TradeInstruction.builder()
                .action(TradeAction.SELL)
                .symbol("BTC")
                .quantity(new BigDecimal("0.2"))
                .rationale("First sell")
                .build();

        filter.apply(instruction1, context);
        
        // 第二次卖出
        TradeInstruction instruction2 = TradeInstruction.builder()
                .action(TradeAction.SELL)
                .symbol("BTC")
                .quantity(new BigDecimal("0.4")) // 应该被截断到0.3
                .rationale("Second sell")
                .build();

        Boolean result = filter.apply(instruction2, context);

        assertNotNull(result);
        assertEquals("第二次卖出应该被截断", new BigDecimal("0.3"), instruction2.getQuantity());
        
        // 检查运行时持仓
        BigDecimal remainingPosition = context.getRunningPositions().get("BTC");
        assertEquals("运行时持仓应该为0", BigDecimal.ZERO, remainingPosition);
        
        log.info("✅ 测试通过: 运行时持仓追踪正确");
    }

    @Test
    public void test_持仓检查_边界值_刚好卖完() {
        log.info("测试: 持仓检查 - 边界值,刚好卖完所有持仓");
        
        TradeInstruction instruction = TradeInstruction.builder()
                .action(TradeAction.SELL)
                .symbol("BTC")
                .quantity(new BigDecimal("0.5")) // 刚好等于持仓
                .rationale("Test")
                .build();

        Boolean result = filter.apply(instruction, context);

        assertNotNull(result);
        assertEquals("卖出数量应该保持不变", new BigDecimal("0.5"), instruction.getQuantity());
        
        // 检查运行时持仓
        BigDecimal remainingPosition = context.getRunningPositions().get("BTC");
        assertEquals("运行时持仓应该为0", BigDecimal.ZERO, remainingPosition);
        
        log.info("✅ 测试通过: 刚好卖完所有持仓");
    }
}
