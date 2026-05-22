package com.liangalong.ces.core.model;

import java.time.Instant;

/**
 * 仓储任务
 */
public class Task {

    private String id;
    private String type;           // PICK(拣选), PUT(上架), REPLENISH(补货), TRANSFER(移库)
    private TaskPriority priority;
    private String sourceLocation;   // 起始库位
    private String targetLocation;   // 目标库位
    private String sku;             // 商品编码
    private int quantity;
    private TaskStatus status;
    private Instant createdTime;
    private Instant assignedTime;
    private Instant completedTime;
    private long timeoutMs;         // 超时时间(毫秒)

    public Task() {}

    public Task(String id, String type, TaskPriority priority, String sourceLocation,
                String targetLocation, String sku, int quantity) {
        this.id = id;
        this.type = type;
        this.priority = priority;
        this.sourceLocation = sourceLocation;
        this.targetLocation = targetLocation;
        this.sku = sku;
        this.quantity = quantity;
        this.status = TaskStatus.WAITING;
        this.createdTime = Instant.now();
        this.timeoutMs = 30000; // 默认30秒超时
    }

    // --- getters / setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }

    public String getSourceLocation() { return sourceLocation; }
    public void setSourceLocation(String sourceLocation) { this.sourceLocation = sourceLocation; }

    public String getTargetLocation() { return targetLocation; }
    public void setTargetLocation(String targetLocation) { this.targetLocation = targetLocation; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public Instant getCreatedTime() { return createdTime; }
    public void setCreatedTime(Instant createdTime) { this.createdTime = createdTime; }

    public Instant getAssignedTime() { return assignedTime; }
    public void setAssignedTime(Instant assignedTime) { this.assignedTime = assignedTime; }

    public Instant getCompletedTime() { return completedTime; }
    public void setCompletedTime(Instant completedTime) { this.completedTime = completedTime; }

    public long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }

    public boolean isTimeout() {
        if (status == TaskStatus.PROCESSING && assignedTime != null) {
            return Instant.now().toEpochMilli() - assignedTime.toEpochMilli() > timeoutMs;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("Task[%s] %s %s -> %s [%s] %s",
                id, type, sourceLocation, targetLocation, priority, status);
    }
}
