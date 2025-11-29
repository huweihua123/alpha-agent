package com.weihua.infrastructure.dao;

import com.weihua.infrastructure.dao.po.PortfolioSnapshotPO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface IPortfolioSnapshotDao {
    int insert(PortfolioSnapshotPO po);
    List<PortfolioSnapshotPO> selectByStrategyId(Long strategyInstanceId);
}
