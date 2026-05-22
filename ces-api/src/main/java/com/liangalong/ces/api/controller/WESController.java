package com.liangalong.ces.api.controller;

import com.liangalong.ces.api.dto.DispatchSnapshot;
import com.liangalong.ces.core.model.Task;
import com.liangalong.ces.core.model.TaskPriority;
import com.liangalong.ces.core.scheduler.TaskSchedulerEngine;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * WES 调度控制API
 */
@RestController
@RequestMapping("/api/wes")
public class WESController {

    private final TaskSchedulerEngine engine;

    public WESController(TaskSchedulerEngine engine) {
        this.engine = engine;
    }

    /** 获取调度状态快照 */
    @GetMapping("/status")
    public DispatchSnapshot getStatus() {
        return DispatchSnapshot.from(
                engine.getQueueSize(),
                engine.getProcessingCount(),
                engine.getTotalDispatched(),
                engine.getTotalCompleted(),
                engine.getTotalTimeout(),
                engine.getAllWorkers(),
                engine.getProcessingTasks(),
                engine.getCompletedHistory()
        );
    }

    /** 提交一个任务 */
    @PostMapping("/tasks")
    public Task submitTask(@RequestParam String type,
                           @RequestParam(defaultValue = "NORMAL") String priority,
                           @RequestParam String src,
                           @RequestParam String dst,
                           @RequestParam(defaultValue = "SKU-X") String sku,
                           @RequestParam(defaultValue = "1") int qty) {
        Task task = new Task(
                "TASK-" + System.currentTimeMillis(),
                type,
                TaskPriority.valueOf(priority),
                src, dst, sku, qty
        );
        return engine.submitTask(task);
    }

    /** 批量生成并提交测试任务 */
    @PostMapping("/tasks/batch")
    public List<Task> generateBatch(@RequestParam(defaultValue = "10") int count) {
        List<Task> tasks = engine.generateTestTasks(count);
        return engine.submitTasks(tasks);
    }
}
