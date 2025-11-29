package com.weihua.infrastructure.dao;

import com.weihua.infrastructure.dao.po.PromptTemplatePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IPromptTemplateDao {
    
    int insert(PromptTemplatePO record);

    PromptTemplatePO selectById(Long id);

    PromptTemplatePO selectByTemplateId(@Param("templateId") String templateId);

    List<PromptTemplatePO> selectByCategory(@Param("category") String category);

    List<PromptTemplatePO> selectAll();
    
    int update(PromptTemplatePO record);
    
    int deleteById(Long id);
}
