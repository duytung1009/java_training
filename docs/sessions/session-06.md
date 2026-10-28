# S06 – Multi-threading & Concurrency

- **Module:** – (tự nghiên cứu + tự làm slide trình bày trên lớp) · **Ngày T7:** 24/10 · **Slide:** `slides/session-6.html` (Tùng trình bày)
- **Demo:** `java playground/session06/ConcurrencyPitfalls.java` (cần JDK 21+ vì virtual thread; try-with-resources cho `ExecutorService` từ Java 19)

## 1. Tóm tắt 1 phút
1. **Thread** là một luồng chạy code trong process. Các thread dùng **chung heap**, nhưng mỗi thread có **stack riêng**. Chạy song song thật trên nhiều core nên nhanh hơn, nhưng dữ liệu dùng chung sẽ bị "đụng" nhau.
2. Task được viết dưới dạng `Runnable` (không trả kết quả) hoặc `Callable<T>` (trả kết quả, được ném checked exception). Muốn chạy trên thread mới thì phải gọi `start()`; gọi `run()` chỉ là gọi method bình thường. Trong code thật, **giao task cho `ExecutorService`** và lấy kết quả qua `Future`, không tự `new Thread()`.
3. Lỗi kinh điển là **race condition**: `if (stock > 0) stock--` và `count++` đều là nhiều bước đọc-sửa-ghi. Cách sửa: `synchronized` (mọi thread phải dùng **chung một lock**), `Atomic*` (check + update gộp trong **một lần gọi**, ví dụ `getAndUpdate`), hoặc `ReentrantLock` khi cần `tryLock`/timeout.
4. `volatile` chỉ đảm bảo **visibility** (thread khác thấy giá trị mới), **không** đảm bảo atomic. **Deadlock** cần đủ 4 điều kiện Coffman; dễ phá nhất là circular wait, bằng cách luôn khóa theo **một thứ tự cố định**.
5. **Virtual thread** (Java 21) là thread giá rẻ do JVM quản lý: tốt cho việc chờ IO, không cần pool, nhưng không làm việc nặng CPU nhanh hơn. Giới hạn tài nguyên phía sau (DB, API) bằng `Semaphore`.

## 2. Keyword checklist

### Process, thread & tạo task
- [ ] **Process vs thread**: process có vùng nhớ riêng; thread trong cùng process dùng chung heap, mỗi thread có stack riêng (mặc định khoảng 1 MB với platform thread).
- [ ] **extends Thread / implements Runnable / lambda / Callable&lt;T&gt;**: 4 cách đặt code của task. `Runnable.run()` trả `void`, không ném checked exception; `Callable.call()` trả `T` và được `throws Exception`. `new Thread(...)` không nhận `Callable`.
- [ ] **start() vs run()**: `start()` tạo thread mới rồi thread đó gọi `run()`. Gọi `run()` trực tiếp thì code chạy ngay trên thread hiện tại.
- [ ] **ExecutorService / thread pool**: tạo sẵn một số thread cố định và dùng lại cho nhiều task (`Executors.newFixedThreadPool(n)`). Task xếp hàng chờ thread rảnh.
- [ ] **submit → Future**: `future.get()` chặn thread đang gọi tới khi task xong (giống `await` nhưng chặn thật). Task ném exception thì `get()` ném **`ExecutionException`** bọc exception gốc (`getCause()`).
- [ ] **invokeAll(tasks)**: submit cả danh sách `Callable` và chờ tất cả xong, tương tự `Promise.all`.
- [ ] **shutdown / close**: quên đóng pool thì các thread của pool vẫn sống và ứng dụng không tắt được. `ExecutorService` là `AutoCloseable` từ **Java 19**: `close()` gọi `shutdown()` rồi chờ mọi task xong. Đã shutdown mà vẫn submit thì bị `RejectedExecutionException`.
- [ ] **ScheduledExecutorService**: `schedule`, `scheduleAtFixedRate`, tương đương `setTimeout`/`setInterval`.

### Lifecycle & các method của Thread
- [ ] **6 state**: `NEW` → `RUNNABLE` → `TERMINATED`; nhánh phụ `BLOCKED` (chờ lock của `synchronized`), `WAITING` (`join()`, `wait()`), `TIMED_WAITING` (`sleep(ms)`, `join(ms)`).
- [ ] **Start một lần**: thread đã `TERMINATED` mà gọi `start()` lần nữa thì ném `IllegalThreadStateException` (runtime, compiler không bắt được). Muốn chạy lại thì tạo thread mới, hoặc submit lại task.
- [ ] **sleep(ms)**: thread tạm dừng nhưng **vẫn giữ lock** đang cầm.
- [ ] **join() / join(ms)**: chờ thread kia chạy xong.
- [ ] **interrupt()**: chỉ bật cờ "yêu cầu dừng", không ép dừng. Thread đang `sleep`/`join`/`wait` sẽ ném `InterruptedException` ngay và **cờ bị xóa**; thread đang chạy bình thường phải tự kiểm `isInterrupted()`.
- [ ] **Không nuốt InterruptedException**: catch xong thì gọi `Thread.currentThread().interrupt()` để bật lại cờ, hoặc ném tiếp.
- [ ] **Thread.stop()**: đã bị vô hiệu hóa (từ Java 20 gọi là ném `UnsupportedOperationException`), vì nó cắt ngang thread và để dữ liệu dở dang.
- [ ] **Thread.currentThread().getName()**: hay dùng để ghi log theo thread.

### Synchronization
- [ ] **Race condition**: kết quả phụ thuộc thứ tự chạy ngẫu nhiên giữa các thread, nên lỗi lúc có lúc không. `count++` gồm 3 bước (đọc → cộng → ghi).
- [ ] **Critical section**: đoạn đọc-sửa-ghi dữ liệu dùng chung; mỗi lúc chỉ được một thread chạy.
- [ ] **Check-then-act**: kiểm tra và sửa phải nằm **trong cùng một lock**. Kiểm tra bên ngoài lock rồi sửa bên trong lock vẫn bị race.
- [ ] **synchronized & monitor**: method thường khóa trên `this`, method `static synchronized` khóa trên `X.class`, `synchronized (obj)` khóa trên `obj`. Lock tự nhả khi ra khỏi khối, kể cả khi có exception. Hai thread khóa trên hai object khác nhau thì **không chặn nhau**.
- [ ] **Reentrant**: thread đang giữ lock được lấy lại chính lock đó (gọi method `synchronized` khác trên cùng object) mà không tự chặn mình. `ReentrantLock` đếm số lần (`getHoldCount`), phải `unlock()` đủ bấy nhiêu lần.
- [ ] **AtomicInteger / AtomicLong**: mỗi method là atomic, chạy bằng **CAS** (compare-and-set: bị chen thì thử lại). `get()` rồi `decrementAndGet()` là **hai** lần gọi nên vẫn race. Phải gộp vào một lần gọi: `getAndUpdate(s -> s > 0 ? s - 1 : s)`, `compareAndSet`. Thread-safe class ≠ thread-safe logic.
- [ ] **incrementAndGet vs getAndIncrement**: một bên trả giá trị sau khi cộng, một bên trả giá trị trước khi cộng.
- [ ] **ReentrantLock**: `lock()` + `try { … } finally { unlock(); }`. Có thêm `tryLock(timeout)`, `lockInterruptibly()`, fair lock (`new ReentrantLock(true)`).
- [ ] **Chọn công cụ**: bắt đầu bằng `synchronized`; chỉ có một biến số thì dùng `Atomic`; cần `tryLock`/timeout thì dùng `ReentrantLock`.
- [ ] **ConcurrentHashMap**: Map thread-safe. Muốn cập nhật atomic theo key thì dùng `merge`/`compute`, không dùng `get` rồi `put`.

### Volatile & visibility
- [ ] **Visibility problem**: thread ghi một biến, thread khác có thể không bao giờ thấy giá trị mới, vì CPU cache và JIT được phép giữ giá trị cũ (ví dụ vòng `while (running) {}` bị tối ưu thành vòng lặp vô hạn).
- [ ] **volatile**: mọi lần đọc đều thấy lần ghi mới nhất. Hợp với cờ trạng thái do một thread ghi, nhiều thread đọc. **Không atomic**: `volatile int count; count++` vẫn race.
- [ ] **Happens-before** (đọc thêm): Java Memory Model đảm bảo visibility qua các quan hệ happens-before: unlock → lock trên cùng monitor, ghi → đọc `volatile`, `start()`, `join()`. Nói "volatile đi thẳng xuống RAM" chỉ là cách hình dung đơn giản.

### Deadlock
- [ ] **Deadlock**: các thread chờ lock của nhau thành vòng nên đứng im mãi. Java **không** ném exception, chương trình chỉ treo. Phát hiện bằng thread dump (`jstack`, VisualVM), sẽ thấy dòng "Found one Java-level deadlock"; trong code thì dùng `ThreadMXBean.findDeadlockedThreads()`.
- [ ] **4 điều kiện Coffman**: mutual exclusion, hold and wait, no preemption, circular wait. Phải có đủ cả 4.
- [ ] **Lock ordering**: luôn khóa object có id nhỏ hơn trước, bất kể chiều chuyển, để phá circular wait.
- [ ] **Phòng deadlock**: lock ordering; `tryLock` có timeout; giữ lock ít và ngắn (không gọi DB/API trong lock); dùng thư viện có sẵn (`ConcurrentHashMap`, `Atomic`, transaction của DB).

### Virtual thread (Java 21)
- [ ] **Platform thread vs virtual thread**: platform thread ứng với 1 OS thread, stack khoảng 1 MB, vài nghìn đã là nhiều. Virtual thread do JVM quản lý, stack vài KB, chạy luân phiên trên vài **carrier** thread, scale tới hàng triệu.
- [ ] **Blocking IO**: virtual thread đang chờ thì nhả carrier cho virtual thread khác, nên vẫn viết code blocking bình thường mà chi phí rẻ gần như async.
- [ ] **Thread.startVirtualThread / Executors.newVirtualThreadPerTaskExecutor()**: mỗi task một virtual thread, không cần pool.
- [ ] **Không nhanh hơn với việc nặng CPU**: số core không đổi.
- [ ] **Semaphore**: khi app không còn bị giới hạn bởi số thread, bottleneck dồn xuống DB hoặc API đối tác. Dùng `Semaphore` để giới hạn tối đa N lời gọi cùng lúc.
- [ ] **ThreadLocal**: mỗi thread giữ một bản riêng; nhân với hàng triệu virtual thread thì tốn rất nhiều bộ nhớ.
- [ ] **Pinning**: Java 21–23, virtual thread chờ bên trong `synchronized` sẽ giữ luôn carrier. Java 24+ đã sửa (JEP 491); project dùng Java 25.
- [ ] **Spring Boot**: `spring.threads.virtual.enabled=true` (từ 3.2) để Tomcat xử lý request bằng virtual thread.

## 3. Quiz tự kiểm tra
Câu 1–14 có trong demo `ConcurrencyPitfalls.java` (dòng in ra cùng số), riêng câu 7 để dạng comment vì là lỗi compile. Câu 15 là câu thiết kế. Các câu 4, 5, 6, 10 cho kết quả khác nhau tùy lần chạy và tùy máy; đáp án ghi kết quả đã chạy trên JDK 21, máy 2 core.

**Q1.** `Thread t = new Thread(() -> println(Thread.currentThread().getName()), "worker"); t.run(); t.start();` → in ra gì, theo thứ tự nào?
<details><summary>Đáp án</summary>`main` rồi `worker`. `run()` là gọi method bình thường nên lambda chạy trên thread `main`, và chạy đồng bộ nên chắc chắn in trước. Sau đó `start()` mới tạo thread mới tên `worker`.</details>

**Q2.** `t.start(); t.join(); t.start();` (task in `hi`) → chuyện gì xảy ra? State của `t` lúc đó là gì?
<details><summary>Đáp án</summary>In `hi` một lần, rồi lần `start()` thứ hai ném `IllegalThreadStateException`; state là `TERMINATED`. Một object `Thread` chỉ được `start()` một lần. Đây là lỗi runtime, compiler không bắt được. Task (`Runnable`) thì dùng lại được: tạo thread mới hoặc submit lại vào `ExecutorService`.</details>

**Q3.** Thread `sleep(10_000)` rồi in `woke`; trong `catch (InterruptedException)` in `interrupted` kèm `isInterrupted()`. Main gọi `t.start(); t.interrupt();`. In ra gì và sau bao lâu?
<details><summary>Đáp án</summary>`interrupted, isInterrupted() = false`, gần như ngay lập tức. `interrupt()` chỉ bật cờ; `sleep` thấy cờ (kể cả khi được bật trước lúc vào `sleep`) thì ném `InterruptedException` ngay và **xóa cờ**, nên `isInterrupted()` trong catch là `false`. Vì vậy phải gọi `Thread.currentThread().interrupt()` nếu muốn giữ tín hiệu dừng. `woke` không bao giờ được in.</details>

**Q4.** `static int count = 0;` hai thread, mỗi thread chạy `count++` 100.000 lần, `join` cả hai rồi in `count`. Kết quả?
<details><summary>Đáp án</summary>**Thường** nhỏ hơn 200000, và mỗi lần chạy một khác (đã chạy được `167815`; có lần ra đúng `200000`, vì "lúc đúng lúc sai" chính là đặc điểm của race condition). `count++` gồm đọc → cộng → ghi, hai thread cùng đọc một giá trị thì mất một lần cộng. Không phải lỗi compile: luật effectively final chỉ áp cho biến local, còn `count` là field static. Sửa: `AtomicInteger.incrementAndGet()` hoặc `synchronized` trên **chung một lock**.</details>

**Q5.** `class Counter { static int total; synchronized void inc() { total++; } }`. Thread A gọi `c1.inc()`, thread B gọi `c2.inc()`, mỗi thread 100.000 lần. `total` có luôn là 200000 không? Có deadlock không?
<details><summary>Đáp án</summary>Không luôn đúng (đã chạy được `196182`), và không deadlock. Method `synchronized` thường khóa trên `this`: A khóa `c1`, B khóa `c2`, hai lock khác nhau nên không chặn nhau, nhưng cả hai cùng sửa một field static. Cũng không deadlock vì không thread nào chờ lock của thread kia. Sửa: `static synchronized void inc()` (khóa `Counter.class`) hoặc `AtomicInteger`.</details>

**Q6.** Còn 1 sản phẩm, `AtomicInteger stock = new AtomicInteger(1)`. Hai khách cùng mua, lặp 2000 lần. Cách A: `if (stock.get() > 0) stock.decrementAndGet();`. Cách B: `stock.getAndUpdate(s -> s > 0 ? s - 1 : s)`. Mỗi cách bán quá (stock âm) bao nhiêu lần?
<details><summary>Đáp án</summary>Cách A bán quá vài lần (demo: 11–12 trên 2000; demo cố ý nới khoảng hở giữa check và act bằng `Thread.onSpinWait()` để dễ thấy). Cách B luôn là `0`. Atomic chỉ đảm bảo **từng method** là atomic; `get()` rồi `decrementAndGet()` là hai lần gọi, thread khác chen vào giữa được. Thread-safe class ≠ thread-safe logic. `getAndUpdate` gộp check + trừ vào một lần CAS, và trả giá trị **trước** khi cập nhật: `before > 0` nghĩa là mua được.</details>

**Q7.** Dòng nào compile lỗi?
(a) `new Thread(() -> System.out.println("ok"));`
(b) `new Thread(() -> 42);`
(c) `Callable<Integer> c = () -> 42; new Thread(c);`
(d) `Future<Integer> f = Executors.newFixedThreadPool(2).submit(() -> 42);`
<details><summary>Đáp án</summary>(b) và (c). (b): target type là `Runnable`, mà `run()` trả `void`, còn `42` không phải một câu lệnh (*lambda body is not compatible with a void functional interface*). Ngược lại, `println(...)` ở (a) là câu lệnh nên hợp lệ dù có trả giá trị hay không. (c): `Thread` không có constructor nhận `Callable`. (d): compiler chọn overload `submit(Callable)` vì lambda trả về giá trị.</details>

**Q8.** Main giữ `synchronized (lock)`, rồi start `sleeper` (`sleep(300)`) và `blocked` (`synchronized (lock) {}`), ngủ 100 ms. In state của `sleeper` trước khi start, state của cả hai lúc đó, và state của `sleeper` sau `join`.
<details><summary>Đáp án</summary>`NEW TIMED_WAITING BLOCKED TERMINATED`. `sleep(ms)` cho `TIMED_WAITING`; chờ lock của `synchronized` cho `BLOCKED` (khác `WAITING`, là trạng thái của `join()`/`wait()` không có thời hạn).</details>

**Q9.** `synchronized boolean reserve(int qty)` gọi `synchronized boolean hasStock(int qty)` trên cùng object, stock = 3. `reserve(2)` hai lần in gì? `ReentrantLock` gọi `lock()` 2 lần, `getHoldCount()` là bao nhiêu? Sau một lần `unlock()` thì `isLocked()` là gì?
<details><summary>Đáp án</summary>`true false holdCount = 2 → 1 isLocked = true → isLocked = false`. Thread đang giữ lock của `this` được vào method `synchronized` khác trên cùng `this` (reentrant), nên không tự deadlock. Lần thứ hai chỉ còn 1 nên trả `false`. `ReentrantLock` đếm số lần lock: còn `holdCount > 0` thì lock vẫn bị giữ. Vì vậy phải `unlock()` đủ số lần, và luôn đặt `unlock()` trong `finally`.</details>

**Q10.** `static boolean running = true;` (không volatile). Worker chạy `while (running) {}` rồi in `stopped`. Main ngủ 1 giây rồi gán `running = false`, `join(2000)`. Worker còn sống không?
<details><summary>Đáp án</summary>Có thể còn sống mãi (demo in `true`, không in `stopped`). Vòng lặp chỉ đọc `running` nên JIT được phép đọc một lần rồi lặp với giá trị cũ. Kết quả phụ thuộc JVM và máy. Sửa: `static volatile boolean running`. Demo đặt worker là daemon thread để JVM vẫn thoát được.</details>

**Q11.** T1 khóa `a`, ngủ 100 ms, rồi khóa `b`; T2 khóa `b`, ngủ 100 ms, rồi khóa `a`. Sau 1 giây in state của T1, T2 và số thread mà `findDeadlockedThreads()` báo.
<details><summary>Đáp án</summary>`BLOCKED BLOCKED, deadlocked threads = 2`, và không in `T1`/`T2`. Có đủ 4 điều kiện Coffman (circular wait: T1 chờ b, T2 chờ a). Java không có `DeadlockException`; chương trình chỉ treo. Thread dump hoặc `ThreadMXBean` mới cho thấy deadlock.</details>

**Q12.** `transfer(from, to, qty)` luôn khóa kho có id nhỏ hơn trước (`compareTo`). T1 chuyển HN → HCM, T2 chuyển HCM → HN, mỗi thread 100.000 lần, mỗi kho có 1000. Có xong không? Tổng tồn kho bằng bao nhiêu?
<details><summary>Đáp án</summary>`xong? true, tổng tồn kho = 2000`. Cả hai thread đều khóa `HCM` trước (vì `"HCM" < "HN"`), nên không có vòng chờ. Khóa cả hai kho thì không ai thấy trạng thái "đã trừ bên này, chưa cộng bên kia", nên tổng luôn được bảo toàn. Cần nghĩ thêm: chuyển một kho cho chính nó (`from == to`) thì sao? (Gợi ý: `synchronized` reentrant nên không treo, nhưng nghiệp vụ có nên cho phép không?)</details>

**Q13.** `Callable` ném `IllegalStateException("Kho HN mất kết nối")`, submit vào pool rồi `f.get()`. Catch được exception nào?
<details><summary>Đáp án</summary>`ExecutionException ← IllegalStateException: Kho HN mất kết nối`. Exception của task không bay thẳng ra thread gọi `get()` mà được bọc trong `ExecutionException` (checked), lấy exception gốc bằng `getCause()`. `get()` còn ném `InterruptedException` nếu thread đang chờ bị interrupt. Nếu không bao giờ gọi `get()`, exception của task bị "nuốt" im lặng.</details>

**Q14.** 10.000 task `sleep(200)` trên `newVirtualThreadPerTaskExecutor()`, so với 1.000 task `sleep(200)` trên `newFixedThreadPool(100)`. Mỗi bên mất khoảng bao lâu?
<details><summary>Đáp án</summary>Virtual thread khoảng 0.3–0.4 s; pool(100) khoảng 2.1 s. Pool chỉ chạy 100 task một lúc, nên 1.000 / 100 × 0.2 s = 2 s. Mỗi virtual thread đang ngủ nhả carrier, nên 10.000 task ngủ song song. Nếu task là tính toán nặng thì virtual thread không nhanh hơn.</details>

**Q15.** (Thiết kế) Flash sale: 100 khách cùng checkout, mỗi `Order` có nhiều `OrderLine` (nhiều product). `InventoryManager.decreaseStock(order)` phải all-or-nothing. Chọn cách khóa nào: `synchronized` cả method trên `InventoryManager`, một lock cho mỗi product, hay `AtomicInteger` cho stock từng product? Cái giá của mỗi cách?
<details><summary>Đáp án</summary>`AtomicInteger` cho từng product **không đủ**: order có nhiều dòng thì check-all-rồi-trừ-all là nhiều bước trên nhiều biến, nên vẫn race giữa các product (đơn A trừ áo xong, chưa trừ quần thì đơn B chen vào). `synchronized` cả method (một lock chung) là đơn giản và đúng nhất, nhưng mọi checkout xếp hàng nối tiếp nhau, kể cả các đơn không liên quan. Lock theo từng product cho phép các đơn khác product chạy song song, nhưng mỗi đơn phải khóa nhiều product, nên phải khóa theo **thứ tự id cố định** để không deadlock (giống Q12). Gợi ý: bắt đầu bằng `synchronized`, đo rồi mới tối ưu; giữ lock ngắn, không gọi IO trong lock. Khi lên DB thật thì để DB lo bằng transaction / `SELECT … FOR UPDATE` / optimistic lock.</details>

## 4. Bài tập & homework
> Tham khảo, **không tính tiến độ**. Chỉ phần đưa vào ecommerce (mục 5) mới được tính.
- [x] Tự làm và trình bày slide S06 (24/10)
- [ ] Chạy `ConcurrencyPitfalls.java`, đoán output trước rồi mới chạy; chạy vài lần để thấy câu 4, 5, 6 thay đổi
- [ ] Chạy câu 11 rồi lấy thread dump bằng `jstack <pid>` hoặc VisualVM, tìm dòng "Found one Java-level deadlock"
- [ ] Đọc thêm (tùy chọn): Java Memory Model và happens-before (JLS §17.4); JEP 444 (Virtual Threads); JEP 491 (pinning với `synchronized`)

## 5. Áp dụng vào ecommerce
Xem `docs/ecommerce-backlog.md` → **S06**. Đây là 3 bài tập cuối slide: chống oversell khi nhiều thread cùng trừ kho, chuyển hàng giữa hai kho không deadlock, và gọi song song 3 nguồn cho trang chi tiết đơn.

## 6. Ghi chú cá nhân / câu hỏi mang lên lớp
- **Slide "Bài tập của Tùng"** gọi tên `InventoryService.reserve()`, nhưng project đã có `InventoryManager.decreaseStock(Order)` (all-or-nothing, S04–S05). Backlog S06 làm thread-safe trên class đang có thay vì tạo service mới. Tên method (`reserve` hay giữ `decreaseStock`) chốt ở spec.
- **Slide ví dụ 1 (flash sale)** chỉ có một biến `stock`. Thực tế một order có nhiều product, nên `AtomicInteger` từng product không giữ được all-or-nothing (xem Q15). Hỏi mentor: với domain in-memory thì một lock chung có chấp nhận được không, hay nên làm lock theo product + lock ordering?
- **Slide ví dụ 2 (transfer)** so sánh `getId()` bằng `<`, tức là id kiểu số. `Product` trong project dùng id `String`, nên `Warehouse` cũng dùng `String` và so bằng `compareTo`. Ví dụ chưa xử lý `from == to` hoặc hai kho trùng id. Ví dụ ném `IllegalStateException`, còn project đã có `OutOfStockException` (S04): nên dùng lại.
- **Slide ví dụ 3 (`loadDetail`)** khai báo `throws Exception` quá rộng, và nếu một `get()` ném lỗi thì `close()` vẫn chờ 2 task còn lại chạy xong, không hủy chúng. Hỏi mentor: nên unwrap `ExecutionException` thành exception nghiệp vụ không, và có nên hủy task còn lại không (structured concurrency, JEP 505, vẫn là preview ở Java 25)?
- **Slide volatile**: "buộc đọc/ghi qua main memory" là cách hình dung đơn giản. Spec thật là happens-before của JMM. Đã ghi chú trong keyword checklist.
- **Slide Atomic**: "không dùng lock nên thường nhanh hơn synchronized" chỉ đúng khi tranh chấp thấp. Tranh chấp cao thì CAS phải thử lại nhiều lần; biến đếm có nhiều thread cùng ghi thì `LongAdder` tốt hơn.
- **Pinning**: project dùng Java 25, nên pinning do `synchronized` (Java 21–23) không còn là vấn đề (JEP 491). Pinning vẫn xảy ra khi gọi native/JNI.
- **Test concurrency**: test 100 thread cùng mua thì không chứng minh được là code không có race (test xanh có thể chỉ là may mắn). Hỏi mentor xem có cần chạy lặp nhiều lần (`@RepeatedTest`) hay dùng công cụ như jcstress không. Test đo thời gian (song song ≈ nguồn chậm nhất) dễ flaky trên CI.

## 7. Feedback review T4
- 
