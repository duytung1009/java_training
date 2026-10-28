package com.training.ecommerce.infrastructure.persistence.jdbc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bản rút gọn của helper S06 (module domain): chạy N task cùng lúc sau một cổng xuất phát. */
final class Concurrently {

    private Concurrently() {
    }

    /** Kết quả theo thứ tự task: {@code null} = chạy xong, khác null = exception task đã ném. */
    static List<Throwable> run(int threads, IntFunction<Runnable> taskFor) throws Exception {
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Throwable>> futures = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                Runnable task = taskFor.apply(i);
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        task.run();
                        return null;
                    } catch (RuntimeException e) {
                        return e;
                    }
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS), "các thread không sẵn sàng kịp");
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "task chạy quá 30 giây (treo?)");
        } finally {
            pool.shutdownNow();
        }
        List<Throwable> results = new ArrayList<>();
        for (Future<Throwable> f : futures) {
            results.add(f.get());
        }
        return results;
    }
}
