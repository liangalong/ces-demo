package com.liangalong.ces.core.config;

import com.liangalong.ces.core.model.Worker;
import com.liangalong.ces.core.scheduler.TaskSchedulerEngine;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;

/**
 * 初始化配置 - 注册模拟设备
 */
@Configuration
public class WESConfig {

    private final TaskSchedulerEngine engine;

    public WESConfig(TaskSchedulerEngine engine) {
        this.engine = engine;
    }

    @PostConstruct
    public void init() {
        // 注册模拟设备
        engine.registerWorker(new Worker("输送线-A区", Worker.WorkerType.CONVEYOR, "A"));
        engine.registerWorker(new Worker("输送线-B区", Worker.WorkerType.CONVEYOR, "B"));
        engine.registerWorker(new Worker("AGV-1号", Worker.WorkerType.AGV, "A"));
        engine.registerWorker(new Worker("AGV-2号", Worker.WorkerType.AGV, "B"));
        engine.registerWorker(new Worker("AGV-3号", Worker.WorkerType.AGV, "C"));
        engine.registerWorker(new Worker("四穿车-1号", Worker.WorkerType.SHUTTLE, "A"));
        engine.registerWorker(new Worker("四穿车-2号", Worker.WorkerType.SHUTTLE, "B"));
        engine.registerWorker(new Worker("提升机-1号", Worker.WorkerType.ELEVATOR, "A"));
        engine.registerWorker(new Worker("提升机-2号", Worker.WorkerType.ELEVATOR, "B"));
        engine.registerWorker(new Worker("机械臂-1号", Worker.WorkerType.ARM, "C"));

        engine.start();
    }

    @PreDestroy
    public void destroy() {
        engine.stop();
    }
}
