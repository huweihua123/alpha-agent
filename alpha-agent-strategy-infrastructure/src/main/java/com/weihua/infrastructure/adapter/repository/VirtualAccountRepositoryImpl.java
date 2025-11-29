package com.weihua.infrastructure.adapter.repository;

import com.weihua.infrastructure.dao.IStrategyAccountDao;
import com.weihua.infrastructure.dao.IStrategyPositionDao;
import com.weihua.infrastructure.dao.po.StrategyAccountPO;
import com.weihua.infrastructure.dao.po.StrategyPositionPO;
import com.weihua.strategy.domain.model.aggregate.VirtualAccountAggregate;
import com.weihua.strategy.domain.repository.IVirtualAccountRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class VirtualAccountRepositoryImpl implements IVirtualAccountRepository {

    @Resource
    private IStrategyAccountDao strategyAccountDao;

    @Resource
    private IStrategyPositionDao strategyPositionDao;

    @Resource
    private com.weihua.infrastructure.dao.IStrategyInstanceDao strategyInstanceDao;

    private Long getPhysicalId(String strategyId) {
        com.weihua.infrastructure.dao.po.StrategyInstancePO po = strategyInstanceDao.selectByStrategyId(strategyId);
        if (po == null) {
            throw new IllegalArgumentException("Strategy not found: " + strategyId);
        }
        return po.getId();
    }

    @Override
    public VirtualAccountAggregate findByStrategyId(String strategyId, String currency) {
        Long physicalId = getPhysicalId(strategyId);

        // 1. 查询账户余额
        StrategyAccountPO accountPO = strategyAccountDao.selectByStrategyIdAndCurrency(physicalId, currency);
        
        // 如果账户不存在，初始化一个默认账户 (仅用于模拟盘首次运行)
        if (accountPO == null) {
            accountPO = new StrategyAccountPO();
            accountPO.setStrategyInstanceId(physicalId);
            accountPO.setCurrency(currency);
            accountPO.setBalance(new BigDecimal("10000.00")); // 默认 10000 U
            accountPO.setFrozen(BigDecimal.ZERO);
            strategyAccountDao.insert(accountPO);
        }

        // 2. 查询持仓列表
        List<StrategyPositionPO> positionPOs = strategyPositionDao.selectByStrategyId(physicalId);
        
        // 3. 转换为 Domain 对象
        Map<String, VirtualAccountAggregate.Position> positions = new HashMap<>();
        if (positionPOs != null) {
            for (StrategyPositionPO po : positionPOs) {
                positions.put(po.getSymbol(), VirtualAccountAggregate.Position.builder()
                        .symbol(po.getSymbol())
                        .quantity(po.getQuantity())
                        .avgPrice(po.getAvgPrice())
                        .build());
            }
        }

        return VirtualAccountAggregate.builder()
                .strategyId(strategyId) // Use String ID
                .currency(currency)
                .balance(accountPO.getBalance())
                .frozen(accountPO.getFrozen())
                .positions(positions)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(VirtualAccountAggregate account) {
        Long physicalId = getPhysicalId(account.getStrategyId());

        // 1. 更新账户余额
        StrategyAccountPO accountPO = strategyAccountDao.selectByStrategyIdAndCurrency(physicalId, account.getCurrency());
        if (accountPO != null) {
            accountPO.setBalance(account.getBalance());
            accountPO.setFrozen(account.getFrozen());
            strategyAccountDao.updateBalance(accountPO);
        }

        // 2. 更新持仓
        List<StrategyPositionPO> dbPositions = strategyPositionDao.selectByStrategyId(physicalId);
        Map<String, StrategyPositionPO> dbPositionMap = dbPositions.stream()
                .collect(Collectors.toMap(StrategyPositionPO::getSymbol, p -> p));

        // 遍历 Domain 中的持仓
        for (VirtualAccountAggregate.Position domainPos : account.getPositions().values()) {
            StrategyPositionPO dbPos = dbPositionMap.get(domainPos.getSymbol());
            if (dbPos == null) {
                // 新增
                StrategyPositionPO newPos = new StrategyPositionPO();
                newPos.setStrategyInstanceId(physicalId);
                newPos.setSymbol(domainPos.getSymbol());
                newPos.setQuantity(domainPos.getQuantity());
                newPos.setAvgPrice(domainPos.getAvgPrice());
                strategyPositionDao.insert(newPos);
            } else {
                // 更新
                dbPos.setQuantity(domainPos.getQuantity());
                dbPos.setAvgPrice(domainPos.getAvgPrice());
                strategyPositionDao.updatePosition(dbPos);
                // 标记已处理
                dbPositionMap.remove(domainPos.getSymbol());
            }
        }

        // 处理剩余的 DB 记录 (Domain 中已不存在，说明卖光了)
        for (StrategyPositionPO remainingDbPos : dbPositionMap.values()) {
            // 将数量置为 0
            remainingDbPos.setQuantity(BigDecimal.ZERO);
            strategyPositionDao.updatePosition(remainingDbPos);
        }
    }
}
