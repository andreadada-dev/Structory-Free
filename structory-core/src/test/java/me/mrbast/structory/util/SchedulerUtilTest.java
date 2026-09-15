package me.mrbast.structory.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulerUtilTest {

    @Test
    void asyncExecutorUsesNamedWorkersAndReportsDiagnostics() throws Exception {
        SchedulerUtil.AsyncExecutor executor = new SchedulerUtil.AsyncExecutor();
        executor.setCorePoolSize(1);
        executor.init();

        try {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> threadName = new AtomicReference<>();

            executor.async(() -> {
                threadName.set(Thread.currentThread().getName());
                latch.countDown();
            });

            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertTrue(threadName.get().startsWith("Structory-Async-"));
            assertTrue(executor.isRunning());
            assertEquals(0, executor.getQueueSize());
            assertTrue(executor.getCompletedTaskCount() >= 1);
        } finally {
            executor.shutdown();
        }
    }
}
