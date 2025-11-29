package com.weihua.strategy.domain.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TradeAction {
    BUY("BUY", "买入"),
    SELL("SELL", "卖出"),
    HOLD("HOLD", "持有");

    private final String code;
    private final String desc;

    public static TradeAction fromCode(String code) {
        for (TradeAction action : values()) {
            if (action.code.equalsIgnoreCase(code)) {
                return action;
            }
        }
        return HOLD; // 默认或者抛异常
    }
}
