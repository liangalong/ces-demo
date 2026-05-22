package com.liangalong.ces.api.dto;

import com.liangalong.ces.core.model.Task;
import com.liangalong.ces.core.model.TaskPriority;
import com.liangalong.ces.core.model.TaskStatus;
import com.liangalong.ces.core.model.Worker;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 调度状态快照DTO
 */
public class DispatchSnapshot {

    private int queueSize;
    private int processingCount;
    private int totalDispatched;
    private int totalCompleted;
    private int totalTimeout;
    private List<WorkerVO> workers;
    private List<TaskVO> processingTasks;
    private List<TaskVO> completedTasks;

    public static DispatchSnapshot from(
            int queueSize, int processingCount,
            int totalDispatched, int totalCompleted, int totalTimeout,
            List<Worker> workers, List<Task> processing, List<Task> completed) {
        DispatchSnapshot s = new DispatchSnapshot();
        s.queueSize = queueSize;
        s.processingCount = processingCount;
        s.totalDispatched = totalDispatched;
        s.totalCompleted = totalCompleted;
        s.totalTimeout = totalTimeout;
        s.workers = workers.stream().map(WorkerVO::from).collect(Collectors.toList());
        s.processingTasks = processing.stream().map(TaskVO::from).collect(Collectors.toList());
        s.completedTasks = completed.stream().map(TaskVO::from).collect(Collectors.toList());
        return s;
    }

    // --- getters ---
    public int getQueueSize() { return queueSize; }
    public int getProcessingCount() { return processingCount; }
    public int getTotalDispatched() { return totalDispatched; }
    public int getTotalCompleted() { return totalCompleted; }
    public int getTotalTimeout() { return totalTimeout; }
    public List<WorkerVO> getWorkers() { return workers; }
    public List<TaskVO> getProcessingTasks() { return processingTasks; }
    public List<TaskVO> getCompletedTasks() { return completedTasks; }

    public static class WorkerVO {
        private String id;
        private String name;
        private String type;
        private String status;
        private String currentTaskId;
        private double efficiency;
        private String zone;

        static WorkerVO from(Worker w) {
            WorkerVO v = new WorkerVO();
            v.id = w.getId();
            v.name = w.getName();
            v.type = w.getType().name();
            v.status = w.getStatus().name();
            v.currentTaskId = w.getCurrentTaskId();
            v.efficiency = Math.round(w.getEfficiency() * 100) / 100.0;
            v.zone = w.getZone();
            return v;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getType() { return type; }
        public String getStatus() { return status; }
        public String getCurrentTaskId() { return currentTaskId; }
        public double getEfficiency() { return efficiency; }
        public String getZone() { return zone; }
    }

    public static class TaskVO {
        private String id;
        private String type;
        private String priority;
        private String status;
        private String sourceLocation;
        private String targetLocation;
        private String sku;
        private int quantity;

        static TaskVO from(Task t) {
            TaskVO v = new TaskVO();
            v.id = t.getId();
            v.type = t.getType();
            v.priority = t.getPriority().name();
            v.status = t.getStatus().name();
            v.sourceLocation = t.getSourceLocation();
            v.targetLocation = t.getTargetLocation();
            v.sku = t.getSku();
            v.quantity = t.getQuantity();
            return v;
        }

        public String getId() { return id; }
        public String getType() { return type; }
        public String getPriority() { return priority; }
        public String getStatus() { return status; }
        public String getSourceLocation() { return sourceLocation; }
        public String getTargetLocation() { return targetLocation; }
        public String getSku() { return sku; }
        public int getQuantity() { return quantity; }
    }
}
