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

public final class SchedulerUtil {

    private static JavaPlugin plugin;
    private static Platform platform;
    private static AsyncExecutor asyncExecutor;

    private SchedulerUtil() {
    }

    public static void init(JavaPlugin owningPlugin, Platform owningPlatform) {
        plugin = Objects.requireNonNull(owningPlugin, "owningPlugin");
        platform = Objects.requireNonNull(owningPlatform, "owningPlatform");
        asyncExecutor = new AsyncExecutor();
    }

    public static AsyncExecutor getAsyncExecutor() {
        return requireExecutor();
    }

    public static PlatformTask sync(Runnable runnable) {
        return global(runnable);
    }

    public static PlatformTask global(Runnable runnable) {
        return requirePlatform().scheduleGlobal(runnable);
    }

    public static PlatformTask globalLater(Runnable runnable, long delayTicks) {
        return requirePlatform().scheduleGlobalLater(runnable, delayTicks);
    }

    public static PlatformTask globalRepeating(Runnable runnable, long initialDelayTicks, long periodTicks) {
        return requirePlatform().scheduleGlobalRepeating(runnable, initialDelayTicks, periodTicks);
    }

    public static PlatformTask region(Location location, Runnable runnable) {
        return requirePlatform().scheduleAt(location, runnable);
    }

    public static PlatformTask regionLater(Location location, Runnable runnable, long delayTicks) {
        return requirePlatform().scheduleAtLater(location, runnable, delayTicks);
    }

    public static PlatformTask regionRepeating(Location location, Runnable runnable,
                                               long initialDelayTicks, long periodTicks) {
        return requirePlatform().scheduleAtRepeating(location, runnable, initialDelayTicks, periodTicks);
    }

    public static PlatformTask entity(Entity entity, Runnable runnable) {
        return requirePlatform().scheduleFor(entity, runnable);
    }

    public static PlatformTask entityLater(Entity entity, Runnable runnable, long delayTicks) {
        return requirePlatform().scheduleForLater(entity, runnable, delayTicks);
    }

    public static PlatformTask entityRepeating(Entity entity, Runnable runnable,
                                               long initialDelayTicks, long periodTicks) {
        return requirePlatform().scheduleForRepeating(entity, runnable, initialDelayTicks, periodTicks);
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

    public static void asyncThenSync(Runnable async, Runnable sync) {
        requireExecutor().asyncThenSync(async, sync);
    }

    public static void safe(Runnable runnable) {
        sync(runnable);
    }

    public static void waitTimedTickThenSync(String name, long time, TimeUnit unit, Runnable runnable) {
        requireExecutor().scheduleTask(
                name,
                () -> sync(runnable),
                TimeUtil.toMillis(time, unit),
                TimeUnit.MILLISECONDS
        );
    }

    public static void waitTimedTickThenSync(String name, long milliseconds, Runnable runnable) {
        requireExecutor().scheduleTask(name, () -> sync(runnable), milliseconds, TimeUnit.MILLISECONDS);
    }

    public static void waitTimedTickThenSync(long time, TimeUnit unit, Runnable runnable) {
        waitTimedTickThenSync("waitTimedTickThenSync", time, unit, runnable);
    }

    public static void waitTickThenSync(long milliseconds, Runnable runnable) {
        waitTimedTickThenSync("waitTickThenSync", milliseconds, runnable);
    }

    public static CompletableFuture<Void> syncThenAsync(Runnable sync, Runnable async) {
        CompletableFuture<Void> completion = new CompletableFuture<>();
        safe(() -> {
            try {
                sync.run();
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

    public static void shutdown() {
        if (asyncExecutor != null) asyncExecutor.shutdown();
        asyncExecutor = null;
        plugin = null;
        platform = null;
    }

    public static void onEvery(String name, Runnable task, TimeUnit unit, long period) {
        requireExecutor().onEvery(name, task, period, unit);
    }

    public static void bukkitSync(Runnable runnable) {
        global(runnable);
    }

    private static AsyncExecutor requireExecutor() {
        return Objects.requireNonNull(asyncExecutor, "SchedulerUtil has not been initialized");
    }

    private static Platform requirePlatform() {
        return Objects.requireNonNull(platform, "SchedulerUtil has not been initialized");
    }

    public static final class AsyncExecutor {
        private static final AtomicInteger THREAD_SEQUENCE = new AtomicInteger();

        private int corePoolSize = 1;
        private int maxPoolSize = 4;
        private long keepAliveTime = 60L;

        private final List<Runnable> onInit = new ArrayList<>();
        private final Map<String, PeriodicTask> onEveryTasks = new ConcurrentHashMap<>();
        private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

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
            replaceTask(name, executor.scheduleAtFixedRate(task, initialDelay, period, unit));
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
            onEvery = executor.scheduleAtFixedRate(this::runOnEvery, initialDelay, checkPeriod, unit);
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
            return requireReadyExecutor().submit(Objects.requireNonNull(runnable, "runnable"));
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

        public void asyncThenSync(Runnable async, Runnable sync) {
            async(() -> {
                try {
                    async.run();
                } finally {
                    safe(sync);
                }
            });
        }

        public void scheduleTask(String name, Runnable task, long delay, TimeUnit unit) {
            validateNameAndTask(name, task);
            TimeUtil.requireNonNegative(delay, "delay");
            Objects.requireNonNull(unit, "unit");

            if (!isReady()) {
                addOnInit(() -> scheduleTask(name, task, delay, unit));
                return;
            }
            replaceTask(name, executor.schedule(task, delay, unit));
        }

        public boolean cancelTask(String name) {
            ScheduledFuture<?> future = tasks.remove(name);
            return future != null && future.cancel(false);
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
            if (isReady()) executor.setCorePoolSize(value);
        }

        public synchronized void setKeepAliveTime(long value) {
            if (value < 0) throw new IllegalArgumentException("keepAliveTime cannot be negative");
            keepAliveTime = value;
            if (isReady()) executor.setKeepAliveTime(value, TimeUnit.SECONDS);
        }

        public synchronized void setMaxPoolSize(int value) {
            if (value <= 0) throw new IllegalArgumentException("maxPoolSize must be positive");
            maxPoolSize = value;
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

        public int getTrackedTaskCount() {
            return tasks.size() + onEveryTasks.size() + (onEvery == null ? 0 : 1);
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
