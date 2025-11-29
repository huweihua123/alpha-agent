package com.weihua.strategy.domain.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StrategyStatus {
    STOPPED("STOPPED", "已停止"),
    RUNNING("RUNNING", "运行中"),
    PAUSED("PAUSED", "已暂停"),
    ERROR("ERROR", "异常");

    private final String code;
    private final String desc;
}
