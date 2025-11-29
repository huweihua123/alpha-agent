package com.weihua.types.design.framework.link.model2;

import java.util.HashMap;
import java.util.Map;

/**
 * 链路动态上下文
 */
public class DynamicContext {

    private boolean proceed;

    public DynamicContext() {
        this.proceed = true;
    }

    private Map<String, Object> dataObjects = new HashMap<>();

    public <T> void setValue(String key, T value) {
        dataObjects.put(key, value);
    }

    public <T> T getValue(String key) {
        return (T) dataObjects.get(key);
    }

    public boolean isProceed() {
        return proceed;
    }

    public void setProceed(boolean proceed) {
        this.proceed = proceed;
    }
}
