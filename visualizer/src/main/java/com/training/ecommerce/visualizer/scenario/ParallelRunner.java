package com.training.ecommerce.visualizer.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Chạy các task song song, mỗi task một thread, cho xuất phát cùng lúc rồi chờ xong hết.
 * Executor do class này tạo và đóng (try-with-resources) nên không rò thread.
 */
final class ParallelRunner {

    private ParallelRunner() {
    }

    /**
     * @throws IllegalArgumentException nếu tasks null hoặc rỗng
     * @throws IllegalStateException    nếu một task ném exception ngoài dự kiến hoặc bị ngắt
     */
    static void runTogether(List<Runnable> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            throw new IllegalArgumentException("tasks không được rỗng");
        }
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(tasks.size())) {
            for (Runnable task : tasks) {
                futures.add(pool.submit(() -> {
                    awaitStart(startGate);
                    task.run();
                }));
            }
            startGate.countDown();
        }   // close() chờ mọi task xong
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Bị ngắt khi chờ task", e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("Task lỗi ngoài dự kiến", e.getCause());
            }
        }
    }

    private static void awaitStart(CountDownLatch startGate) {
        try {
            startGate.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Bị ngắt trước khi xuất phát", e);
        }
    }
}
