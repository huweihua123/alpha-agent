package com.weihua.infrastructure.dao;

import com.weihua.infrastructure.dao.po.DecisionLogPO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface IDecisionLogDao {
    int insert(DecisionLogPO po);
    List<DecisionLogPO> selectByStrategyId(Long strategyInstanceId);
}
