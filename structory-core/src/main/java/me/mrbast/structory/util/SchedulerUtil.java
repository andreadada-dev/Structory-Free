package me.mrbast.structory.util;

import me.mrbast.platform.Platform;
import me.mrbast.platform.scheduler.PlatformTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public final class SchedulerUtil {

    private static Platform platform;
    private static AsyncExecutor asyncExecutor;

    private static final AtomicLong PLATFORM_TASK_SEQUENCE = new AtomicLong();
    private static final AtomicLong PLATFORM_EXECUTIONS = new AtomicLong();
    private static final AtomicLong PLATFORM_FAILURES = new AtomicLong();
    private static final Map<Long, TrackedPlatformTask> PLATFORM_TASKS = new ConcurrentHashMap<>();

    private SchedulerUtil() {
    }

    public static synchronized void init(JavaPlugin owningPlugin, Platform owningPlatform) {
        Objects.requireNonNull(owningPlugin, "owningPlugin");
        Objects.requireNonNull(owningPlatform, "owningPlatform");

        if (platform != null || asyncExecutor != null) shutdown();

        platform = owningPlatform;
        asyncExecutor = new AsyncExecutor();
        asyncExecutor.init();
    }

    public static AsyncExecutor getAsyncExecutor() {
        return requireExecutor();
    }

    /**
     * Global scheduler alias kept for source compatibility. World/entity access
     * should use region(...) or entity(...) instead.
     */
    @Deprecated
    public static PlatformTask sync(Runnable runnable) {
        return global(runnable);
    }

    public static PlatformTask global(Runnable runnable) {
        return trackOneShot(TaskContext.GLOBAL,
                wrapped -> requirePlatform().scheduleGlobal(wrapped), runnable);
    }

    public static PlatformTask globalLater(Runnable runnable, long delayTicks) {
        TimeUtil.requireNonNegative(delayTicks, "delayTicks");
        return trackOneShot(TaskContext.GLOBAL,
                wrapped -> requirePlatform().scheduleGlobalLater(wrapped, delayTicks), runnable);
    }

    public static PlatformTask globalRepeating(Runnable runnable, long initialDelayTicks, long periodTicks) {
        TimeUtil.requireNonNegative(initialDelayTicks, "initialDelayTicks");
        TimeUtil.requirePositive(periodTicks, "periodTicks");
        return trackRepeating(TaskContext.GLOBAL,
                wrapped -> requirePlatform().scheduleGlobalRepeating(wrapped, initialDelayTicks, periodTicks), runnable);
    }

    public static PlatformTask region(Location location, Runnable runnable) {
        Objects.requireNonNull(location, "location");
        return trackOneShot(TaskContext.REGION,
                wrapped -> requirePlatform().scheduleAt(location, wrapped), runnable);
    }

    public static PlatformTask regionLater(Location location, Runnable runnable, long delayTicks) {
        Objects.requireNonNull(location, "location");
        TimeUtil.requireNonNegative(delayTicks, "delayTicks");
        return trackOneShot(TaskContext.REGION,
                wrapped -> requirePlatform().scheduleAtLater(location, wrapped, delayTicks), runnable);
    }

    public static PlatformTask regionRepeating(Location location, Runnable runnable,
                                               long initialDelayTicks, long periodTicks) {
        Objects.requireNonNull(location, "location");
        TimeUtil.requireNonNegative(initialDelayTicks, "initialDelayTicks");
        TimeUtil.requirePositive(periodTicks, "periodTicks");
        return trackRepeating(TaskContext.REGION,
                wrapped -> requirePlatform().scheduleAtRepeating(location, wrapped, initialDelayTicks, periodTicks), runnable);
    }

    public static PlatformTask entity(Entity entity, Runnable runnable) {
        Objects.requireNonNull(entity, "entity");
        return trackOneShot(TaskContext.ENTITY,
                wrapped -> requirePlatform().scheduleFor(entity, wrapped), runnable);
    }

    public static PlatformTask entityLater(Entity entity, Runnable runnable, long delayTicks) {
        Objects.requireNonNull(entity, "entity");
        TimeUtil.requireNonNegative(delayTicks, "delayTicks");
        return trackOneShot(TaskContext.ENTITY,
                wrapped -> requirePlatform().scheduleForLater(entity, wrapped, delayTicks), runnable);
    }

    public static PlatformTask entityRepeating(Entity entity, Runnable runnable,
                                               long initialDelayTicks, long periodTicks) {
        Objects.requireNonNull(entity, "entity");
        TimeUtil.requireNonNegative(initialDelayTicks, "initialDelayTicks");
        TimeUtil.requirePositive(periodTicks, "periodTicks");
        return trackRepeating(TaskContext.ENTITY,
                wrapped -> requirePlatform().scheduleForRepeating(entity, wrapped, initialDelayTicks, periodTicks), runnable);
    }

    public static Future<?> async(Runnable runnable) {
        return requireExecutor().async(runnable);
    }

    public static void sleep(long milliseconds) {
        TimeUtil.requireNonNegative(milliseconds, "milliseconds");
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Scheduled operation was interrupted", exception);
        }
    }

    public static void asyncThenGlobal(Runnable async, Runnable global) {
        requireExecutor().asyncThen(async, () -> global(global));
    }

    public static void asyncThenRegion(Runnable async, Location location, Runnable region) {
        Objects.requireNonNull(location, "location");
        requireExecutor().asyncThen(async, () -> region(location, region));
    }

    public static void asyncThenEntity(Runnable async, Entity entity, Runnable entityTask) {
        Objects.requireNonNull(entity, "entity");
        requireExecutor().asyncThen(async, () -> entity(entity, entityTask));
    }

    /** @deprecated use asyncThenGlobal, asyncThenRegion or asyncThenEntity explicitly. */
    @Deprecated
    public static void asyncThenSync(Runnable async, Runnable sync) {
        asyncThenGlobal(async, sync);
    }

    /** @deprecated global scheduling is not a generic safe context on Folia. */
    @Deprecated
    public static void safe(Runnable runnable) {
        global(runnable);
    }

    public static void waitTimedThenGlobal(String name, long time, TimeUnit unit, Runnable runnable) {
        requireExecutor().scheduleTask(
                name,
                () -> global(runnable),
                TimeUtil.toMillis(time, unit),
                TimeUnit.MILLISECONDS
        );
    }

    public static void waitTimedThenGlobal(String name, long milliseconds, Runnable runnable) {
        requireExecutor().scheduleTask(name, () -> global(runnable), milliseconds, TimeUnit.MILLISECONDS);
    }

    /** @deprecated use waitTimedThenGlobal. */
    @Deprecated
    public static void waitTimedTickThenSync(String name, long time, TimeUnit unit, Runnable runnable) {
        waitTimedThenGlobal(name, time, unit, runnable);
    }

    /** @deprecated use waitTimedThenGlobal. */
    @Deprecated
    public static void waitTimedTickThenSync(String name, long milliseconds, Runnable runnable) {
        waitTimedThenGlobal(name, milliseconds, runnable);
    }

    /** @deprecated use waitTimedThenGlobal. */
    @Deprecated
    public static void waitTimedTickThenSync(long time, TimeUnit unit, Runnable runnable) {
        waitTimedThenGlobal("waitTimedTickThenSync", time, unit, runnable);
    }

    /** @deprecated use waitTimedThenGlobal. */
    @Deprecated
    public static void waitTickThenSync(long milliseconds, Runnable runnable) {
        waitTimedThenGlobal("waitTickThenSync", milliseconds, runnable);
    }

    public static CompletableFuture<Void> globalThenAsync(Runnable global, Runnable async) {
        CompletableFuture<Void> completion = new CompletableFuture<>();
        global(() -> {
            try {
                global.run();
                requireExecutor().async(() -> {
                    try {
                        async.run();
                        completion.complete(null);
                    } catch (Throwable throwable) {
                        completion.completeExceptionally(throwable);
                    }
                });
            } catch (Throwable throwable) {
                completion.completeExceptionally(throwable);
            }
        });
        return completion;
    }

    public static CompletableFuture<Void> regionThenAsync(Location location, Runnable region, Runnable async) {
        Objects.requireNonNull(location, "location");
        CompletableFuture<Void> completion = new CompletableFuture<>();
        region(location, () -> {
            try {
                region.run();
                requireExecutor().async(() -> completeAsync(async, completion));
            } catch (Throwable throwable) {
                completion.completeExceptionally(throwable);
            }
        });
        return completion;
    }

    public static CompletableFuture<Void> entityThenAsync(Entity entity, Runnable entityTask, Runnable async) {
        Objects.requireNonNull(entity, "entity");
        CompletableFuture<Void> completion = new CompletableFuture<>();
        entity(entity, () -> {
            try {
                entityTask.run();
                requireExecutor().async(() -> completeAsync(async, completion));
            } catch (Throwable throwable) {
                completion.completeExceptionally(throwable);
            }
        });
        return completion;
    }

    /** @deprecated use globalThenAsync, regionThenAsync or entityThenAsync explicitly. */
    @Deprecated
    public static CompletableFuture<Void> syncThenAsync(Runnable sync, Runnable async) {
        return globalThenAsync(sync, async);
    }

    public static AsyncExecutor.TaskChain createChain() {
        return new AsyncExecutor.TaskChain(requireExecutor());
    }

    public static void scheduleTask(String name, Runnable task, long delay, TimeUnit unit) {
        requireExecutor().scheduleTask(name, task, delay, unit);
    }

    public static void scheduleRepeatingTask(String name, Runnable task, long initialDelay, long period, TimeUnit unit) {
        requireExecutor().scheduleRepeatingTask(name, task, initialDelay, period, unit);
    }

    public static void cancelTask(String name) {
        requireExecutor().cancelTask(name);
    }

    public static synchronized void shutdown() {
        cancelPlatformTasks();
        if (asyncExecutor != null) asyncExecutor.shutdown();
        asyncExecutor = null;
        platform = null;
    }

    public static void cancelPlatformTasks() {
        new ArrayList<>(PLATFORM_TASKS.values()).forEach(TrackedPlatformTask::cancel);
        PLATFORM_TASKS.clear();
    }

    public static void onEvery(String name, Runnable task, TimeUnit unit, long period) {
        requireExecutor().onEvery(name, task, period, unit);
    }

    /** @deprecated use global, region or entity explicitly. */
    @Deprecated
    public static void bukkitSync(Runnable runnable) {
        global(runnable);
    }

    public static boolean isFolia() {
        Platform current = platform;
        return current != null && current.isFolia();
    }

    public static String platformLabel() {
        Platform current = platform;
        return current == null ? "uninitialized" : current.getLabel();
    }

    public static SchedulerSnapshot snapshot() {
        purgeCancelledPlatformTasks();
        AsyncExecutor executor = asyncExecutor;

        int globalTasks = 0;
        int regionTasks = 0;
        int entityTasks = 0;
        for (TrackedPlatformTask task : PLATFORM_TASKS.values()) {
            switch (task.context) {
                case GLOBAL:
                    globalTasks++;
                    break;
                case REGION:
                    regionTasks++;
                    break;
                case ENTITY:
                    entityTasks++;
                    break;
                default:
                    break;
            }
        }

        return new SchedulerSnapshot(
                isFolia(),
                platformLabel(),
                PLATFORM_TASKS.size(),
                globalTasks,
                regionTasks,
                entityTasks,
                PLATFORM_EXECUTIONS.get(),
                PLATFORM_FAILURES.get(),
                executor != null && executor.isRunning(),
                executor == null ? 0 : executor.getActiveCount(),
                executor == null ? 0 : executor.getQueueSize(),
                executor == null ? 0 : executor.getTrackedTaskCount(),
                executor == null ? 0L : executor.getSubmittedTaskCount(),
                executor == null ? 0L : executor.getCompletedTaskCount(),
                executor == null ? 0L : executor.getFailedTaskCount()
        );
    }

    private static void completeAsync(Runnable async, CompletableFuture<Void> completion) {
        try {
            async.run();
            completion.complete(null);
        } catch (Throwable throwable) {
            completion.completeExceptionally(throwable);
        }
    }

    private static PlatformTask trackOneShot(TaskContext context,
                                             Function<Runnable, PlatformTask> scheduler,
                                             Runnable runnable) {
        return track(context, scheduler, runnable, true);
    }

    private static PlatformTask trackRepeating(TaskContext context,
                                               Function<Runnable, PlatformTask> scheduler,
                                               Runnable runnable) {
        return track(context, scheduler, runnable, false);
    }

    private static PlatformTask track(TaskContext context,
                                      Function<Runnable, PlatformTask> scheduler,
                                      Runnable runnable,
                                      boolean removeAfterRun) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(scheduler, "scheduler");
        Objects.requireNonNull(runnable, "runnable");

        long id = PLATFORM_TASK_SEQUENCE.incrementAndGet();
        TrackedPlatformTask tracked = new TrackedPlatformTask(id, context);
        PLATFORM_TASKS.put(id, tracked);

        Runnable wrapped = () -> {
            PLATFORM_EXECUTIONS.incrementAndGet();
            try {
                runnable.run();
            } catch (Throwable throwable) {
                PLATFORM_FAILURES.incrementAndGet();
                throw throwable;
            } finally {
                if (removeAfterRun) PLATFORM_TASKS.remove(id);
            }
        };

        try {
            tracked.bind(scheduler.apply(wrapped));
            if (tracked.isCancelled()) PLATFORM_TASKS.remove(id);
            return tracked;
        } catch (RuntimeException | Error throwable) {
            PLATFORM_TASKS.remove(id);
            throw throwable;
        }
    }

    private static void purgeCancelledPlatformTasks() {
        PLATFORM_TASKS.entrySet().removeIf(entry -> entry.getValue().isCancelled());
    }

    private static AsyncExecutor requireExecutor() {
        return Objects.requireNonNull(asyncExecutor, "SchedulerUtil has not been initialized");
    }

    private static Platform requirePlatform() {
        return Objects.requireNonNull(platform, "SchedulerUtil has not been initialized");
    }

    private enum TaskContext {
        GLOBAL,
        REGION,
        ENTITY
    }

    private static final class TrackedPlatformTask implements PlatformTask {
        private final long id;
        private final TaskContext context;
        private volatile PlatformTask delegate;
        private volatile boolean cancelled;

        private TrackedPlatformTask(long id, TaskContext context) {
            this.id = id;
            this.context = context;
        }

        private void bind(PlatformTask delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            if (cancelled) delegate.cancel();
        }

        @Override
        public void cancel() {
            cancelled = true;
            PlatformTask current = delegate;
            if (current != null) current.cancel();
            PLATFORM_TASKS.remove(id);
        }

        @Override
        public boolean isCancelled() {
            PlatformTask current = delegate;
            return cancelled || (current != null && current.isCancelled());
        }
    }

    public static final class SchedulerSnapshot {
        private final boolean folia;
        private final String platformLabel;
        private final int platformTasks;
        private final int globalTasks;
        private final int regionTasks;
        private final int entityTasks;
        private final long platformExecutions;
        private final long platformFailures;
        private final boolean asyncRunning;
        private final int activeAsyncWorkers;
        private final int queuedAsyncTasks;
        private final int trackedAsyncTasks;
        private final long submittedAsyncTasks;
        private final long completedAsyncTasks;
        private final long failedAsyncTasks;

        private SchedulerSnapshot(boolean folia, String platformLabel, int platformTasks,
                                  int globalTasks, int regionTasks, int entityTasks,
                                  long platformExecutions, long platformFailures,
                                  boolean asyncRunning, int activeAsyncWorkers,
                                  int queuedAsyncTasks, int trackedAsyncTasks,
                                  long submittedAsyncTasks, long completedAsyncTasks,
                                  long failedAsyncTasks) {
            this.folia = folia;
            this.platformLabel = platformLabel;
            this.platformTasks = platformTasks;
            this.globalTasks = globalTasks;
            this.regionTasks = regionTasks;
            this.entityTasks = entityTasks;
            this.platformExecutions = platformExecutions;
            this.platformFailures = platformFailures;
            this.asyncRunning = asyncRunning;
            this.activeAsyncWorkers = activeAsyncWorkers;
            this.queuedAsyncTasks = queuedAsyncTasks;
            this.trackedAsyncTasks = trackedAsyncTasks;
            this.submittedAsyncTasks = submittedAsyncTasks;
            this.completedAsyncTasks = completedAsyncTasks;
            this.failedAsyncTasks = failedAsyncTasks;
        }

        public boolean isFolia() { return folia; }
        public String getPlatformLabel() { return platformLabel; }
        public int getPlatformTasks() { return platformTasks; }
        public int getGlobalTasks() { return globalTasks; }
        public int getRegionTasks() { return regionTasks; }
        public int getEntityTasks() { return entityTasks; }
        public long getPlatformExecutions() { return platformExecutions; }
        public long getPlatformFailures() { return platformFailures; }
        public boolean isAsyncRunning() { return asyncRunning; }
        public int getActiveAsyncWorkers() { return activeAsyncWorkers; }
        public int getQueuedAsyncTasks() { return queuedAsyncTasks; }
        public int getTrackedAsyncTasks() { return trackedAsyncTasks; }
        public long getSubmittedAsyncTasks() { return submittedAsyncTasks; }
        public long getCompletedAsyncTasks() { return completedAsyncTasks; }
        public long getFailedAsyncTasks() { return failedAsyncTasks; }
    }

    public static final class AsyncExecutor {
        private static final AtomicInteger THREAD_SEQUENCE = new AtomicInteger();

        private int corePoolSize = 1;
        private int maxPoolSize = 4;
        private long keepAliveTime = 60L;

        private final List<Runnable> onInit = new ArrayList<>();
        private final Map<String, PeriodicTask> onEveryTasks = new ConcurrentHashMap<>();
        private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
        private final AtomicLong submittedTaskCount = new AtomicLong();
        private final AtomicLong failedTaskCount = new AtomicLong();

        private ScheduledFuture<?> onEvery;
        private ScheduledThreadPoolExecutor executor;

        public synchronized void init() {
            if (executor != null && !executor.isShutdown()) return;

            ThreadFactory threadFactory = runnable -> {
                Thread thread = new Thread(runnable, "Structory-Async-" + THREAD_SEQUENCE.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            };

            executor = new ScheduledThreadPoolExecutor(
                    maxPoolSize,
                    threadFactory,
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );
            executor.setCorePoolSize(corePoolSize);
            executor.setMaximumPoolSize(Math.max(corePoolSize, maxPoolSize));
            executor.setKeepAliveTime(keepAliveTime, TimeUnit.SECONDS);
            executor.setRemoveOnCancelPolicy(true);
            executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);

            List<Runnable> pending = new ArrayList<>(onInit);
            onInit.clear();
            pending.forEach(Runnable::run);
        }

        public synchronized void addOnInit(Runnable task) {
            onInit.add(Objects.requireNonNull(task, "task"));
        }

        public void scheduleRepeatingTask(String name, Runnable task, long initialDelay, long period, TimeUnit unit) {
            validateNameAndTask(name, task);
            TimeUtil.requireNonNegative(initialDelay, "initialDelay");
            TimeUtil.requirePositive(period, "period");
            Objects.requireNonNull(unit, "unit");

            if (!isReady()) {
                addOnInit(() -> scheduleRepeatingTask(name, task, initialDelay, period, unit));
                return;
            }
            replaceTask(name, executor.scheduleAtFixedRate(instrument(task), initialDelay, period, unit));
        }

        public void setOnEvery(long initialDelay, long checkPeriod, TimeUnit unit) {
            TimeUtil.requireNonNegative(initialDelay, "initialDelay");
            TimeUtil.requirePositive(checkPeriod, "checkPeriod");
            Objects.requireNonNull(unit, "unit");

            if (!isReady()) {
                addOnInit(() -> setOnEvery(initialDelay, checkPeriod, unit));
                return;
            }
            if (onEvery != null) onEvery.cancel(false);
            onEvery = executor.scheduleAtFixedRate(instrument(this::runOnEvery), initialDelay, checkPeriod, unit);
        }

        private void runOnEvery() {
            onEveryTasks.values().forEach(PeriodicTask::executeIfReady);
        }

        public void onEvery(String name, Runnable task, long period, TimeUnit unit) {
            validateNameAndTask(name, task);
            TimeUtil.requirePositive(period, "period");
            onEveryTasks.put(name, new PeriodicTask(task, period, unit));
        }

        public Future<?> async(Runnable runnable) {
            return requireReadyExecutor().submit(instrument(Objects.requireNonNull(runnable, "runnable")));
        }

        public synchronized void shutdown() {
            if (executor == null) return;

            tasks.values().forEach(future -> future.cancel(false));
            tasks.clear();
            if (onEvery != null) {
                onEvery.cancel(false);
                onEvery = null;
            }
            onEveryTasks.clear();
            onInit.clear();

            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) executor.shutdownNow();
            } catch (InterruptedException exception) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            } finally {
                executor = null;
            }
        }

        public void asyncThen(Runnable async, Runnable continuation) {
            async(() -> {
                try {
                    async.run();
                } finally {
                    continuation.run();
                }
            });
        }

        /** @deprecated use SchedulerUtil asyncThenGlobal/Region/Entity helpers. */
        @Deprecated
        public void asyncThenSync(Runnable async, Runnable sync) {
            asyncThen(async, () -> SchedulerUtil.global(sync));
        }

        public void scheduleTask(String name, Runnable task, long delay, TimeUnit unit) {
            validateNameAndTask(name, task);
            TimeUtil.requireNonNegative(delay, "delay");
            Objects.requireNonNull(unit, "unit");

            if (!isReady()) {
                addOnInit(() -> scheduleTask(name, task, delay, unit));
                return;
            }
            replaceTask(name, executor.schedule(instrument(task), delay, unit));
        }

        public boolean cancelTask(String name) {
            ScheduledFuture<?> future = tasks.remove(name);
            return future != null && future.cancel(false);
        }

        private Runnable instrument(Runnable task) {
            return () -> {
                submittedTaskCount.incrementAndGet();
                try {
                    task.run();
                } catch (Throwable throwable) {
                    failedTaskCount.incrementAndGet();
                    throw throwable;
                }
            };
        }

        private void replaceTask(String name, ScheduledFuture<?> future) {
            ScheduledFuture<?> previous = tasks.put(name, future);
            if (previous != null) previous.cancel(false);
        }

        private boolean isReady() {
            return executor != null && !executor.isShutdown();
        }

        private ScheduledThreadPoolExecutor requireReadyExecutor() {
            if (!isReady()) throw new IllegalStateException("AsyncExecutor has not been initialized");
            return executor;
        }

        private static void validateNameAndTask(String name, Runnable task) {
            if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("name cannot be blank");
            Objects.requireNonNull(task, "task");
        }

        public synchronized void setCorePoolSize(int value) {
            if (value <= 0) throw new IllegalArgumentException("corePoolSize must be positive");
            corePoolSize = value;
            if (isReady()) {
                executor.setCorePoolSize(value);
                if (executor.getMaximumPoolSize() < value) executor.setMaximumPoolSize(value);
            }
        }

        public synchronized void setKeepAliveTime(long value) {
            if (value < 0) throw new IllegalArgumentException("keepAliveTime cannot be negative");
            keepAliveTime = value;
            if (isReady()) executor.setKeepAliveTime(value, TimeUnit.SECONDS);
        }

        public synchronized void setMaxPoolSize(int value) {
            if (value <= 0) throw new IllegalArgumentException("maxPoolSize must be positive");
            maxPoolSize = value;
            if (isReady()) executor.setMaximumPoolSize(Math.max(corePoolSize, value));
        }

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public long getKeepAliveTime() {
            return keepAliveTime;
        }

        public ScheduledThreadPoolExecutor getExecutor() {
            return executor;
        }

        public int getActiveCount() {
            ScheduledThreadPoolExecutor current = executor;
            return current == null ? 0 : current.getActiveCount();
        }

        public int getQueueSize() {
            ScheduledThreadPoolExecutor current = executor;
            return current == null ? 0 : current.getQueue().size();
        }

        public long getCompletedTaskCount() {
            ScheduledThreadPoolExecutor current = executor;
            return current == null ? 0L : current.getCompletedTaskCount();
        }

        public long getSubmittedTaskCount() {
            return submittedTaskCount.get();
        }

        public long getFailedTaskCount() {
            return failedTaskCount.get();
        }

        public int getTrackedTaskCount() {
            tasks.entrySet().removeIf(entry -> entry.getValue().isCancelled() || entry.getValue().isDone());
            return tasks.size() + onEveryTasks.size() + (onEvery == null || onEvery.isCancelled() ? 0 : 1);
        }

        public boolean isRunning() {
            return isReady();
        }

        public static final class TaskChain {
            private final AsyncExecutor executor;
            private long cumulativeDelay;

            public TaskChain(AsyncExecutor executor) {
                this.executor = Objects.requireNonNull(executor, "executor");
            }

            public TaskChain then(String name, Runnable task, long delay, TimeUnit unit) {
                cumulativeDelay += TimeUtil.toMillis(delay, unit);
                executor.scheduleTask(name, task, cumulativeDelay, TimeUnit.MILLISECONDS);
                return this;
            }

            public TaskChain then(String name, Runnable task) {
                executor.scheduleTask(name, task, cumulativeDelay, TimeUnit.MILLISECONDS);
                return this;
            }
        }

        private static final class PeriodicTask {
            private final Runnable task;
            private final long waitMillis;
            private volatile long nextExecution;

            private PeriodicTask(Runnable task, long period, TimeUnit unit) {
                this.task = Objects.requireNonNull(task, "task");
                this.waitMillis = TimeUtil.toMillis(period, unit);
            }

            private void executeIfReady() {
                long now = System.currentTimeMillis();
                if (now < nextExecution) return;
                nextExecution = now + waitMillis;
                task.run();
            }
        }
    }
}
