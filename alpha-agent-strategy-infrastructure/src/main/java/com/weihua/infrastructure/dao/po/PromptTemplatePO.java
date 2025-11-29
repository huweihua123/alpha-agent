package com.weihua.infrastructure.dao.po;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Prompt模板持久化对象
 */
@Data
public class PromptTemplatePO {
    /** 物理主键ID */
    private Long id;
    /** 业务模板ID (UUID) */
    private String templateId;
    /** 模板名称 */
    private String name;
    /** 模板描述 */
    private String description;
    /** Prompt 模板内容 */
    private String content;
    /** 分类 */
    private String category;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
