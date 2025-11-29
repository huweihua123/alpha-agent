package com.weihua.infrastructure.dao;

import com.weihua.infrastructure.dao.po.StrategyAccountPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IStrategyAccountDao {
    int insert(StrategyAccountPO po);
    int updateBalance(StrategyAccountPO po);
    StrategyAccountPO selectByStrategyIdAndCurrency(@Param("strategyInstanceId") Long strategyInstanceId, @Param("currency") String currency);
}
