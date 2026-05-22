package com.liangalong.ces.core.model;

/**
 * 任务状态机
 * WAITING -> QUEUED -> PROCESSING -> COMPLETED / FAILED / TIMEOUT
 */
public enum TaskStatus {
    WAITING,      // 等待入队
    QUEUED,       // 已入队
    PROCESSING,   // 执行中
    COMPLETED,    // 完成
    FAILED,       // 失败
    TIMEOUT       // 超时
}
