package com.weihua.test.rule;

import com.weihua.strategy.domain.model.aggregate.VirtualAccountAggregate;
import com.weihua.strategy.domain.model.entity.MarketSnapshotEntity;
import com.weihua.strategy.domain.model.entity.TradeInstruction;
import com.weihua.strategy.domain.model.valobj.TradeAction;
import com.weihua.strategy.domain.service.rule.factory.TradeRuleFilterFactory;
import com.weihua.strategy.domain.service.rule.filter.FundAvailabilityRuleFilter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * FundAvailabilityRuleFilter (增强版) 单元测试
 * 覆盖资金检查、滑点缓冲、分段逻辑等场景
 */
@Slf4j
public class FundAvailabilityRuleFilterTest {

    private FundAvailabilityRuleFilter filter;
    private TradeRuleFilterFactory.DynamicContext context;

    @Before
    public void setUp() {
        filter = new FundAvailabilityRuleFilter();
    }

    /**
     * 创建上下文, 可自定义余额和当前持仓
     */
    private TradeRuleFilterFactory.DynamicContext createContext(BigDecimal balance, BigDecimal positionQty) {
        VirtualAccountAggregate account = VirtualAccountAggregate.builder()
                .strategyId("test-strategy-id")
                .balance(balance)
                .frozen(BigDecimal.ZERO)
                .positions(new HashMap<>())
                .build();
        if (positionQty != null) {
            VirtualAccountAggregate.Position pos = VirtualAccountAggregate.Position.builder()
                    .symbol("BTC")
                    .quantity(positionQty)
                    .avgPrice(new BigDecimal("90000"))
                    .build();
            account.getPositions().put("BTC", pos);
        }
        MarketSnapshotEntity market = MarketSnapshotEntity.builder()
                .symbol("BTC")
                .price(new BigDecimal("90000"))
                .build();
        return TradeRuleFilterFactory.DynamicContext.create(account, market);
    }

    @Test
    public void test_资金检查_余额充足_直接通过() {
        log.info("测试: 余额充足,买入直接通过");
        context = createContext(new BigDecimal("10000"), BigDecimal.ZERO);
        TradeInstruction instruction = TradeInstruction.builder()
                .action(TradeAction.BUY)
                .symbol("BTC")
                .quantity(new BigDecimal("0.01")) // 900 USDT
                .rationale("Test")
                .build();
        Boolean result = filter.apply(instruction, context);
        assertNotNull(result);
        assertEquals("数量不应被修改", new BigDecimal("0.01"), instruction.getQuantity());
        log.info("✅ 通过");
    }

    @Test
    public void test_资金检查_余额不足_应截断数量() {
        log.info("测试: 余额不足,应截断到最大可买数量");
        // 余额 500 USDT, 价格 90000, 有效价格会加 0.25% 滑点 => 90225
        context = createContext(new BigDecimal("500"), BigDecimal.ZERO);
        TradeInstruction instruction = TradeInstruction.builder()
                .action(TradeAction.BUY)
                .symbol("BTC")
                .quantity(new BigDecimal("0.01")) // 需要 902.25 USDT > 500
                .rationale("Test")
                .build();
        Boolean result = filter.apply(instruction, context);
        assertNotNull(result);
        // 计算期望最大数量: balance * 0.999 / effectivePrice
        BigDecimal effectivePrice = new BigDecimal("90000").multiply(
                BigDecimal.ONE.add(new BigDecimal("25").divide(new BigDecimal("10000")))); // 90225
        BigDecimal expectedQty = new BigDecimal("500").multiply(new BigDecimal("0.999"))
                .divide(effectivePrice, 8, BigDecimal.ROUND_DOWN);
        assertEquals("数量应被截断到最大可买", expectedQty, instruction.getQuantity());
        log.info("✅ 截断成功, qty={}", instruction.getQuantity());
    }

    @Test
    public void test_资金检查_分段逻辑_反向减仓_允许更宽松购买力() {
        log.info("测试: 反向减仓(空头)时,允许更宽松的购买力");
        // 当前持仓为 -0.2 (空头), 余额 1000 USDT
        context = createContext(new BigDecimal("1000"), new BigDecimal("-0.2"));
        TradeInstruction instruction = TradeInstruction.builder()
                .action(TradeAction.BUY)
                .symbol("BTC")
                .quantity(new BigDecimal("0.5")) // 大量买入, 需要检查分段逻辑
                .rationale("Test")
                .build();
        Boolean result = filter.apply(instruction, context);
        assertNotNull(result);
        // 计算期望最大数量: 2*|position| + balance*0.999/effectivePrice
        BigDecimal effectivePrice = new BigDecimal("90000").multiply(
                BigDecimal.ONE.add(new BigDecimal("25").divide(new BigDecimal("10000")))); // 90225
        BigDecimal allowedByPosition = new BigDecimal("0.2").multiply(new BigDecimal("2")); // 0.4
        BigDecimal allowedByBalance = new BigDecimal("1000").multiply(new BigDecimal("0.999"))
                .divide(effectivePrice, 8, BigDecimal.ROUND_DOWN);
        BigDecimal expectedMax = allowedByPosition.add(allowedByBalance);
        assertTrue("买入数量应不超过分段计算的上限", instruction.getQuantity().compareTo(expectedMax) <= 0);
        log.info("✅ 反向减仓逻辑通过, qty={}, maxAllowed={}", instruction.getQuantity(), expectedMax);
    }

    @Test
    public void test_资金检查_账户余额为零_直接拒绝() {
        log.info("测试: 账户余额为0,应直接拒绝买入");
        context = createContext(BigDecimal.ZERO, BigDecimal.ZERO);
        TradeInstruction instruction = TradeInstruction.builder()
                .action(TradeAction.BUY)
                .symbol("BTC")
                .quantity(new BigDecimal("0.01"))
                .rationale("Test")
                .build();
        Boolean result = filter.apply(instruction, context);
        assertEquals("应返回false", Boolean.FALSE, result);
        log.info("✅ 正确拒绝");
    }

    @Test
    public void test_资金检查_最大可买为0_直接拒绝() {
        log.info("测试: 计算出的最大可买数量为0,应直接拒绝");
        // 余额极小,导致 maxAffordableQty 为 0
        context = createContext(new BigDecimal("0.001"), BigDecimal.ZERO);
        TradeInstruction instruction = TradeInstruction.builder()
                .action(TradeAction.BUY)
                .symbol("BTC")
                .quantity(new BigDecimal("0.00001"))
                .rationale("Test")
                .build();
        Boolean result = filter.apply(instruction, context);
        assertEquals("应返回false", Boolean.FALSE, result);
        log.info("✅ 正确拒绝");
    }
}
