package com.weihua.infrastructure.dao;

import com.weihua.infrastructure.dao.po.MarketHistoryPO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface IMarketHistoryDao {
    int insert(MarketHistoryPO po);
    List<MarketHistoryPO> selectByStrategyId(Long strategyInstanceId);
}
