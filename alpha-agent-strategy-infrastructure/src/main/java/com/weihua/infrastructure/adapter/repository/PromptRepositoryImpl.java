package com.weihua.infrastructure.adapter.repository;

import com.weihua.infrastructure.dao.IPromptTemplateDao;
import com.weihua.infrastructure.dao.po.PromptTemplatePO;
import com.weihua.strategy.domain.repository.IPromptRepository;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;

@Repository
public class PromptRepositoryImpl implements IPromptRepository {

    @Resource
    private IPromptTemplateDao promptTemplateDao;

    @Override
    public String getTemplateContent(String templateId) {
        PromptTemplatePO po = promptTemplateDao.selectByTemplateId(templateId);
        return po != null ? po.getContent() : null;
    }
}
