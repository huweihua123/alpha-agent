package com.weihua.infrastructure.dao;

import com.weihua.infrastructure.dao.po.TradeExecutionPO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface ITradeExecutionDao {
    int insert(TradeExecutionPO po);
    int update(TradeExecutionPO po);
    TradeExecutionPO selectByInstructionId(String instructionId);
    List<TradeExecutionPO> selectByStrategyId(Long strategyInstanceId);
}
