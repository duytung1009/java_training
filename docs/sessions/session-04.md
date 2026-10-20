# S04 – Generics, Collections & Exception

- **Module:** – (tự nghiên cứu, không có slide) · **Ngày T7:** 03/10 · **Slide:** – _(soạn theo chủ đề; cập nhật lại nếu sau này có tài liệu của lớp)_
- **Demo:** `java playground/session04/GenericsCollectionsPitfalls.java` (JDK 11+) · pretest 01/10: `playground/session04/CollectionsStreamDeepDive.java`

## 1. Tóm tắt 1 phút
1. **Generics** giúp compiler kiểm tra kiểu: `List<Product>` không nhận `String`, không cần ép kiểu. Generics **invariant**: `List<String>` không phải `List<Object>`. Muốn linh hoạt thì dùng wildcard theo quy tắc **PECS**: Producer `? extends`, Consumer `? super`.
2. **Type erasure**: thông tin kiểu generic bị xóa lúc compile. Vì vậy không có `new T[]`, `T.class`, `instanceof List<String>`, và `ArrayList<String>` với `ArrayList<Integer>` là cùng một class lúc runtime.
3. **Chọn collection theo cách truy cập**: `List` có thứ tự và cho trùng; `Set` không trùng (dựa vào `equals`/`hashCode`, hoặc `compareTo`/`Comparator` với `TreeSet`); `Map` tra theo key; `Deque` dùng làm stack/queue; `PriorityQueue` lấy phần tử nhỏ nhất trước.
4. **Bẫy hay gặp**: `List.of`/`Arrays.asList` không add được; sửa list trong lúc for-each thì ném `ConcurrentModificationException` (fail-fast, best-effort); key mutable trong `HashSet`/`HashMap` bị "mồ côi"; `TreeSet` coi 2 phần tử là trùng khi `compare` trả về 0.
5. **Exception**: checked (compiler bắt khai báo hoặc xử lý) vs unchecked (`RuntimeException`). `finally` luôn chạy; try-with-resources đóng resource **ngược thứ tự** khai báo và giữ exception phụ ở dạng *suppressed*. Domain exception nên là unchecked và có message rõ ràng.

## 2. Keyword checklist

### Generics
- [ ] **Type parameter** `class Box<T>`, `<T> T first(List<T>)` (generic method), diamond `new ArrayList<>()`.
- [ ] **Bounded type** `<T extends Comparable<T>>`, `<T extends Identifiable<ID>, ID>`; nhiều bound thì viết `<T extends A & B>`.
- [ ] **Invariance**: `List<String>` không gán được cho `List<Object>` (lỗi compile), khác với mảng (`String[]` là `Object[]`, sai kiểu thì nổ `ArrayStoreException` lúc runtime).
- [ ] **Wildcard & PECS**: `List<? extends Number>` thì đọc được, không add được (trừ `null`); `List<? super Integer>` thì add được `Integer`, đọc ra chỉ là `Object`. Ví dụ `Collections.copy(List<? super T> dest, List<? extends T> src)`.
- [ ] **Type erasure**: không có `new T[]`, `T.class`, `instanceof List<String>`; không overload được hai method chỉ khác nhau ở type argument; dữ liệu generic dùng sai kiểu sẽ nổ `ClassCastException` lúc lấy ra.
- [ ] **Raw type** `List list = new ArrayList();`: mất kiểm tra kiểu, compiler cảnh báo *unchecked*. Không dùng.

### List
- [ ] **ArrayList vs LinkedList**: `ArrayList` truy cập theo index O(1), là lựa chọn mặc định; `LinkedList` chỉ có lợi khi thêm/xóa ở đầu qua iterator. Trong thực tế hầu như luôn dùng `ArrayList`, còn `ArrayDeque` dùng cho queue.
- [ ] **ArrayList tự nới**: bên trong là một mảng, đầy thì copy sang mảng lớn hơn khoảng 1.5 lần.
- [ ] **`remove(int index)` vs `remove(Object o)`**: với `List<Integer>`, gọi `remove(1)` là xóa theo **index**. Muốn xóa giá trị thì viết `remove(Integer.valueOf(1))`.
- [ ] **List.of (Java 9) / Arrays.asList / List.copyOf**: `List.of` bất biến và không nhận `null`; `Arrays.asList` có kích thước cố định, cho `set` nhưng không cho `add`/`remove`, và là view trên mảng gốc.
- [ ] **Collections.unmodifiableList**: chỉ là *view*; list gốc đổi thì view đổi theo. Muốn một bản snapshot thì dùng `List.copyOf`.

### Set
- [ ] **HashSet / LinkedHashSet / TreeSet**: không thứ tự / giữ thứ tự thêm / có sắp xếp.
- [ ] **equals + hashCode contract** (S03): `HashSet` gọi `hashCode` để tìm bucket rồi gọi `equals`. Override `equals` mà không override `hashCode` thì set chứa trùng.
- [ ] **Key mutable**: sửa field tham gia `hashCode` sau khi đã thêm vào set thì phần tử bị "mồ côi": `contains`/`remove` đều trả `false`. Field làm key phải `final`.
- [ ] **TreeSet + Comparator**: hai phần tử có `compare == 0` bị coi là **trùng**, dù `equals` khác nhau. Comparator cần nhất quán với `equals`, ví dụ `comparing(price).thenComparing(id)`.
- [ ] **TreeSet với class không Comparable**: vẫn compile được, nhưng `add` đầu tiên đã ném `ClassCastException`.

### Map
- [ ] **HashMap / LinkedHashMap / TreeMap**: không thứ tự / giữ thứ tự thêm (hoặc thứ tự truy cập, dùng làm LRU) / sắp theo key.
- [ ] **HashMap bên trong**: mảng bucket, va chạm thì nối danh sách, bucket đủ lớn thì chuyển thành cây (Java 8). Load factor 0.75 thì resize.
- [ ] **getOrDefault, putIfAbsent, merge, compute, computeIfAbsent**: `merge(k, v, Integer::sum)` cộng dồn; remapping trả về `null` thì **xóa key**. `computeIfAbsent(k, x -> new ArrayList<>()).add(v)` là idiom multimap.
- [ ] **Map.of (Java 9)**: bất biến, tối đa 10 cặp (nhiều hơn thì dùng `Map.ofEntries`), key trùng thì ném `IllegalArgumentException`, không nhận `null`.
- [ ] **Duyệt Map**: `entrySet()` (lấy cả key và value), `keySet()`, `values()`. Sửa map trong lúc duyệt cũng fail-fast.

### Queue, Deque & Iterator
- [ ] **Queue / Deque**: `offer`/`poll`/`peek` trả về `null`/`false` khi rỗng; `add`/`remove`/`element` thì ném exception. `ArrayDeque` dùng làm stack (`push`/`pop`) thay cho class cũ `Stack`.
- [ ] **PriorityQueue**: `poll` luôn lấy phần tử nhỏ nhất. Nhưng `toString()` và iterator **không** theo thứ tự sắp xếp, vì bên trong là binary heap.
- [ ] **Iterator, fail-fast, modCount**: sửa collection trong lúc for-each thì `next()` lần sau ném `ConcurrentModificationException`. Cơ chế chỉ là best-effort: xóa phần tử **áp chót** không nổ, mà bỏ qua phần tử cuối. Cách đúng là `iterator.remove()` hoặc `removeIf`.
- [ ] **Comparable vs Comparator**: thứ tự tự nhiên (`compareTo`, có trong class) vs thứ tự bên ngoài (`Comparator.comparing(...).thenComparing(...).reversed()`).

### Exception
- [ ] **Hierarchy**: `Throwable` → `Error` (không bắt) và `Exception` → `RuntimeException` (unchecked) và các exception còn lại (checked).
- [ ] **Checked vs unchecked**: phân loại theo cây kế thừa, không theo tên. Ví dụ `FileNotFoundException` extends `IOException` nên là checked; `catch` một checked exception mà khối try không thể ném ra là lỗi compile.
- [ ] **try / catch / finally**: `finally` luôn chạy. Giá trị `return` trong try được tính và cất lại **trước** khi finally chạy, nên sửa biến primitive trong finally không đổi kết quả. Viết `return` trong finally thì nó ghi đè, và còn nuốt luôn exception (rất nên tránh).
- [ ] **Multi-catch** `catch (A | B e)`; thứ tự catch phải đi từ cụ thể tới tổng quát (bắt lớp cha trước thì lỗi compile *already been caught*).
- [ ] **try-with-resources** (`AutoCloseable`): đóng **ngược** thứ tự khai báo (LIFO). Nếu cả body lẫn `close()` cùng ném, exception của body được giữ lại, các exception từ `close()` vào `getSuppressed()`.
- [ ] **Custom / domain exception**: `class OutOfStockException extends IllegalStateException`, có message và dữ liệu (`productId`, `requested`, `available`). Wrap exception thì nhớ giữ `cause`.
- [ ] **Override + exception** (S03): method override không được ném checked exception rộng hơn method cha.

## 3. Quiz tự kiểm tra
Câu 1–14 có trong demo `GenericsCollectionsPitfalls.java` (dòng in ra cùng số); câu 11 có thêm phần lỗi compile để dạng comment. Câu 15 là câu thiết kế.

**Q1.** `List<Integer> quantities = new ArrayList<>(List.of(1, 2, 3)); quantities.remove(1);` → list còn gì?
<details><summary>Đáp án</summary>`[1, 3]`. Overload `remove(int index)` khớp chính xác với literal `1`, nên được chọn trước `remove(Object)` (vốn phải autoboxing). Muốn xóa giá trị 1 thì viết `remove(Integer.valueOf(1))`. Đây cũng là quy tắc chọn overload ở S03: exact match trước, boxing sau.</details>

**Q2.** `List<String> fixed = Arrays.asList("Áo", "Quần"); fixed.set(0, "Tất"); fixed.add("Mũ");` → ?
<details><summary>Đáp án</summary>`set` chạy được, list thành `[Tất, Quần]`; còn `add` ném `UnsupportedOperationException`. `Arrays.asList` trả về list kích thước cố định, là view trên mảng gốc. Khác với `List.of`: bản này `set` cũng không được.</details>

**Q3.** `List.of("Áo", null)` → ?
<details><summary>Đáp án</summary>`NullPointerException` ngay lúc tạo. `List.of`, `Set.of`, `Map.of` (Java 9) đều không nhận `null`.</details>

**Q4.** Duyệt `["Áo","Quần","Tất"]` bằng for-each và `remove("Áo")`, kết quả? Nếu đổi thành `remove("Quần")` (phần tử áp chót) thì sao?
<details><summary>Đáp án</summary>Xóa "Áo" thì `ConcurrentModificationException` ở lần `next()` tiếp theo. Xóa "Quần" thì **không** lỗi, list còn `[Áo, Tất]`, nhưng "Tất" **bị bỏ qua**, không được duyệt tới: `size` giảm còn 2 nên `hasNext()` trả `false`. Fail-fast chỉ là best-effort. Cách đúng: `cart.removeIf(x -> x.equals("Áo"))` hoặc `iterator.remove()`.</details>

**Q5.** Thêm `key` (sku P001) vào `HashSet`, sau đó sửa `key.sku = "P002"`. In ra `contains(new CartKey("P002"))`, `contains(key)`, `size()`?
<details><summary>Đáp án</summary>`false, false, 1`. Phần tử nằm ở bucket tính từ hash cũ ("P001"), còn mọi lần tìm đều dùng hash mới ("P002") nên không tìm ra. Phần tử bị "mồ côi" và cũng không `remove` được. Field tham gia `hashCode` phải `final`, như `Product.id` của project.</details>

**Q6.** `TreeSet` dùng `Comparator.comparing(price)`, thêm P001 (150.000), P002 (150.000), P003 (99.000) → set in ra gì?
<details><summary>Đáp án</summary>`[P003, P001]`. P002 **bị mất**, vì `compare(P001, P002) == 0` nên `TreeSet` coi là trùng, dù `equals` khác nhau (khác sku). Sửa bằng `comparing(price).thenComparing(sku)`.</details>

**Q7.** `merge("P001", 2, sum)`, `merge("P001", 3, sum)`, `merge("P002", 1, sum)`, rồi `merge("P002", 1, (a, b) -> null)` → map?
<details><summary>Đáp án</summary>`{P001=5}`. Remapping function trả về `null` thì **xóa** key P002. Đây đúng là hành vi cần cho giỏ hàng: số lượng về 0 thì bỏ sản phẩm khỏi giỏ.</details>

**Q8.** Gọi `computeIfAbsent("FASHION", k -> new ArrayList<>()).add(...)` 2 lần và `"SOFTWARE"` 1 lần trên một `LinkedHashMap` → ?
<details><summary>Đáp án</summary>`{FASHION=[Áo thun, Quần jean], SOFTWARE=[Office]}`. `computeIfAbsent` tạo list ở lần đầu, các lần sau trả lại đúng list đó (idiom multimap). `LinkedHashMap` giữ thứ tự thêm.</details>

**Q9.** `Map.of("P001", 50, "P001", 20)` → ?
<details><summary>Đáp án</summary>`IllegalArgumentException: duplicate key`. Khác với `put` (ghi đè) và khác với `Collectors.toMap` (ném `IllegalStateException`, sẽ học ở S05).</details>

**Q10.** `PriorityQueue` từ `[30, 10, 20, 5]` rồi `poll` hết; `ArrayDeque` lần lượt `push` "Trang chủ", "Danh mục", "Sản phẩm" rồi `pop` → ?
<details><summary>Đáp án</summary>`poll = 5 10 20 30 | pop = Sản phẩm`. `poll` luôn lấy phần tử nhỏ nhất, nhưng `toString()` của `PriorityQueue` không theo thứ tự đó vì bên trong là heap. `ArrayDeque.push`/`pop` là LIFO, giống lịch sử điều hướng (breadcrumb).</details>

**Q11.** Dòng nào compile lỗi? (a) `List<Object> objects = new ArrayList<String>();` (b) `List<? extends Number> nums = new ArrayList<Integer>(); nums.add(1);` (c) `sumAll(List.of(1, 2, 3))` với `double sumAll(List<? extends Number>)`.
<details><summary>Đáp án</summary>(a) lỗi, vì generics invariant. (b) lỗi ở `add`: compiler không biết `?` cụ thể là kiểu gì, nên không cho add (trừ `null`). (c) chạy được và in `6.0`: `? extends` là *producer*, chỉ đọc ra (chữ P trong PECS).</details>

**Q12.** `new ArrayList<String>().getClass() == new ArrayList<Integer>().getClass()`?
<details><summary>Đáp án</summary>`true`. Do type erasure, lúc runtime cả hai đều chỉ là `ArrayList`. Đây cũng là lý do không viết được `new T[]` hay `instanceof List<String>`.</details>

**Q13.** `int x = 1; try { return x; } finally { x = 99; }` → method trả về?
<details><summary>Đáp án</summary>`1`. Giá trị return được tính và cất lại trước khi `finally` chạy; gán lại `x` trong finally không ảnh hưởng. (Còn nếu finally có `return` riêng thì nó ghi đè, và nuốt luôn exception nếu có.)</details>

**Q14.** `try (Resource A; Resource B) { throw new IllegalStateException("body"); } catch (...)` → in ra gì?
<details><summary>Đáp án</summary>`close B close A | catch body, suppressed=0`. Resource được đóng **ngược** thứ tự khai báo, và đóng **trước** khi vào `catch`. Ở đây `close()` không ném nên `suppressed = 0`; nếu `close()` cũng ném thì exception đó nằm trong `e.getSuppressed()`, còn exception của body vẫn là exception chính.</details>

**Q15.** (Thiết kế) Giỏ hàng cần: thêm cùng một sản phẩm 2 lần thì cộng dồn số lượng, bỏ sản phẩm khi số lượng về 0, hiển thị theo thứ tự thêm vào. Chọn cấu trúc dữ liệu nào? Khi thiếu hàng thì nên báo lỗi bằng gì?
<details><summary>Đáp án</summary>Dùng `LinkedHashMap<Product, Integer>`: key là `Product` (equals/hashCode theo `id` final, từ S03), `merge(product, qty, Integer::sum)` để cộng dồn, remapping trả về `null` để xóa; `LinkedHashMap` giữ thứ tự thêm. `getItems()` nên trả về một bản copy không sửa được (`Map.copyOf` hoặc `unmodifiableMap`). Thiếu hàng thì ném domain exception unchecked, ví dụ `OutOfStockException extends IllegalStateException`, kèm `productId`, số lượng yêu cầu và số lượng còn. Kế thừa từ `IllegalStateException` thì code và test cũ đang bắt `IllegalStateException` vẫn chạy.</details>

## 4. Bài tập & homework
> Tham khảo, **không tính tiến độ**. Chỉ phần đưa vào ecommerce (mục 5) mới được tính.
- [ ] Chạy `GenericsCollectionsPitfalls.java` và `CollectionsStreamDeepDive.java`, đoán output trước rồi mới chạy
- [ ] Xem lại các gap S04 còn `Mở` trong `docs/review/gaps.md`: HashSet mutable key, TreeSet không Comparable, fail-fast/modCount, Iterator, finally + return
- [ ] Đọc *Effective Java*: Item 26 (raw type), 28 (List thay array), 31 (bounded wildcard, PECS), 69–72 (exception)
- [ ] Đọc source `ArrayList.grow()` và `HashMap.putVal()` trong JDK

## 5. Áp dụng vào ecommerce
Xem `docs/ecommerce-backlog.md` → **S04**.

## 6. Ghi chú cá nhân / câu hỏi mang lên lớp
- **Không có slide:** session này soạn theo chủ đề Tùng tự nghiên cứu. Nếu lớp có tài liệu riêng cho buổi 03/10, hỏi mentor để bổ sung phần còn thiếu, nhất là xem Queue/Deque có nằm trong phạm vi chấm không.
- **Thứ tự buổi:** S05 (Stream, 17/10) sẽ refactor `InventoryManager` sang Stream, nên S04 chọn cấu trúc dữ liệu trước: catalog chuyển từ `Product[]` sang `Map`.
- **Có nên để `OutOfStockException` kế thừa `IllegalStateException`** (để tương thích code cũ), hay tạo một `DomainException` gốc riêng? Hỏi mentor quy ước của team.

## 7. Feedback review T4
- 
