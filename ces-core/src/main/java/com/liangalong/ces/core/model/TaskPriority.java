package com.liangalong.ces.core.model;

/**
 * 任务优先级
 */
public enum TaskPriority {
    URGENT(0),      // 紧急（加急订单）
    HIGH(1),        // 高优先级（VIP客户）
    NORMAL(2),      // 正常
    LOW(3);         // 低优先级（补货等）

    private final int value;

    TaskPriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
