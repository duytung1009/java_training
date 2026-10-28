// SPEC S06 – helper test sinh bởi java-start-backlog 28/10, dùng chung cho các test concurrency của package inventory.
package com.training.ecommerce.domain.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class Concurrently {

    private Concurrently() {
    }

    /**
     * Chạy {@code threads} task cùng lúc: mọi thread chờ ở cổng rồi mới chạy.
     * Kết quả theo đúng thứ tự task: {@code null} = chạy xong, khác null = exception task đã ném.
     */
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
            pool.shutdownNow();     // không dùng try-with-resources: close() sẽ chờ mãi nếu task bị treo
        }
        List<Throwable> results = new ArrayList<>();
        for (Future<Throwable> f : futures) {
            results.add(f.get());
        }
        return results;
    }
}
