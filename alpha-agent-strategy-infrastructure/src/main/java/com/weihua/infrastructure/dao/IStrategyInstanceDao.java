package com.weihua.infrastructure.dao;

import com.weihua.infrastructure.dao.po.StrategyInstancePO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface IStrategyInstanceDao {
    int insert(StrategyInstancePO po);
    int update(StrategyInstancePO po);
    int updateStatus(Long id, String status);
    int updateStatusByStrategyId(@org.apache.ibatis.annotations.Param("strategyId") String strategyId, @org.apache.ibatis.annotations.Param("status") String status);
    int updateConfig(Long id, String configJson);
    int updateConfigByStrategyId(@org.apache.ibatis.annotations.Param("strategyId") String strategyId, @org.apache.ibatis.annotations.Param("configJson") String configJson);
    StrategyInstancePO selectById(Long id);
    StrategyInstancePO selectByStrategyId(String strategyId);
    List<StrategyInstancePO> selectByStatus(String status);
    List<StrategyInstancePO> selectByUserId(String userId);
    List<StrategyInstancePO> selectByUserIdAndStatus(@org.apache.ibatis.annotations.Param("userId") String userId, @org.apache.ibatis.annotations.Param("status") String status);
}
