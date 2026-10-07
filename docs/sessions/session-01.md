# S01 – JVM, Data & String

- **Module:** M1 – Java Fundamentals · **Ngày T7:** 12/09 · **Slide:** `session-1-v2.pptx`
- **Demo:** `java playground/session01/DataTypePitfalls.java`

## 1. Tóm tắt 1 phút
1. `.java` → **javac** → bytecode `.class` → **ClassLoader** nạp vào → **Verifier** kiểm tra → **Execution Engine** chạy (Interpreter + JIT).
2. Bộ nhớ: **Stack** (mỗi thread một stack; chứa biến local, primitive, reference), **Heap** (chứa mọi object, dùng chung giữa các thread, do GC quản lý), **Metaspace** (metadata của class, nằm ngoài heap, có từ Java 8).
3. **GC** thu hồi các object không còn *reachable* từ **GC roots**, theo các bước Mark → Sweep → (Compact).
4. Có 8 primitive, mỗi primitive có wrapper tương ứng. **Autoboxing/unboxing** do compiler tự chèn. Bẫy hay gặp: Integer cache và NPE khi unbox `null`.
5. **String immutable** và có **String Pool**. So sánh nội dung chuỗi luôn dùng `equals()`. Nối chuỗi trong vòng lặp thì dùng **StringBuilder**.

## 2. Keyword checklist
Tick khi tự giải thích được mà **không cần nhìn**.

### JVM & thực thi
- [ ] **JDK / JRE / JVM**: JDK = JRE + công cụ (javac, javap, jmap…). JRE = JVM + thư viện chuẩn. JVM = chạy bytecode, quản lý bộ nhớ và GC. *(Từ Java 11 không còn bản JRE phát hành riêng; dùng `jlink` để tạo runtime.)*
- [ ] **Bytecode**: tập lệnh độc lập nền tảng, là lý do Java "write once, run anywhere".
- [ ] **javap -c**: disassemble file `.class` để xem bytecode (ví dụ thấy `Integer.valueOf` được chèn vào khi autoboxing).
- [ ] **ClassLoader**: Bootstrap (nạp `java.lang.*`…) → Platform (Java 9+; ở Java 8 gọi là Extension) → Application (nạp classpath của mình).
- [ ] **Parent delegation model**: loader con hỏi loader cha trước, nhờ vậy không ai "giả" được `java.lang.String`.
- [ ] **Loading → Linking (verify, prepare, resolve) → Initialization**: static block chạy ở bước initialization.
- [ ] **Interpreter vs JIT**: interpreter chạy từng lệnh. JIT biên dịch *hot code* (đoạn code chạy nhiều) sang mã máy native.
- [ ] **Runtime Data Areas**: Heap, Method Area/Metaspace, Stack (per-thread), PC Register, Native Method Stack.

### Memory & GC
- [ ] **Stack**: LIFO, mỗi lần gọi method tạo một *frame*, frame tự giải phóng khi method return. Đệ quy sâu quá dẫn tới `StackOverflowError`.
- [ ] **Heap**: chứa mọi object. Quá nhiều object còn sống thì gặp `OutOfMemoryError: Java heap space`.
- [ ] **PermGen → Metaspace** (Java 8): Metaspace nằm ngoài heap và tự nới rộng. Nếu hết thì gặp `OutOfMemoryError: Metaspace`.
- [ ] **GC roots**: biến local trên stack, static field, thread đang chạy…
- [ ] **Mark / Sweep / Compact**
- [ ] **Generational heap**: Young (Eden, S0, S1) và Old. *Minor GC* dọn Young, *Major/Full GC* dọn Old hoặc cả heap. Ý tưởng: hầu hết object "chết trẻ".
- [ ] **Stop-the-world**: thời gian ứng dụng bị dừng lại để GC chạy.
- [ ] **Các loại GC**: Serial (1 thread, heap nhỏ) · Parallel (tối ưu throughput, mặc định ở Java 8) · **G1** (cân bằng, mặc định từ Java 9) · ZGC / Shenandoah (pause cực thấp).
- [ ] **ClassNotFoundException vs NoClassDefFoundError**: cái đầu là *checked exception*, xảy ra khi nạp class động (`Class.forName`) mà không tìm thấy. Cái sau là *Error*: class có lúc compile nhưng mất lúc runtime.

### Data types
- [ ] **8 primitive**: byte(1B) short(2B) int(4B) long(8B) float(4B) double(8B) char(2B, UTF-16) boolean.
- [ ] **Giá trị mặc định** chỉ áp dụng cho **field** và phần tử mảng. **Biến local** không có giá trị mặc định, phải gán trước khi dùng, nếu không sẽ compile error.
- [ ] **Wrapper class**: bắt buộc khi dùng với Collections/Generics.
- [ ] **Autoboxing** (`Integer.valueOf(x)`) / **Unboxing** (`obj.intValue()`).
- [ ] **Integer cache -128..127**: vì cache nên `==` giữa hai Integer lúc đúng lúc sai. Luôn dùng `equals()`.
- [ ] **NPE khi unbox null**: `Integer x = null; int y = x;`
- [ ] **Widening** (ngầm định, an toàn): byte→short→int→long→float→double. *(Lưu ý: long→float vẫn có thể mất độ chính xác.)*
- [ ] **Narrowing** (phải ép tường minh, có thể mất dữ liệu): `(int) 9.99 = 9` (cắt phần thập phân), `(byte) 130 = -126` (tràn số, quay vòng).
- [ ] **Numeric promotion**: `byte + byte` cho ra kết quả `int`. Vì vậy `b = b + 1` bị lỗi compile, còn `b += 1` thì OK (compound assignment tự ép kiểu).
- [ ] **Integer overflow**: `Integer.MAX_VALUE + 1` thành số âm, không báo lỗi. Muốn phát hiện thì dùng `Math.addExact`.
- [ ] **double cho tiền là sai**: `0.1 + 0.2 = 0.30000000000000004`. Dùng `BigDecimal` (hoặc lưu `long` theo đơn vị nhỏ nhất).
- [ ] **String ↔ số**: `Integer.parseInt("42")`, `String.valueOf(42)`.

### String
- [ ] **Immutable**: mọi thao tác trên String đều tạo object mới. Nhờ vậy String thread-safe, cache được hashCode, và dùng làm key cho `HashMap` an toàn.
- [ ] **String Pool**: literal giống nhau dùng chung một object. `new String("x")` luôn tạo object mới. Từ Java 7, pool nằm trong heap.
- [ ] **Compile-time constant**: `"hel" + "lo"` được compiler gộp thành literal, nên nằm trong pool. `var + "lo"` thì nối lúc runtime, tạo object mới.
- [ ] **intern()**: đưa chuỗi vào pool (hoặc lấy lại bản đã có trong pool).
- [ ] **`==` vs `equals()`**: `==` so sánh reference, `equals()` so sánh nội dung.
- [ ] **String API**: `length() charAt() substring() indexOf() replace() split() trim()/strip() isBlank()`.
- [ ] **StringBuilder** (mutable, không thread-safe, nhanh) vs **StringBuffer** (mutable, `synchronized`). Dùng `new StringBuilder(capacity)` nếu biết trước kích thước.
- [ ] **`+=` trong vòng lặp**: O(n²) do phải cấp phát lại liên tục.

### Mốc phiên bản Java (bonus)
- [ ] 8: lambda/stream/Optional · 10: `var` · 11 (LTS): HttpClient, `strip()` · 17 (LTS): record, sealed · 21 (LTS): virtual thread · 25 (LTS): structured concurrency (vẫn còn preview).

## 3. Quiz tự kiểm tra
Trả lời ra giấy trước, rồi mới mở đáp án. Ghi điểm vào `progress.md`.

**Q1.** JDK, JRE, JVM: khi deploy app lên server thì cần cái nào? Khi dev thì cần cái nào?
<details><summary>Đáp án</summary>Server chỉ cần runtime (JRE, hoặc runtime tạo bằng jlink / image Docker chỉ có JRE). Dev cần JDK vì phải có javac.</details>

**Q2.** Tại sao lại có parent delegation? Nếu mình tự viết class `java.lang.String` thì chuyện gì xảy ra?
<details><summary>Đáp án</summary>Delegation đảm bảo class lõi luôn do Bootstrap nạp. Class `java.lang.String` tự viết sẽ không bao giờ được dùng, và việc định nghĩa class trong package `java.*` còn bị chặn (SecurityException / lỗi module).</details>

**Q3.** `String s = new String("hi");` tạo mấy object, và chúng nằm ở đâu?
<details><summary>Đáp án</summary>Tối đa 2 object: literal "hi" trong pool (nếu pool chưa có) và object mới do `new` tạo trên heap. Reference `s` nằm trên stack (nếu `s` là biến local).</details>

**Q4.** Output?
```java
Integer a = 127, b = 127, c = 128, d = 128;
System.out.println(a == b); System.out.println(c == d);
```
<details><summary>Đáp án</summary>true, false. Giá trị 127 nằm trong Integer cache nên a, b cùng một object. Còn 128 thì mỗi biến là một object riêng.</details>

**Q5.** `(byte) 130` = ? `(int) -9.99` = ?
<details><summary>Đáp án</summary>-126 (130 − 256) và -9 (cắt về phía 0, không làm tròn).</details>

**Q6.** Dòng nào compile lỗi? `byte b = 10; b = b + 1; b += 1;`
<details><summary>Đáp án</summary>`b = b + 1` bị lỗi vì `b + 1` có kiểu int. `b += 1` OK vì có ép kiểu ngầm.</details>

**Q7.** Output? `String p = "hel"; System.out.println("hello" == "hel" + "lo"); System.out.println("hello" == p + "lo");`
<details><summary>Đáp án</summary>true (compile-time constant), false (nối lúc runtime tạo object mới).</details>

**Q8.** Khi nào dùng StringBuffer thay cho StringBuilder?
<details><summary>Đáp án</summary>Khi *cùng một* buffer được nhiều thread ghi đồng thời. Trường hợp này thực tế hiếm, nên mặc định dùng StringBuilder.</details>

**Q9.** Phân biệt `StackOverflowError` và `OutOfMemoryError`. Cho một ví dụ gây ra mỗi loại.
<details><summary>Đáp án</summary>StackOverflowError: stack của thread bị đầy, ví dụ đệ quy không có điểm dừng. OOM heap: có quá nhiều object còn được tham chiếu, ví dụ liên tục add vào một static List mà không bao giờ xóa.</details>

**Q10.** Vì sao không dùng `double` cho giá sản phẩm? Nên dùng gì?
<details><summary>Đáp án</summary>double là số nhị phân dấu chấm động, không biểu diễn chính xác được 0.1, nên sai số sẽ cộng dồn. Dùng `BigDecimal` (tạo từ String hoặc `BigDecimal.valueOf`) hoặc `long` tính theo đơn vị nhỏ nhất (ví dụ VND).</details>

**Q11.** Mark–Sweep–Compact: mỗi bước làm gì? Tại sao cần bước compact?
<details><summary>Đáp án</summary>Mark: đánh dấu object reachable từ GC roots. Sweep: thu hồi bộ nhớ của object không được đánh dấu. Compact: dồn các object sống lại gần nhau để tránh phân mảnh, nhờ vậy cấp phát object lớn và cấp phát kiểu bump-pointer nhanh hơn.</details>

**Q12.** G1 và ZGC khác nhau thế nào (đây là homework)?
<details><summary>Gợi ý</summary>G1 chia heap thành các region, mục tiêu là pause time đặt trước (mặc định 200ms) và cân bằng với throughput. ZGC làm gần như mọi việc đồng thời với app, pause dưới 1ms đến vài ms kể cả với heap rất lớn, đổi lại tốn thêm CPU/bộ nhớ. Dùng ZGC khi ưu tiên độ trễ thấp.</details>

## 4. Bài tập & homework
- [ ] Practice Task 1: `WordCounter` (đếm word, sentence, char) → `playground/session01/`
- [ ] Practice Task 2: `TextTransformer` (`toTitleCase`, `reverseWords`, `removeVowels`) + CLI menu (bonus)
- [ ] Đọc *Effective Java* Item 6 – Avoid creating unnecessary objects
- [ ] (Optional) Cài VisualVM, quan sát heap khi chạy chương trình nhỏ
- [ ] Research G1 vs ZGC (ghi 3–5 dòng vào mục Ghi chú)

## 5. Áp dụng vào ecommerce
Xem `docs/ecommerce-backlog.md` → **S01**.

## 6. Ghi chú cá nhân / câu hỏi mang lên lớp
- 

## 7. Feedback review T4
- 
