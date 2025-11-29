package com.weihua.infrastructure.dao;

import com.weihua.infrastructure.dao.po.StrategyPositionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface IStrategyPositionDao {
    int insert(StrategyPositionPO po);
    int updatePosition(StrategyPositionPO po);
    StrategyPositionPO selectByStrategyIdAndSymbol(@Param("strategyInstanceId") Long strategyInstanceId, @Param("symbol") String symbol);
    List<StrategyPositionPO> selectByStrategyId(Long strategyInstanceId);
}
