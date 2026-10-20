# S05 – Functional Programming, Lambda & Stream API

- **Module:** – (format mới: tự nghiên cứu + tự làm slide trình bày trên lớp) · **Ngày T7:** 17/10 · **Slide:** [Functional Programming, Lambda & Stream API](https://claude.ai/artifact/7P9Vr9WS3AvWsCdYVFUjFS) (Tùng trình bày)
- **Demo:** `java playground/session05/FunctionalStreamPitfalls.java` (cần JDK 16+ vì `Stream.toList()`) · pretest cũ (01/10): `playground/session04/CollectionsStreamDeepDive.java`

## 1. Tóm tắt 1 phút
1. **Functional Programming** là tư duy *mô tả kết quả muốn có* (declarative) thay vì *mô tả từng bước* (imperative). Hai nguyên tắc chính: **pure function** (không side effect, deterministic) và **immutability**.
2. Java **không có function type**. Hàm được "gói" thành object của một **functional interface**: interface có đúng 1 abstract method (SAM). 4 interface cốt lõi là `Supplier`, `Consumer`, `Function`, `Predicate`.
3. **Lambda** là cách viết ngắn để implement functional interface. Kiểu của lambda do **target type** quyết định. Lambda chỉ capture biến local **effectively final**, và `this` bên trong lambda là object bao ngoài. Lambda chỉ gọi đúng một method thì viết gọn thành **method reference**.
4. **Stream** là pipeline: source → intermediate (lazy) → đúng 1 terminal. Phần tử đi **theo chiều dọc** qua cả pipeline; short-circuit dừng sớm. Stream **chỉ dùng được một lần** và không sửa collection nguồn.
5. **Collectors** (`groupingBy`, `partitioningBy`, `toMap` + merge function, `reducing` cho `BigDecimal`) và **Optional** (dùng `orElse`/`orElseGet`/`orElseThrow`, tránh `get()` trần) là công cụ chính để làm báo cáo.

## 2. Keyword checklist

### Functional Programming
- [ ] **Imperative vs Declarative**: for + biến tạm bị sửa liên tục so với pipeline `filter → map → reduce` đọc như một câu.
- [ ] **Pure function**: (1) không side effect, (2) deterministic, tức cùng input cho cùng output. Đọc giờ hệ thống, random, hay sửa field đều làm hàm mất pure. Mục tiêu là gom side effect về service/repository, còn logic tính toán giữ pure.
- [ ] **Side effect**: mọi tác động ra ngoài phạm vi hàm: DB, API, log, email, sửa field, sửa object được truyền vào.
- [ ] **Immutability**: muốn "sửa" thì tạo object mới. Công cụ: `record` (Java 16), field `final`, `List.of`/`Map.of`, `Stream.toList()` (Java 16, unmodifiable). Lưu ý `final List` vẫn `add` được, vì `final` chỉ chặn gán lại reference.
- [ ] **BigDecimal / String immutable**: `total.add(x);` mà không gán lại thì `total` **không đổi**.
- [ ] **First-class function**: hàm là giá trị: gán vào biến, truyền làm tham số, trả về, lưu trong `Map`.
- [ ] **Higher-order function**: nhận hoặc trả về hàm (`filter`, `map`, `Comparator.comparing`, hàm `priceAbove(min)` trả về `Predicate`).

### Functional interface
- [ ] **Functional interface / SAM**: đúng 1 abstract method. Được có bao nhiêu `default`/`static` cũng được; method public của `Object` (`equals`, `toString`) không tính.
- [ ] **@FunctionalInterface**: không bắt buộc, nhưng có thì compiler báo lỗi khi interface có nhiều hơn 1 abstract method.
- [ ] **Supplier&lt;T&gt; `get()`**: chỉ có output, dùng để trì hoãn việc tạo giá trị (lazy), ví dụ `orElseGet`.
- [ ] **Consumer&lt;T&gt; `accept(t)`**: chỉ có input, gần như luôn có side effect (`forEach`).
- [ ] **Function&lt;T,R&gt; `apply(t)`**: biến đổi dữ liệu (`map`, entity → DTO).
- [ ] **Predicate&lt;T&gt; `test(t)`**: điều kiện (`filter`, `removeIf`).
- [ ] **Bi… / …Operator / primitive**: `BiFunction`, `BiConsumer`, `BiPredicate`; `UnaryOperator` (input và output cùng kiểu), `BinaryOperator` (gộp 2 thành 1, ví dụ `BigDecimal::add`); `IntPredicate`, `ToIntFunction`, `IntStream` dùng để tránh autoboxing.
- [ ] **Composition**: `f.andThen(g)` = g(f(x)) · `f.compose(g)` = f(g(x)) · `Function.identity()` · `and`/`or`/`negate` · `Predicate.not` (Java 11) · `Comparator.comparing().thenComparing().reversed()`.

### Lambda
- [ ] **Cú pháp**: `() -> 42` · `x -> x * 2` · `(a, b) -> a + b` · `(var a, var b) -> …` (Java 11) · block body `{ …; return x; }`. Không được trộn kiểu khai báo, ví dụ `(var a, b)` là lỗi compile.
- [ ] **Target type**: lambda không có kiểu riêng. Cùng một lambda có thể là `Function<String,Integer>` hoặc `ToIntFunction<String>`.
- [ ] **Effectively final**: biến local dùng trong lambda không được gán lại. Field của class thì sửa được.
- [ ] **this trong lambda** là object bao ngoài, khác anonymous class (anonymous class có `this` riêng, ví dụ `Cart$1`).
- [ ] **Checked exception trong lambda**: `Function`/`Consumer` không khai báo `throws`, nên phải try/catch bên trong hoặc tự viết interface có `throws`.
- [ ] **invokedynamic**: lambda không sinh file `Outer$1.class`. Xem bằng `javap -c -p`.
- [ ] **Method reference**: static (`Integer::parseInt`), bound (`System.out::println`), unbound (`Product::getId`, tham số đầu là object gọi method), constructor (`ArrayList::new`).

### Stream API
- [ ] **Pipeline**: source (`list.stream()`, `Stream.of`, `IntStream.range`) → intermediate × n → terminal × 1.
- [ ] **Lazy**: không có terminal thì lambda trong `filter`/`map` không bao giờ chạy.
- [ ] **Xử lý theo chiều dọc**: từng phần tử đi hết pipeline rồi mới tới phần tử sau (khác JS: mỗi bước tạo một mảng mới).
- [ ] **Intermediate stateless** (`filter`, `map`, `flatMap`, `peek`, `mapToInt`) **vs stateful** (`sorted`, `distinct`, `limit`, `skip`). `sorted` phải đọc hết stream.
- [ ] **Terminal**: dạng reduce (`collect`, `toList`, `reduce`, `count`, `max`) vs dạng side-effect (`forEach`, `forEachOrdered`).
- [ ] **Short-circuit**: `limit`, `anyMatch`, `allMatch`, `noneMatch`, `findFirst`, `findAny`. Nhờ đó chạy được trên stream vô hạn (`Stream.iterate(...).limit(n)`).
- [ ] **Single-use**: gọi terminal lần 2 thì ném `IllegalStateException: stream has already been operated upon or closed`.
- [ ] **map vs flatMap**: `flatMap` làm phẳng `Stream<List<X>>` thành `Stream<X>`; hàm truyền vào phải trả về một `Stream`.
- [ ] **Stream.toList() (Java 16) vs Collectors.toList()**: bản đầu unmodifiable; bản sau hiện tại là `ArrayList` nhưng spec không đảm bảo.
- [ ] **parallelStream**: dùng `ForkJoinPool.commonPool`; chỉ đáng dùng khi data lớn, CPU-bound, và đã đo trước. Không dùng cho tác vụ IO.

### Collectors & Optional
- [ ] **groupingBy(key) / groupingBy(key, downstream)**: kết quả là `Map<K, List<T>>` hoặc `Map<K, R>` (`counting`, `summingInt`, `mapping`, `reducing`).
- [ ] **reducing(identity, mapper, op)**: `BigDecimal` không có `summingBigDecimal`, nên dùng `reducing(BigDecimal.ZERO, …, BigDecimal::add)`.
- [ ] **partitioningBy(predicate)**: luôn có đủ 2 key `true` và `false`, kể cả khi một bên rỗng.
- [ ] **toMap(k, v)**: gặp key trùng thì ném `IllegalStateException: Duplicate key`. Cần merge function (tham số thứ 3); value `null` thì ném NPE; bản 4 tham số cho chọn kiểu Map (`LinkedHashMap::new`).
- [ ] **Optional**: kiểu trả về của `findFirst`, `min`, `max`, `reduce(op)`. Dùng `orElse` / `orElseGet` / `orElseThrow` / `map` / `ifPresent`; tránh `get()` trần. Chỉ dùng làm kiểu trả về, không dùng làm field hay tham số.
- [ ] **orElse vs orElseGet**: `orElse(x)` luôn tính `x`; `orElseGet(supplier)` chỉ gọi supplier khi Optional rỗng.
- [ ] **Map.Entry.comparingByValue() + type witness**: `Map.Entry.<String, Integer>comparingByValue().reversed()`, vì gọi `.reversed()` thì compiler không tự suy ra K, V.

## 3. Quiz tự kiểm tra
Câu 1–14 có trong demo `FunctionalStreamPitfalls.java` (dòng in ra cùng số), riêng câu 3 để dạng comment vì là lỗi compile. Câu 15 là câu thiết kế.

**Q1.** `PriceRule` có `BigDecimal apply(BigDecimal)`, `boolean equals(Object)`, một default method `then(...)` và một static method `none()`. Interface này có hợp lệ với `@FunctionalInterface` không? Nếu hợp lệ thì `tenPercentOff.then(PriceRule.none()).apply(new BigDecimal("100000"))` in ra gì (với `tenPercentOff = p -> p.multiply(new BigDecimal("0.9"))`)?
<details><summary>Đáp án</summary>Hợp lệ. Chỉ `apply` là abstract method được tính; `equals` là method public của `Object`, còn default và static method không tính. Kết quả in ra là `90000.0` chứ không phải `90000`: khi nhân `BigDecimal`, scale của kết quả bằng tổng scale hai vế (0 + 1). Muốn in thành tiền thì dùng `setScale(2, HALF_UP)`.</details>

**Q2.** `plus1 = x -> x + 1`, `times2 = x -> x * 2`. In ra gì: `plus1.andThen(times2).apply(3)` và `plus1.compose(times2).apply(3)`?
<details><summary>Đáp án</summary>`8 7`. `andThen` đọc từ trái sang phải: (3 + 1) × 2 = 8. `compose` thì hàm bên trong chạy trước, như f(g(x)): 3 × 2 + 1 = 7. `Predicate` không có `andThen`/`compose`, chỉ có `and`/`or`/`negate`.</details>

**Q3.** `int count = 0; list.forEach(x -> count++);` → kết quả?
<details><summary>Đáp án</summary>Lỗi compile: *local variables referenced from a lambda expression must be final or effectively final*. Lambda chụp lại **giá trị** của biến local tại thời điểm tạo, trong khi biến gốc nằm trên stack. Cách đúng là `long count = list.stream().filter(...).count();`, không cần biến đếm. Lách bằng `int[1]` hay `AtomicInteger` thì vẫn chạy được, nhưng đó là side effect.</details>

**Q4.** `Cart` override `toString()` trả về `"Cart"`. Trong `run()`, lambda in `this` và một anonymous `Runnable` cũng in `this`. Hai dòng in ra gì?
<details><summary>Đáp án</summary>`Cart / Cart$1@…`. Lambda không tạo scope mới, nên `this` là object `Cart`. Anonymous class là một class mới (compiler đặt tên `Cart$1`) và không override `toString`, nên in dạng `ClassName@hash`.</details>

**Q5.** `Stream.of("a","b","c").filter(x -> { println("filter " + x); return true; });` rồi `println("done")`. Console in gì?
<details><summary>Đáp án</summary>Chỉ in `done`. `filter` là intermediate operation, lazy; không có terminal nên lambda bên trong không chạy.</details>

**Q6.** `Stream.of("one","two","three","four").peek(print x + " ").filter(len > 3).findFirst().ifPresent(println)` in ra gì?
<details><summary>Đáp án</summary>`one two three three`. Stream xử lý theo chiều dọc: `three` qua được filter thì `findFirst` dừng ngay (short-circuit), nên `four` không bao giờ được đọc.</details>

**Q7.** Gọi `count()` rồi gọi tiếp `toList()` trên cùng một biến `Stream`?
<details><summary>Đáp án</summary>`IllegalStateException: stream has already been operated upon or closed`. Stream giống Iterator chứ không giống List: chỉ duyệt được một lần. Đây là lỗi runtime, compiler không bắt được.</details>

**Q8.** `Stream.of("A:1","B:2","A:3").collect(toMap(sku, qty))` → kết quả?
<details><summary>Đáp án</summary>`IllegalStateException: Duplicate key A (attempted merging values 1 and 3)`. Khác `Map.put` (ghi đè im lặng). Sửa bằng `toMap(sku, qty, Integer::sum)` hoặc `groupingBy(sku, summingInt(qty))`. Trong ecommerce, lỗi này hay gặp khi gộp giỏ hàng có cùng một sản phẩm thêm hai lần.</details>

**Q9.** `Optional.of("Áo thun nam")`: gọi `orElse(loadDefaultName())` rồi `orElseGet(Demo::loadDefaultName)`. `loadDefaultName` được gọi bao nhiêu lần ở mỗi bên?
<details><summary>Đáp án</summary>`orElse` gọi 1 lần, `orElseGet` gọi 0 lần. Đối số của `orElse` luôn được tính trước khi gọi, kể cả khi Optional có giá trị. `Supplier` trong `orElseGet` chỉ chạy khi Optional rỗng, nên khác biệt này đáng kể nếu hàm load phải truy vấn DB.</details>

**Q10.** `List<String> names = stream.toList(); names.add("Tất");` → ?
<details><summary>Đáp án</summary>`UnsupportedOperationException`. `Stream.toList()` (Java 16) trả về list unmodifiable. Cần sửa list thì copy ra: `new ArrayList<>(names)`.</details>

**Q11.** `Stream.of("b","a","c").peek(print x + " ").sorted().findFirst()` in ra gì trước `-> a`?
<details><summary>Đáp án</summary>`b a c -> a`. `sorted` là stateful op: phải đọc **hết** phần tử mới biết phần tử nào đứng đầu, nên `peek` in đủ cả 3 dù có `findFirst`. So sánh với Q6.</details>

**Q12.** `Function<String,Integer> f = String::length; BiFunction<String,String,Boolean> g = String::equalsIgnoreCase;` → `f.apply("Claude")`, `g.apply("SALE10","sale10")`?
<details><summary>Đáp án</summary>`6 true`. Cả hai đều là **unbound** method reference: tham số đầu tiên là object gọi method. Vì vậy `String::equalsIgnoreCase` khớp với `BiFunction` (hay `BiPredicate`), không khớp với `Function`.</details>

**Q13.** `BigDecimal total = ZERO; for (price : [150000, 450000]) total.add(price);` → in `total`?
<details><summary>Đáp án</summary>`0`. `BigDecimal` immutable: `add` trả về object mới. Phải viết `total = total.add(price)`, hoặc tốt hơn là `prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add)`.</details>

**Q14.** `Stream.of(100_000, 250_000).collect(partitioningBy(t -> t >= 1_000_000))` → ?
<details><summary>Đáp án</summary>`{false=[100000, 250000], true=[]}`. `partitioningBy` **luôn** có đủ 2 key, kể cả khi một bên rỗng. Khác với `groupingBy`: không có phần tử nào thì không có key đó.</details>

**Q15.** (Thiết kế) Ecommerce có nhiều quy tắc giá: thành viên giảm 5%, flash sale trừ 20.000đ, freeship… và quy tắc thay đổi thường xuyên. Nên làm subclass cho mỗi loại giảm giá hay mỗi quy tắc là một hàm? Ghép các quy tắc thế nào?
<details><summary>Đáp án</summary>Mỗi quy tắc là một `UnaryOperator<BigDecimal>`, hoặc functional interface riêng của domain (`PricingRule`), để code dễ đọc. Ghép bằng `andThen`, hoặc gộp cả danh sách bằng `rules.stream().reduce(Function.identity(), Function::andThen)`. Thêm quy tắc mới thì chỉ thêm một hàm, không phải sửa engine (OCP, S03). Nên giữ các rule pure (không đọc giờ hệ thống bên trong) để test không cần mock: truyền thời điểm vào làm tham số.</details>

## 4. Bài tập & homework
> Tham khảo, **không tính tiến độ**. Chỉ phần đưa vào ecommerce (mục 5) mới được tính.
- [ ] Tự làm và trình bày slide S05 (đã trình bày 17/10)
- [ ] Chạy `FunctionalStreamPitfalls.java`, đoán output trước rồi mới chạy
- [ ] Xem lại các gap Stream/Lambda còn `Mở` trong `docs/review/gaps.md` (pretest 01/10: lazy/short-circuit, toMap trùng key, effectively final, checked exception trong lambda…)
- [ ] `javap -c -p` một class có lambda để thấy `invokedynamic` và `lambda$…$0`

## 5. Áp dụng vào ecommerce
Xem `docs/ecommerce-backlog.md` → **S05**. Đây là bài tập cuối slide: refactor sang Stream, mỗi quy tắc giá là một hàm, thêm `ReportService`.

## 6. Ghi chú cá nhân / câu hỏi mang lên lớp
- **Slide ex-pricing:** `inStock.and(digital.negate())` dùng để lọc "sản phẩm cần giao", nhưng trong project thì `SubscriptionProduct` và `BundleProduct` cũng không phải `Digital`, nên sẽ bị lọc nhầm thành "cần giao". Đúng ra nên lọc theo khả năng giao hàng: `Shippable.class::isInstance`. Cũng nên cân nhắc việc dùng `instanceof` ở đây có mâu thuẫn với rule S03 "không dùng `instanceof`" hay không (rule đó chỉ áp cho phần tính giá).
- **Slide fp-why / ex-*:** code ví dụ dùng `getPrice()`, `getSubtotal()`, `getCategory()`, `OrderStatus.PAID`. Project hiện tại dùng `getBasePrice()`/`calculateFinalPrice()` và `lineTotal()`, chưa có category hay status. Task S05 sẽ bổ sung những gì cần.
- **Slide q5 (speaker notes):** "`Stream.of(...).map(...).count()` có thể không in gì". Đúng với Java 9+: `count()` bỏ qua cả pipeline khi biết trước kích thước (SIZED) và không có `filter`. Nhưng đây là tối ưu của implementation chứ không phải hành vi được spec đảm bảo, nên tránh đặt logic quan trọng trong `peek`/`map`.
- **Slide fi-more:** ghi "~40 interface" trong `java.util.function`. Con số thực tế là 43, nên giữ chữ "khoảng".
- **Format mới (từ S04):** tự nghiên cứu và tự trình bày. Hỏi mentor xem buổi T4 vẫn review code ecommerce như trước, hay review cả slide.

## 7. Feedback review T4
- 
