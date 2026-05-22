package com.liangalong.ces.core.scheduler;

import com.liangalong.ces.core.model.Task;
import com.liangalong.ces.core.model.TaskPriority;
import com.liangalong.ces.core.model.TaskStatus;
import com.liangalong.ces.core.model.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * WES 任务调度引擎 v2.0
 * <p>
 * 核心功能：
 * 1. 优先级队列管理 - 紧急任务优先
 * 2. 设备分配 - 同区域优先 > 类型匹配 > 效率最高
 * 3. 超时检测 - 处理僵尸任务
 * 4. Redis 持久化 - 任务队列/设备状态/统计信息持久化
 * 5. WebSocket 事件推送
 */
@Component
public class TaskSchedulerEngine {

    private static final Logger log = LoggerFactory.getLogger(TaskSchedulerEngine.class);

    private static final String REDIS_KEY_QUEUE = "ces:task:queue";
    private static final String REDIS_KEY_PROCESSING = "ces:task:processing";
    private static final String REDIS_KEY_COMPLETED = "ces:task:completed";
    private static final String REDIS_KEY_WORKERS = "ces:workers";
    private static final String REDIS_KEY_STATS = "ces:stats";
    private static final int MAX_HISTORY = 500;

    // 内存就绪队列 - 按优先级排序
    private final PriorityBlockingQueue<Task> readyQueue = new PriorityBlockingQueue<>(1000,
            (a, b) -> Integer.compare(a.getPriority().getValue(), b.getPriority().getValue()));

    // 执行中的任务
    private final Map<String, Task> processingTasks = new ConcurrentHashMap<>();

    // 设备池
    private final Map<String, Worker> workers = new ConcurrentHashMap<>();

    // 已完成任务历史
    private final ConcurrentLinkedQueue<Task> completedHistory = new ConcurrentLinkedQueue<>();

    private final ReentrantLock lock = new ReentrantLock();
    private ScheduledExecutorService scheduler;

    // 统计
    private volatile int totalDispatched = 0;
    private volatile int totalCompleted = 0;
    private volatile int totalTimeout = 0;

    private final RedisTemplate<String, Object> redisTemplate;

    // 事件监听器（WebSocket 推送用）
    private final List<DispatchEventListener> eventListeners = new CopyOnWriteArrayList<>();

    public TaskSchedulerEngine(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addEventListener(DispatchEventListener listener) {
        eventListeners.add(listener);
    }

    /**
     * 初始化调度引擎 - 从 Redis 恢复状态
     */
    public void start() {
        // 从 Redis 恢复统计
        Map<Object, Object> stats = redisTemplate.opsForHash().entries(REDIS_KEY_STATS);
        if (!stats.isEmpty()) {
            totalDispatched = intVal(stats.get("dispatched"));
            totalCompleted = intVal(stats.get("completed"));
            totalTimeout = intVal(stats.get("timeout"));
            log.info("♻️ 从 Redis 恢复统计: 已分配={}, 已完成={}, 超时={}",
                    totalDispatched, totalCompleted, totalTimeout);
        }

        // 从 Redis 恢复设备状态
        Set<Object> workerIds = redisTemplate.opsForSet().members(REDIS_KEY_WORKERS);
        if (workerIds != null) {
            // 设备在 WESConfig 中重新注册，这里只做清理
            redisTemplate.delete(REDIS_KEY_WORKERS);
        }

        scheduler = Executors.newScheduledThreadPool(2);
        // 主调度循环
        scheduler.scheduleAtFixedRate(this::dispatchLoop, 0, 200, TimeUnit.MILLISECONDS);
        // 超时检查
        scheduler.scheduleAtFixedRate(this::checkTimeout, 1, 1, TimeUnit.SECONDS);
        // 状态持久化：每5秒存一次
        scheduler.scheduleAtFixedRate(this::persistStats, 5, 5, TimeUnit.SECONDS);

        log.info("🔄 WES调度引擎 v2.0 已启动 (Redis持久化)");
        fireEvent("ENGINE_START", null);
    }

    /**
     * 停止调度引擎
     */
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        persistStats();
        log.info("⏹ WES调度引擎已停止");
        fireEvent("ENGINE_STOP", null);
    }

    // ==================== 设备管理 ====================

    public void registerWorker(Worker worker) {
        workers.put(worker.getId(), worker);
        redisTemplate.opsForSet().add(REDIS_KEY_WORKERS, worker.getId());
        log.info("🔌 设备注册: {}", worker);
        fireEvent("WORKER_REGISTER", worker);
    }

    public void unregisterWorker(String workerId) {
        workers.remove(workerId);
        redisTemplate.opsForSet().remove(REDIS_KEY_WORKERS, workerId);
        fireEvent("WORKER_UNREGISTER", workerId);
    }

    public List<Worker> getAllWorkers() {
        return List.copyOf(workers.values());
    }

    public List<Worker> getIdleWorkers() {
        return workers.values().stream()
                .filter(Worker::isIdle)
                .collect(Collectors.toList());
    }

    // ==================== 任务管理 ====================

    /**
     * 提交任务 - 入内存队列 + Redis 持久化
     */
    public Task submitTask(Task task) {
        task.setStatus(TaskStatus.QUEUED);
        readyQueue.offer(task);
        // Redis 持久化
        redisTemplate.opsForList().rightPush(REDIS_KEY_QUEUE, task.getId());
        log.info("📥 任务入队: {} (优先级:{})", task.getId(), task.getPriority());
        fireEvent("TASK_SUBMIT", task);
        return task;
    }

    /**
     * 批量提交任务
     */
    public List<Task> submitTasks(List<Task> tasks) {
        tasks.forEach(this::submitTask);
        return tasks;
    }

    /**
     * 批量生成测试任务
     */
    public List<Task> generateTestTasks(int count) {
        String[] types = {"PICK", "PUT", "REPLENISH", "TRANSFER"};
        String[] skus = {"SKU-A001", "SKU-B002", "SKU-C003", "SKU-D004", "SKU-E005"};
        TaskPriority[] priorities = TaskPriority.values();

        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String type = types[ThreadLocalRandom.current().nextInt(types.length)];
            TaskPriority priority = priorities[ThreadLocalRandom.current().nextInt(priorities.length)];
            String sku = skus[ThreadLocalRandom.current().nextInt(skus.length)];
            int qty = ThreadLocalRandom.current().nextInt(1, 100);

            String src = String.format("LOC-%c%02d",
                    (char) ('A' + ThreadLocalRandom.current().nextInt(10)),
                    ThreadLocalRandom.current().nextInt(1, 50));
            String dst = String.format("LOC-%c%02d",
                    (char) ('A' + ThreadLocalRandom.current().nextInt(10)),
                    ThreadLocalRandom.current().nextInt(1, 50));

            Task task = new Task(
                    "TASK-" + System.currentTimeMillis() + "-" + i,
                    type, priority, src, dst, sku, qty
            );
            tasks.add(task);
        }
        return tasks;
    }

    // ==================== 统计查询 ====================

    public int getQueueSize() { return readyQueue.size(); }
    public int getProcessingCount() { return processingTasks.size(); }
    public int getTotalDispatched() { return totalDispatched; }
    public int getTotalCompleted() { return totalCompleted; }
    public int getTotalTimeout() { return totalTimeout; }

    public List<Task> getProcessingTasks() {
        return List.copyOf(processingTasks.values());
    }

    public List<Task> getCompletedHistory() {
        return completedHistory.stream().collect(Collectors.toList());
    }

    public Map<String, Object> getStats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("queueSize", getQueueSize());
        m.put("processingCount", getProcessingCount());
        m.put("totalDispatched", totalDispatched);
        m.put("totalCompleted", totalCompleted);
        m.put("totalTimeout", totalTimeout);
        return m;
    }

    // ==================== 核心调度逻辑 ====================

    private void dispatchLoop() {
        try {
            Task task = readyQueue.poll();
            if (task == null) return;

            Worker assigned = selectWorker(task);
            if (assigned == null) {
                readyQueue.offer(task);
                return;
            }

            // 分配
            task.setStatus(TaskStatus.PROCESSING);
            task.setAssignedTime(Instant.now());
            assigned.assignTask(task.getId());
            processingTasks.put(task.getId(), task);
            totalDispatched++;

            log.info("🚀 任务分配: {} -> {} (区域:{})",
                    task.getId(), assigned.getName(), assigned.getZone());
            fireEvent("TASK_DISPATCH", Map.of("task", task, "worker", assigned));

            // 模拟执行耗时
            long execTime = (long) (500 + Math.random() * 2000 / assigned.getEfficiency());
            scheduler.schedule(() -> completeTask(task, assigned), execTime, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            log.error("调度异常", e);
        }
    }

    private Worker selectWorker(Task task) {
        List<Worker> idle = getIdleWorkers();
        if (idle.isEmpty()) return null;

        // 1. 同区域优先
        if (task.getSourceLocation() != null) {
            String zone = task.getSourceLocation().substring(0, Math.min(1, task.getSourceLocation().length()));
            Optional<Worker> zoneMatch = idle.stream()
                    .filter(w -> zone.equals(w.getZone()))
                    .findFirst();
            if (zoneMatch.isPresent()) return zoneMatch.get();
        }

        // 2. 设备类型匹配
        Optional<Worker.WorkerType> preferredType = getPreferredWorkerType(task.getType());
        if (preferredType.isPresent()) {
            Optional<Worker> typeMatch = idle.stream()
                    .filter(w -> w.getType() == preferredType.get())
                    .findFirst();
            if (typeMatch.isPresent()) return typeMatch.get();
        }

        // 3. 效率最高
        return idle.stream()
                .max(Comparator.comparingDouble(Worker::getEfficiency))
                .orElse(idle.get(0));
    }

    private Optional<Worker.WorkerType> getPreferredWorkerType(String taskType) {
        return switch (taskType) {
            case "PICK" -> Optional.of(Worker.WorkerType.AGV);
            case "PUT" -> Optional.of(Worker.WorkerType.CONVEYOR);
            case "REPLENISH" -> Optional.of(Worker.WorkerType.SHUTTLE);
            case "TRANSFER" -> Optional.of(Worker.WorkerType.ELEVATOR);
            default -> Optional.empty();
        };
    }

    private void completeTask(Task task, Worker worker) {
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedTime(Instant.now());
        worker.release();

        processingTasks.remove(task.getId());
        completedHistory.offer(task);
        while (completedHistory.size() > MAX_HISTORY) {
            completedHistory.poll();
        }
        totalCompleted++;

        long elapsed = task.getCompletedTime().toEpochMilli() - task.getAssignedTime().toEpochMilli();
        log.info("✅ 任务完成: {} (设备:{}, 耗时:{}ms)", task.getId(), worker.getName(), elapsed);
        fireEvent("TASK_COMPLETE", Map.of("task", task, "worker", worker, "elapsedMs", elapsed));
    }

    private void checkTimeout() {
        List<String> timeoutIds = new ArrayList<>();
        for (Task task : processingTasks.values()) {
            if (task.isTimeout()) {
                timeoutIds.add(task.getId());
                task.setStatus(TaskStatus.TIMEOUT);
                Worker worker = findWorkerByTaskId(task.getId());
                if (worker != null) {
                    worker.release();
                }
                totalTimeout++;
                log.warn("⏰ 任务超时: {} (超过{}ms)", task.getId(), task.getTimeoutMs());
                fireEvent("TASK_TIMEOUT", Map.of("task", task));
            }
        }
        timeoutIds.forEach(processingTasks::remove);
    }

    private Worker findWorkerByTaskId(String taskId) {
        return workers.values().stream()
                .filter(w -> taskId.equals(w.getCurrentTaskId()))
                .findFirst()
                .orElse(null);
    }

    // ==================== Redis 持久化 ====================

    /**
     * 持久化统计信息到 Redis
     */
    private void persistStats() {
        try {
            Map<String, Integer> stats = new HashMap<>();
            stats.put("dispatched", totalDispatched);
            stats.put("completed", totalCompleted);
            stats.put("timeout", totalTimeout);
            redisTemplate.opsForHash().putAll(REDIS_KEY_STATS, stats);
        } catch (Exception e) {
            log.warn("Redis 持久化失败", e);
        }
    }

    private int intVal(Object val) {
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) return Integer.parseInt((String) val);
        return 0;
    }

    // ==================== 事件推送 ====================

    private void fireEvent(String type, Object data) {
        for (DispatchEventListener listener : eventListeners) {
            try {
                listener.onEvent(type, data);
            } catch (Exception e) {
                log.warn("事件推送异常", e);
            }
        }
    }

    public interface DispatchEventListener {
        void onEvent(String type, Object data);
    }
}
