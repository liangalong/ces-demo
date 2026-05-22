package com.liangalong.ces.core.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 设备模型 - 模拟输送线/AGV/提升机等自动化设备
 */
public class Worker {

    private static final AtomicInteger ID_GEN = new AtomicInteger(1);

    public enum WorkerType {
        CONVEYOR,    // 输送线
        AGV,         // 自动导引车
        ELEVATOR,    // 提升机
        SHUTTLE,     // 四穿车
        ARM          // 机械臂
    }

    public enum WorkerStatus {
        IDLE,        // 空闲
        BUSY,        // 忙碌
        FAULT,       // 故障
        OFFLINE      // 离线
    }

    private String id;
    private String name;
    private WorkerType type;
    private WorkerStatus status;
    private String currentTaskId;
    private double efficiency;         // 效率系数 0.0~1.0
    private String zone;               // 所在区域

    public Worker() {}

    public Worker(String name, WorkerType type, String zone) {
        this.id = "W" + String.format("%03d", ID_GEN.getAndIncrement());
        this.name = name;
        this.type = type;
        this.zone = zone;
        this.status = WorkerStatus.IDLE;
        this.efficiency = 0.8 + Math.random() * 0.2; // 0.8~1.0
    }

    // --- getters / setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public WorkerType getType() { return type; }
    public void setType(WorkerType type) { this.type = type; }

    public WorkerStatus getStatus() { return status; }
    public void setStatus(WorkerStatus status) { this.status = status; }

    public String getCurrentTaskId() { return currentTaskId; }
    public void setCurrentTaskId(String currentTaskId) { this.currentTaskId = currentTaskId; }

    public double getEfficiency() { return efficiency; }
    public void setEfficiency(double efficiency) { this.efficiency = efficiency; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public boolean isIdle() { return status == WorkerStatus.IDLE; }

    public void assignTask(String taskId) {
        this.currentTaskId = taskId;
        this.status = WorkerStatus.BUSY;
    }

    public void release() {
        this.currentTaskId = null;
        this.status = WorkerStatus.IDLE;
    }

    @Override
    public String toString() {
        return String.format("Worker[%s] %s [%s] %s",
                id, name, type, status);
    }
}
