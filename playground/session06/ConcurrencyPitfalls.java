import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * S06 – Multi-threading & Concurrency.
 * Chạy: java playground/session06/ConcurrencyPitfalls.java   (cần JDK 21+ vì virtual thread;
 *       try-with-resources cho ExecutorService từ Java 19)
 *
 * ĐOÁN output từng dòng trước khi chạy! Số đầu dòng khớp với số câu quiz trong docs/sessions/session-06.md.
 * Câu 7 không có output (lỗi compile, xem comment), câu 15 là câu thiết kế.
 * Các câu 4, 5, 6, 10 là race / visibility: kết quả có thể khác nhau giữa các lần chạy và giữa các máy.
 */
public class ConcurrencyPitfalls {

    // ---------- Câu 4 ----------
    static int count = 0;

    // ---------- Câu 5 ----------
    static class Counter {
        static int total = 0;

        synchronized void inc() {        // lock = this → mỗi instance một lock
            total++;
        }
    }

    // ---------- Câu 9 ----------
    static class Inventory {
        private int stock = 3;

        synchronized boolean reserve(int qty) {
            if (!hasStock(qty)) {        // gọi method synchronized khác trên cùng this
                return false;
            }
            stock -= qty;
            return true;
        }

        synchronized boolean hasStock(int qty) {
            return stock >= qty;
        }
    }

    // ---------- Câu 10 ----------
    static boolean running = true;       // cố ý KHÔNG volatile

    // ---------- Câu 12 ----------
    static class Warehouse {
        final String id;
        int stock;

        Warehouse(String id, int stock) {
            this.id = id;
            this.stock = stock;
        }
    }

    static void transfer(Warehouse from, Warehouse to, int qty) {
        Warehouse first = from.id.compareTo(to.id) < 0 ? from : to;   // lock ordering theo id
        Warehouse second = first == from ? to : from;
        synchronized (first) {
            synchronized (second) {
                if (from.stock >= qty) {
                    from.stock -= qty;
                    to.stock += qty;
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        q1RunVsStart();
        q2StartTwice();
        q3InterruptSleeping();
        q4LostUpdate();
        q5DifferentLocks();
        q6AtomicCheckThenAct();
        // q7: lỗi compile, xem comment bên dưới
        q8States();
        q9Reentrant();
        q10Visibility();
        q11Deadlock();
        q12LockOrdering();
        q13FutureException();
        q14VirtualVsPool();
    }

    static void q1RunVsStart() throws InterruptedException {
        Thread t = new Thread(() -> System.out.println("1: " + Thread.currentThread().getName()), "worker");
        t.run();      // gọi method bình thường
        t.start();    // tạo thread mới
        t.join();
    }

    static void q2StartTwice() throws InterruptedException {
        Thread t = new Thread(() -> System.out.println("2: hi"));
        t.start();
        t.join();
        try {
            t.start();
        } catch (IllegalThreadStateException e) {
            System.out.println("2: " + e.getClass().getSimpleName() + ", state = " + t.getState());
        }
    }

    static void q3InterruptSleeping() throws InterruptedException {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(10_000);
                System.out.println("3: woke");
            } catch (InterruptedException e) {
                System.out.println("3: interrupted, isInterrupted() = " + Thread.currentThread().isInterrupted());
            }
        });
        long start = System.nanoTime();
        t.start();
        t.interrupt();
        t.join();
        System.out.println("3: sau " + ((System.nanoTime() - start) / 1_000_000_000) + " giây");
    }

    static void q4LostUpdate() throws InterruptedException {
        Runnable task = () -> {
            for (int i = 0; i < 100_000; i++) {
                count++;
            }
        };
        Thread a = new Thread(task);
        Thread b = new Thread(task);
        a.start();
        b.start();
        a.join();
        b.join();
        System.out.println("4: count = " + count + " (kỳ vọng 200000)");
    }

    static void q5DifferentLocks() throws InterruptedException {
        Counter c1 = new Counter();
        Counter c2 = new Counter();
        Thread a = new Thread(() -> { for (int i = 0; i < 100_000; i++) c1.inc(); });
        Thread b = new Thread(() -> { for (int i = 0; i < 100_000; i++) c2.inc(); });
        a.start();
        b.start();
        a.join();
        b.join();
        System.out.println("5: total = " + Counter.total + " (kỳ vọng 200000)");
    }

    /** 2 khách cùng mua sản phẩm cuối, lặp 2000 lần; đếm số lần bán quá (stock < 0). */
    static void q6AtomicCheckThenAct() throws InterruptedException {
        int oversoldWrong = 0;
        int oversoldRight = 0;
        for (int trial = 0; trial < 2000; trial++) {
            AtomicInteger wrong = new AtomicInteger(1);
            runTogether(() -> {
                if (wrong.get() > 0) {
                    Thread.onSpinWait();         // nới khoảng hở giữa check và act cho dễ thấy
                    wrong.decrementAndGet();
                }
            });
            if (wrong.get() < 0) oversoldWrong++;

            AtomicInteger right = new AtomicInteger(1);
            runTogether(() -> right.getAndUpdate(s -> s > 0 ? s - 1 : s));
            if (right.get() < 0) oversoldRight++;
        }
        System.out.println("6: get()+decrementAndGet() bán quá " + oversoldWrong + "/2000 lần; getAndUpdate bán quá "
                + oversoldRight + "/2000 lần");
    }

    /** Chạy 2 thread cùng lúc: cả hai chờ ở cổng (CountDownLatch) rồi mới chạy. */
    static void runTogether(Runnable action) throws InterruptedException {
        CountDownLatch gate = new CountDownLatch(1);
        Runnable r = () -> {
            try {
                gate.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            action.run();
        };
        Thread t1 = new Thread(r);
        Thread t2 = new Thread(r);
        t1.start();
        t2.start();
        gate.countDown();
        t1.join();
        t2.join();
    }

    /*
     * Câu 7: dòng nào compile lỗi?
     *   (a) new Thread(() -> System.out.println("ok"));
     *   (b) new Thread(() -> 42);
     *   (c) Callable<Integer> c = () -> 42;  new Thread(c);
     *   (d) ExecutorService ex = Executors.newFixedThreadPool(2);  Future<Integer> f = ex.submit(() -> 42);
     * → (b) lỗi: Runnable.run() trả void, mà `42` không phải câu lệnh (lambda body is not compatible with a void functional interface).
     * → (c) lỗi: Thread không có constructor nhận Callable.
     * → (a), (d) compile được; (d) chọn overload submit(Callable) vì lambda trả về giá trị.
     */

    static void q8States() throws InterruptedException {
        Object lock = new Object();
        Thread sleeper = new Thread(() -> sleep(300));
        Thread blocked = new Thread(() -> { synchronized (lock) { } });
        System.out.print("8: " + sleeper.getState());
        synchronized (lock) {                 // main giữ lock
            sleeper.start();
            blocked.start();
            sleep(100);
            System.out.print(" " + sleeper.getState() + " " + blocked.getState());
        }
        sleeper.join();
        blocked.join();
        System.out.println(" " + sleeper.getState());
    }

    static void q9Reentrant() {
        Inventory inv = new Inventory();
        System.out.print("9: " + inv.reserve(2) + " " + inv.reserve(2));
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        lock.lock();
        System.out.print(" holdCount = " + lock.getHoldCount());
        lock.unlock();
        System.out.print(" → " + lock.getHoldCount() + " isLocked = " + lock.isLocked());
        lock.unlock();
        System.out.println(" → isLocked = " + lock.isLocked());
    }

    static void q10Visibility() throws InterruptedException {
        Thread worker = new Thread(() -> {
            while (running) {
                // vòng lặp rỗng, chỉ đọc running
            }
            System.out.println("10: stopped");
        });
        worker.setDaemon(true);                // để JVM vẫn thoát được nếu worker treo
        worker.start();
        Thread.sleep(1000);
        running = false;
        worker.join(2000);
        System.out.println("10: worker còn sống sau 2 giây? " + worker.isAlive());
    }

    static void q11Deadlock() throws InterruptedException {
        Object a = new Object();
        Object b = new Object();
        Thread t1 = new Thread(() -> { synchronized (a) { sleep(100); synchronized (b) { System.out.println("11: T1"); } } });
        Thread t2 = new Thread(() -> { synchronized (b) { sleep(100); synchronized (a) { System.out.println("11: T2"); } } });
        t1.setDaemon(true);
        t2.setDaemon(true);
        t1.start();
        t2.start();
        t1.join(1000);
        long[] deadlocked = ManagementFactory.getThreadMXBean().findDeadlockedThreads();
        System.out.println("11: " + t1.getState() + " " + t2.getState() + ", deadlocked threads = "
                + (deadlocked == null ? 0 : deadlocked.length));
    }

    static void q12LockOrdering() throws InterruptedException {
        Warehouse hn = new Warehouse("HN", 1000);
        Warehouse hcm = new Warehouse("HCM", 1000);
        Thread t1 = new Thread(() -> { for (int i = 0; i < 100_000; i++) transfer(hn, hcm, 1); });
        Thread t2 = new Thread(() -> { for (int i = 0; i < 100_000; i++) transfer(hcm, hn, 1); });
        t1.start();
        t2.start();
        t1.join(5000);
        t2.join(5000);
        System.out.println("12: xong? " + !(t1.isAlive() || t2.isAlive()) + ", tổng tồn kho = " + (hn.stock + hcm.stock));
    }

    static void q13FutureException() throws InterruptedException {
        try (ExecutorService ex = Executors.newFixedThreadPool(2)) {
            Callable<Integer> countStock = () -> {
                throw new IllegalStateException("Kho HN mất kết nối");
            };
            Future<Integer> f = ex.submit(countStock);
            try {
                f.get();
            } catch (ExecutionException e) {
                System.out.println("13: " + e.getClass().getSimpleName() + " ← " + e.getCause().getClass().getSimpleName()
                        + ": " + e.getCause().getMessage());
            }
        }
    }

    static void q14VirtualVsPool() {
        long start = System.nanoTime();
        try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 10_000; i++) {
                ex.submit(() -> sleep(200));
            }
        }
        long virtualMs = (System.nanoTime() - start) / 1_000_000;

        start = System.nanoTime();
        try (ExecutorService ex = Executors.newFixedThreadPool(100)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 1_000; i++) {
                futures.add(ex.submit(() -> sleep(200)));
            }
        }
        long poolMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("14: 10.000 task virtual ≈ " + roundToTenth(virtualMs) + " s; 1.000 task pool(100) ≈ "
                + roundToTenth(poolMs) + " s");
    }

    static String roundToTenth(long ms) {
        return String.valueOf(Math.round(ms / 100.0) / 10.0);
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
