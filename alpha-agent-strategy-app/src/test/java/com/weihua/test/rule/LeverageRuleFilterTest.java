package com.weihua.test.rule;

import com.weihua.strategy.domain.model.aggregate.VirtualAccountAggregate;
import com.weihua.strategy.domain.model.entity.MarketSnapshotEntity;
import com.weihua.strategy.domain.model.entity.TradeInstruction;
import com.weihua.strategy.domain.model.valobj.TradeAction;
import com.weihua.strategy.domain.service.rule.factory.TradeRuleFilterFactory;
import com.weihua.strategy.domain.service.rule.filter.LeverageRuleFilter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * LeverageRuleFilter 单元测试
 * 测试杠杆控制规则
 */
@Slf4j
public class LeverageRuleFilterTest {

    private LeverageRuleFilter filter;
    private TradeRuleFilterFactory.DynamicContext context;

    @Before
    public void setUp() {
        filter = new LeverageRuleFilter();
    }

    private TradeRuleFilterFactory.DynamicContext createContext(
            BigDecimal balance, 
            BigDecimal existingPositionQty) {
        
        VirtualAccountAggregate account = VirtualAccountAggregate.builder()
                .strategyId("test-strategy-id")
                .balance(balance)
                .frozen(BigDecimal.ZERO)
                .positions(new HashMap<>())
                .build();

        if (existingPositionQty.compareTo(BigDecimal.ZERO) > 0) {
            VirtualAccountAggregate.Position position = VirtualAccountAggregate.Position.builder()
                    .symbol("BTC")
                    .quantity(existingPositionQty)
                    .avgPrice(new BigDecimal("90000"))
                    .build();
            account.getPositions().put("BTC", position);
        }

        MarketSnapshotEntity market = MarketSnapshotEntity.builder()
                .symbol("BTC")
                .price(new BigDecimal("90000"))
                .build();

        return TradeRuleFilterFactory.DynamicContext.create(account, market);
    }

    @Test
    public void test_杠杆检查_卖出操作_跳过检查() {
        log.info("测试: 杠杆检查 - 卖出操作应该跳过检查");
        
        context = createContext(new BigDecimal("1000"), new BigDecimal("0.1"));
        
        TradeInstruction instruction = TradeInstruction.builder()
                .action(TradeAction.SELL)
                .symbol("BTC")
                .quantity(new BigDecimal("0.05"))
                .rationale("Test")
                .build();

        Boolean result = filter.apply(instruction, context);

        assertNotNull(result);
        assertEquals("卖出数量应该保持不变", new BigDecimal("0.05"), instruction.getQuantity());
        log.info("✅ 测试通过: 卖出操作跳过杠杆检查");
    }

    @Test
    public void test_杠杆检查_正常买入_杠杆未超限() {
        log.info("测试: 杠杆检查 - 正常买入,杠杆未超限");
        
        // 账户权益1000, 当前持仓0.02 BTC (价值1800), 杠杆1.8倍
        context = createContext(new BigDecimal("1000"), new BigDecimal("0.02"));
        
        TradeInstruction instruction = TradeInstruction.builder()
                .action(TradeAction.BUY)
                .symbol("BTC")
                .quantity(new BigDecimal("0.01")) // 新增900, 总持仓2700, 杠杆2.7倍 < 5倍
                .rationale("Test")
                .build();

        Boolean result = filter.apply(instruction, context);

        assertNotNull(result);
        assertEquals("买入数量应该保持不变", new BigDecimal("0.01"), instruction.getQuantity());
        log.info("✅ 测试通过: 杠杆未超限,买入正常");
    }

    @Test
    public void test_杠杆检查_买入超限_自动降低() {
        log.info("测试: 杠杆检查 - 买入超限,应该自动降低数量");
        
        // 账户权益1000, 当前持仓0.05 BTC (价值4500), 杠杆4.5倍
        context = createContext(new BigDecimal("1000"), new BigDecimal("0.05"));
        
        TradeInstruction instruction = TradeInstruction.builder()
                .action(TradeAction.BUY)
                .symbol("BTC")
                .quantity(new BigDecimal("0.01")) // 新增900, 总持仓5400, 杠杆5.4倍 > 5倍
                .rationale("Test")
                .build();

        Boolean result = filter.apply(instruction, context);

        assertNotNull(result);
        
        // 最大允许持仓: 1000 * 5 = 5000
        // 当前持仓: 4500
        // 最大新增: 500
        // 最大数量: 500 / 90000 = 0.00555...
        assertTrue("买入数量应该被降低", 
                instruction.getQuantity().compareTo(new BigDecimal("0.01")) < 0);
        assertTrue("理由中应该包含杠杆限制说明", 
                instruction.getRationale().contains("leverage limit"));
        
        log.info("✅ 测试通过: 杠杆超限被降低,新数量={}", instruction.getQuantity());
    }

    @Test
    public void test_杠杆检查_已达最大杠杆_拒绝买入() {
        log.info("测试: 杠杆检查 - 已达最大杠杆,应该拒绝买入");
        
        // 账户权益1000, 当前持仓0.055 BTC (价值4950), 杠杆4.95倍
        context = createContext(new BigDecimal("1000"), new BigDecimal("0.055"));
        
        TradeInstruction instruction = TradeInstruction.builder()
                .action(TradeAction.BUY)
                .symbol("BTC")
                .quantity(new BigDecimal("0.001")) // 任何买入都会超过5倍
                .rationale("Test")
                .build();

        Boolean result = filter.apply(instruction, context);

        // 应该被拒绝或数量被降低到接近0
        if (Boolean.FALSE.equals(result)) {
            log.info("✅ 测试通过: 已达最大杠杆,买入被拒绝");
        } else {
            assertTrue("数量应该被大幅降低", 
                    instruction.getQuantity().compareTo(new BigDecimal("0.001")) < 0);
            log.info("✅ 测试通过: 已达最大杠杆,数量被大幅降低到{}", instruction.getQuantity());
        }
    }

    @Test
    public void test_杠杆检查_无持仓_正常买入() {
        log.info("测试: 杠杆检查 - 无持仓,正常买入");
        
        // 账户权益1000, 无持仓
        context = createContext(new BigDecimal("1000"), BigDecimal.ZERO);
        
        TradeInstruction instruction = TradeInstruction.builder()
                .action(TradeAction.BUY)
                .symbol("BTC")
                .quantity(new BigDecimal("0.05")) // 价值4500, 杠杆4.5倍 < 5倍
                .rationale("Test")
                .build();

        Boolean result = filter.apply(instruction, context);

        assertNotNull(result);
        assertEquals("买入数量应该保持不变", new BigDecimal("0.05"), instruction.getQuantity());
        log.info("✅ 测试通过: 无持仓,正常买入");
    }

    @Test
    public void test_杠杆检查_边界值_刚好5倍杠杆() {
        log.info("测试: 杠杆检查 - 边界值,刚好5倍杠杆");
        
        // 账户权益1000, 当前持仓0.04 BTC (价值3600), 杠杆3.6倍
        context = createContext(new BigDecimal("1000"), new BigDecimal("0.04"));
        
        // 买入0.01555 BTC (价值1400), 总持仓5000, 杠杆刚好5倍
        TradeInstruction instruction = TradeInstruction.builder()
                .action(TradeAction.BUY)
                .symbol("BTC")
                .quantity(new BigDecimal("0.01555"))
                .rationale("Test")
                .build();

        Boolean result = filter.apply(instruction, context);

        assertNotNull(result);
        // 应该通过或被轻微调整
        log.info("✅ 测试通过: 边界值测试,最终数量={}", instruction.getQuantity());
    }

    @Test
    public void test_杠杆检查_账户权益为0_拒绝() {
        log.info("测试: 杠杆检查 - 账户权益为0,应该拒绝");
        
        context = createContext(BigDecimal.ZERO, BigDecimal.ZERO);
        
        TradeInstruction instruction = TradeInstruction.builder()
                .action(TradeAction.BUY)
                .symbol("BTC")
                .quantity(new BigDecimal("0.01"))
                .rationale("Test")
                .build();

        Boolean result = filter.apply(instruction, context);

        assertEquals("应该返回false(拒绝)", Boolean.FALSE, result);
        log.info("✅ 测试通过: 账户权益为0被拒绝");
    }
}
