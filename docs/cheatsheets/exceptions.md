# Cheatsheet – Exceptions

> Mỗi thẻ ứng với một gap trong `docs/review/gaps.md`. Ví dụ code đều đã chạy thử (JDK 21).

## Mục lục
- [Checked vs unchecked theo hierarchy](#checked-vs-unchecked-theo-hierarchy--l2--mình-hiểu-sai--gap-2026-10-07)
- [Override & throws](#override--throws--l2--biết-bề-mặt--gap-2026-10-07)
- [finally + return](#finally--return--l3--mình-hiểu-sai--gap-2026-10-01)
- [try-with-resources: thứ tự close](#try-with-resources-thứ-tự-close--l3--mình-hiểu-sai--gap-2026-10-07)

---

### Checked vs unchecked theo hierarchy  ·  L2 · mình hiểu sai · gap 2026-10-07

**Cốt lõi:** Không phải đoán theo tên mà là theo **kế thừa**: mọi `Throwable` không nằm dưới `RuntimeException` hoặc `Error` đều là checked.

**Quy tắc**
- `Exception` là checked; `RuntimeException` (con của `Exception`) và mọi con của nó là unchecked; `Error` là unchecked.
- `FileNotFoundException → IOException → Exception`: checked. `UncheckedIOException → RuntimeException`: unchecked (Java 8).
- Compiler bắt lỗi nếu `catch` một checked exception mà khối `try` không thể ném ra nó, **trừ `Exception` và `Throwable`** (vì chúng bao cả unchecked).
- Unchecked exception thì luôn `catch` được, kể cả khi không ai ném nó.

**Ví dụ** (đã chạy, JDK 21)
```java
// checked hay unchecked? -> hỏi thẳng hierarchy
RuntimeException.class.isAssignableFrom(FileNotFoundException.class)  // -> false  (checked)
IOException.class.isAssignableFrom(FileNotFoundException.class)       // -> true
RuntimeException.class.isAssignableFrom(IllegalStateException.class)  // -> true   (unchecked)

try { cart.total(); } catch (FileNotFoundException e) {}  // compile error: never thrown in body of try
try { cart.total(); } catch (Exception e) {}              // OK: được miễn trừ
```

**Khi làm thật:** Gặp exception tên lạ (`ClassNotFoundException`, `InterruptedException`, `ParseException`...) thì mở Javadoc xem dòng hierarchy, đừng đoán theo tên.

**Đọc thêm**
- Javadoc: [`Throwable`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Throwable.html): đọc đoạn thứ 2 của class description (định nghĩa checked exception).
- Javadoc: [`FileNotFoundException`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/FileNotFoundException.html): xem cây kế thừa ở đầu trang.
- JLS §11.1.1: [The Kinds of Exceptions](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.1.1)
- JLS §11.2.3: [Exception Checking](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.2.3): đọc câu "It is a compile-time error if a catch clause can catch checked exception class E1…".

**Tự kiểm**
1. `ClassNotFoundException` và `NumberFormatException`: cái nào checked?
   <details><summary>Đáp án</summary><code>ClassNotFoundException</code> là checked (→ ReflectiveOperationException → Exception). <code>NumberFormatException</code> là unchecked (→ IllegalArgumentException → RuntimeException).</details>
2. `try { System.out.println(); } catch (Throwable t) {}` có compile không?
   <details><summary>Đáp án</summary>Có. Throwable là superclass của Exception nên được miễn trừ (JLS §11.2.3).</details>

---

### Override & throws  ·  L2 · biết bề mặt · gap 2026-10-07

**Cốt lõi:** Method không khai báo `throws` nghĩa là **không ném checked exception nào**; method override chỉ được ném checked exception **bằng hoặc hẹp hơn** method cha.

**Quy tắc**
- Override được: bỏ bớt checked exception, khai báo exception con (hẹp hơn), hoặc không khai báo gì.
- Override không được: thêm checked exception mới, hoặc khai báo exception rộng hơn (`throws Exception` khi cha `throws IOException`).
- Unchecked exception thì compiler không theo dõi: `throws IllegalStateException` khai báo trên method nào cũng được, chỉ có tác dụng như tài liệu.
- Lý do (LSP): người gọi qua kiểu cha chỉ chuẩn bị xử lý những gì kiểu cha hứa.
- Muốn ném lỗi IO trong implementation của interface "sạch" thì bọc bằng `UncheckedIOException(msg, cause)` và **luôn giữ cause**.

**Ví dụ** (đã chạy, JDK 21)
```java
interface ProductRepo { void save(String sku); }

class FileProductRepo implements ProductRepo {
    public void save(String sku) {                       // không thể thêm "throws IOException"
        try { writeCsv(sku); }                           // writeCsv ném FileNotFoundException
        catch (IOException e) { throw new UncheckedIOException("save failed: " + sku, e); }
    }
}
// e.getCause() -> java.io.FileNotFoundException: catalog.csv   (stack trace gốc còn nguyên)
```

**Khi làm thật:** Đừng `throw new RuntimeException(e.getMessage())`, cách này làm mất cause và mất stack trace gốc. Truyền `e` vào constructor.

**Đọc thêm**
- JLS §8.4.8.3: [Requirements in Overriding and Hiding](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.3): tìm đoạn về `throws` clause.
- Javadoc: [`UncheckedIOException`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/UncheckedIOException.html): 2 constructor, đều bắt buộc có cause.
- Effective Java (3rd ed.) Item 73: Throw exceptions appropriate to the abstraction (exception translation & chaining)

**Tự kiểm**
1. `interface Loader { void load() throws IOException; }`. Ba implementation lần lượt khai báo `throws FileNotFoundException`, không khai báo gì, và `throws Exception`. Cái nào compile được?
   <details><summary>Đáp án</summary>Hai cái đầu compile được (hẹp hơn hoặc không ném). <code>throws Exception</code> lỗi: "cannot implement load() in Loader".</details>
2. Người gọi viết `Loader l = new A(); l.load();`, trong đó A khai báo `throws FileNotFoundException`. Người gọi phải bắt exception nào?
   <details><summary>Đáp án</summary><code>IOException</code>. Compiler xét kiểu khai báo (Loader), không xét object thật.</details>

---

### finally + return  ·  L3 · mình hiểu sai · gap 2026-10-01

**Cốt lõi:** Không phải "finally không ảnh hưởng gì tới giá trị trả về" mà là: giá trị `return` được **tính và cất vào biến tạm trước** khi finally chạy; finally chỉ thay đổi được kết quả nếu nó **tự `return`/`throw`** (và khi đó exception đang bay sẽ bị nuốt).

**Quy tắc**
- finally luôn chạy, dù try kết thúc bình thường, bằng `return` hay bằng exception (trừ khi JVM dừng hẳn, ví dụ `System.exit`).
- `return x;` trong try: giá trị của `x` được copy sang local slot tạm → chạy finally → trả về giá trị đã cất. Sửa `x` trong finally không có tác dụng với primitive.
- Với reference: cái được cất là **tham chiếu**, nên sửa *nội dung* object trong finally vẫn thấy được.
- `return` (hoặc `throw`) trong finally **ghi đè** mọi kết quả trước đó, kể cả exception đang bay. Exception biến mất không để lại dấu vết.
- Cơ chế: javac **chép (inline) thân finally** vào mọi lối ra (lối thường + handler `any` cho exception). JVMS §3.13 mô tả cách cũ dùng `jsr/ret`, javac hiện đại không dùng nữa.

**Ví dụ** (đã chạy, JDK 21)
```java
static int reserve() { int qty = 1; try { return qty; } finally { qty = 99; } }
// -> 1

static List<String> lines() { List<String> l = new ArrayList<>(); try { return l; } finally { l.add("SKU-1"); } }
// -> [SKU-1]   (cất tham chiếu, object vẫn bị sửa)

static int checkout() { try { throw new IllegalStateException("out of stock"); } finally { return 0; } }
// -> 0         (exception "out of stock" bị nuốt mất)
```
Bytecode của `reserve()` (`javap -c`): `iload_0; istore_1` (cất giá trị trả về) → `bipush 99; istore_0` (finally) → `iload_1; ireturn`.

**Khi làm thật:** Không bao giờ `return` trong finally (javac còn có warning `-Xlint:finally`). Finally chỉ dùng để dọn dẹp, và từ Java 7 nên thay bằng try-with-resources.

**Đọc thêm**
- JLS §14.20.2: [Execution of try-finally and try-catch-finally](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.2): đọc các nhánh "If the finally block completes abruptly for reason S…".
- JVMS §3.13: [Compiling finally](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-3.html#jvms-3.13): chỉ đọc để hiểu ý "cất giá trị vào local trước finally". Phần `jsr/ret` là cách biên dịch cũ.

**Tự kiểm**
1. `int x = 1; try { throw new RuntimeException(); } catch (RuntimeException e) { return x; } finally { x = 5; System.out.println("F"); }`: in ra gì, và trả về bao nhiêu?
   <details><summary>Đáp án</summary>In <code>F</code>, trả về <code>1</code>. Giá trị return ở catch được cất trước khi finally chạy.</details>
2. Trong `checkout()` ở trên, nếu đổi `return 0` thành `System.out.println("cleanup")` thì sao?
   <details><summary>Đáp án</summary>In <code>cleanup</code>, rồi <code>IllegalStateException: out of stock</code> tiếp tục bay ra ngoài. finally hoàn tất bình thường nên không ghi đè gì cả.</details>

---

### try-with-resources: thứ tự close  ·  L3 · mình hiểu sai · gap 2026-10-07

**Cốt lõi:** Không phải đóng theo thứ tự khai báo mà là đóng **ngược thứ tự khởi tạo** (LIFO); exception của body thắng, exception của `close()` được gắn vào `getSuppressed()`.

**Quy tắc**
- Resource khai báo sau thường phụ thuộc vào resource khai báo trước (`rs` cần `ps`, `ps` cần `conn`), nên phải đóng thằng phụ thuộc trước.
- Resource nào **khởi tạo thất bại** thì không được close; các resource đã mở trước nó vẫn được close.
- Body ném E, close ném C → E bay ra ngoài, C nằm trong `E.getSuppressed()`. Body không ném → exception của `close()` đầu tiên bay ra, các close sau vào suppressed của nó.
- Cơ chế: compiler desugar thành các try/finally lồng nhau (resource đầu tiên ở vòng ngoài cùng), rồi tự gọi `addSuppressed`. So với try/finally viết tay kiểu Java 6, exception của close **ghi đè** và làm mất exception gốc.
- `catch`/`finally` gắn vào try-with-resources chạy **sau** khi mọi resource đã đóng.

**Ví dụ** (đã chạy, JDK 21)
```java
try (Res conn = new Res("conn"); Res ps = new Res("ps")) {   // close() của Res đều ném IllegalStateException
    throw new RuntimeException("body");
} catch (Exception e) {
    print("caught " + e.getMessage());
    for (Throwable s : e.getSuppressed()) print("  suppressed " + s.getMessage());
}
// -> open conn / open ps / close ps / close conn / caught body / suppressed close-ps / suppressed close-conn
```

**Khi làm thật:** Khi đọc log production, nhớ xem cả phần `Suppressed:` trong stack trace. Dùng try-with-resources cho `Connection`/`PreparedStatement`/`ResultSet`/stream và bỏ hẳn kiểu `finally { x.close(); }`.

**Đọc thêm**
- JLS §14.20.3.1: [Basic try-with-resources](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.3.1): đọc đoạn dịch (translation) sang try/finally lồng nhau.
- JLS §14.20.3: [try-with-resources](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.3): tìm câu "closed in the reverse order".
- Javadoc: [`Throwable#addSuppressed`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Throwable.html#addSuppressed(java.lang.Throwable))

**Tự kiểm**
1. Body chạy bình thường, chỉ `ps.close()` ném `close-ps`, còn `conn.close()` không ném. Output là gì?
   <details><summary>Đáp án</summary><code>open conn / open ps / body ok / close ps / close conn / caught close-ps</code>, suppressed = 0. conn vẫn được đóng dù ps.close() ném.</details>
2. Constructor của `ps` ném `init-ps`. Resource nào được close?
   <details><summary>Đáp án</summary>Chỉ <code>conn</code> (in <code>open conn / close conn / caught init-ps</code>). ps chưa từng mở thành công nên không có gì để close.</details>
