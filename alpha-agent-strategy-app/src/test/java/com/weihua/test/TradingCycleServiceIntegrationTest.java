package com.weihua.test;

import com.weihua.strategy.domain.adapter.port.ILlmPort;
import com.weihua.strategy.domain.adapter.port.IMarketDataPort;
import com.weihua.strategy.domain.model.aggregate.StrategyInstanceAggregate;
import com.weihua.strategy.domain.model.aggregate.VirtualAccountAggregate;
import com.weihua.strategy.domain.model.entity.MarketSnapshotEntity;
import com.weihua.strategy.domain.model.entity.StrategyConfigEntity;
import com.weihua.strategy.domain.model.entity.TradeInstruction;
import com.weihua.strategy.domain.model.entity.TradePlan;
import com.weihua.strategy.domain.model.valobj.StrategyStatus;
import com.weihua.strategy.domain.model.valobj.TradeAction;
import com.weihua.strategy.domain.repository.ITradingCycleRepository;
import com.weihua.strategy.domain.repository.IVirtualAccountRepository;
import com.weihua.strategy.domain.repository.IStrategyInstanceRepository;
import com.weihua.strategy.application.service.TradingCycleAppService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 全流程集成测试 – 验证 TradingCycleAppService 能够完成一次完整的交易周期。
 * 包括：
 *   1. 加载策略实例
 *   2. 加载虚拟账户
 *   3. 获取市场数据 & 技术指标
 *   4. 调用 LLM 生成 TradePlan
 *   5. 通过风险控制过滤链 (已在 Spring 容器中自动装配)
 *   6. 执行指令、更新账户、保存历史记录
 */
@Slf4j
@SpringBootTest
public class TradingCycleServiceIntegrationTest {

    @Autowired
    private TradingCycleAppService tradingCycleAppService;

    @MockBean
    private IStrategyInstanceRepository strategyInstanceRepository;

    @MockBean
    private IVirtualAccountRepository virtualAccountRepository;

    @MockBean
    private IMarketDataPort marketDataPort;

    @MockBean
    private ILlmPort llmPort;

    @MockBean
    private ITradingCycleRepository tradingCycleRepository;

    private final String strategyId = "test-strategy-id";

    @BeforeEach
    public void setUp() {
        // 1️⃣ Mock StrategyInstanceAggregate
        StrategyInstanceAggregate strategy = StrategyInstanceAggregate.builder()
                .strategyId(strategyId)
                .status(StrategyStatus.RUNNING)
                .config(StrategyConfigEntity.builder()
                        .symbols(Collections.singletonList("BTC"))
                        .templateId("dummy")
                        .build())
                .build();
        when(strategyInstanceRepository.findByStrategyId(strategyId)).thenReturn(strategy);

        // 2️⃣ Mock VirtualAccountAggregate (初始余额 10,000 USDT, 无持仓)
        VirtualAccountAggregate account = VirtualAccountAggregate.builder()
                .strategyId(strategyId)
                .balance(new BigDecimal("10000"))
                .frozen(BigDecimal.ZERO)
                .positions(new HashMap<>())
                .build();
        when(virtualAccountRepository.findByStrategyId(strategyId, "USDT")).thenReturn(account);

        // 3️⃣ Mock Market Data
        MarketSnapshotEntity market = MarketSnapshotEntity.builder()
                .symbol("BTC")
                .price(new BigDecimal("90000"))
                .build();
        Map<String, MarketSnapshotEntity> marketMap = new HashMap<>();
        marketMap.put("BTC", market);
        when(marketDataPort.fetchLatestPrices(any())).thenReturn(marketMap);
        when(marketDataPort.fetchTechnicalIndicators("BTC")).thenReturn("{}");

        // 4️⃣ Mock LLM 返回的 TradePlan
        TradeInstruction buyInstruction = TradeInstruction.builder()
                .action(TradeAction.BUY)
                .symbol("BTC")
                .quantity(new BigDecimal("0.02")) // 1800 USDT，余额足够
                .rationale("LLM buy test")
                .build();
        TradeInstruction sellInstruction = TradeInstruction.builder()
                .action(TradeAction.SELL)
                .symbol("BTC")
                .quantity(new BigDecimal("0.01")) // 持仓不足，将被 PositionCheckRuleFilter 截断
                .rationale("LLM sell test")
                .build();
        TradePlan plan = TradePlan.builder()
                .rationale("test plan")
                .instructions(List.of(buyInstruction, sellInstruction))
                .build();
        Mockito.when(llmPort.askForPlan(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(plan);
    }

    @Test
    public void testFullTradingCycle() {
        // 执行完整的交易周期
        tradingCycleAppService.executeCycle(strategyId);

        // 验证账户已被保存（余额应已扣除买入成本）
        ArgumentCaptor<VirtualAccountAggregate> accountCaptor = ArgumentCaptor.forClass(VirtualAccountAggregate.class);
        verify(virtualAccountRepository, atLeastOnce()).save(accountCaptor.capture());
        VirtualAccountAggregate savedAccount = accountCaptor.getValue();
        // 计算预期余额：10000 - 0.02*90000* (考虑 0.1% 手续费) ≈ 10000 - 1800 - 1.8 ≈ 8198.2
        BigDecimal expectedCost = new BigDecimal("0.02").multiply(new BigDecimal("90000"));
        BigDecimal expectedFee = expectedCost.multiply(new BigDecimal("0.001"));
        BigDecimal expectedBalance = new BigDecimal("10000").subtract(expectedCost).subtract(expectedFee);
        assertEquals(0, savedAccount.getBalance().compareTo(expectedBalance), "余额应已扣除买入成本和手续费");

        // 验证交易周期已被持久化
        verify(tradingCycleRepository, atLeastOnce()).save(any());

        // 验证 PositionCheckRuleFilter 已经截断了 SELL 指令（因为没有持仓）
        // 通过捕获 DecisionEntity 中的指令数量来确认
        // 这里我们直接检查日志或返回值比较困难，改为间接检查：
        // 由于卖出指令被截断为 0（无持仓），执行阶段不会产生 TradeExecutionEntity
        // 所以交易周期的 executions 数量应为 1（仅 BUY）
        // 为此我们捕获 TradingCycleAggregate 并检查其 executions 列表大小
        ArgumentCaptor<com.weihua.strategy.domain.model.aggregate.TradingCycleAggregate> cycleCaptor = ArgumentCaptor.forClass(com.weihua.strategy.domain.model.aggregate.TradingCycleAggregate.class);
        verify(tradingCycleRepository).save(cycleCaptor.capture());
        com.weihua.strategy.domain.model.aggregate.TradingCycleAggregate savedCycle = cycleCaptor.getValue();
        // savedCycle 实际类型为 TradingCycleAggregate，使用反射读取 executions
        try {
            java.lang.reflect.Method getExecutions = savedCycle.getClass().getMethod("getExecutions");
            List<?> executions = (List<?>) getExecutions.invoke(savedCycle);
            assertEquals(1, executions.size(), "只应有一次 BUY 执行，SELL 被截断");
        } catch (Exception e) {
            fail("无法通过反射读取 executions: " + e.getMessage());
        }
    }
}
