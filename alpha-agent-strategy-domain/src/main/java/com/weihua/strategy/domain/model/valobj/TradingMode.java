package com.weihua.strategy.domain.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TradingMode {
    VIRTUAL("VIRTUAL", "模拟盘"),
    LIVE("LIVE", "实盘");

    private final String code;
    private final String desc;
}
