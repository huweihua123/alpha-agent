package com.weihua.strategy.domain.repository;

/**
 * Prompt 资源仓库接口
 */
public interface IPromptRepository {
    
    /**
     * 根据模板 ID 获取模板内容
     * @param templateId 模板 ID
     * @return 模板内容，如果不存在返回 null
     */
    String getTemplateContent(String templateId);
}
