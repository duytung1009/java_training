# Keyword Tracker

Mức tự tin: **0** chưa hiểu · **1** hiểu khi đọc lại · **2** tự giải thích được · **3** dạy lại được / áp dụng đúng trong code

Cách dùng: sau mỗi lần ôn, cập nhật cột *Tự tin* và *Ôn lần cuối*. Keyword nào ≤ 1 thì ưu tiên ôn trước. Giải thích chi tiết và quiz nằm trong `docs/sessions/session-XX.md`.

## S01

| Keyword | Tự tin | Ôn lần cuối | Ghi chú |
|---|---|---|---|
| JDK / JRE / JVM | | | |
| Compilation pipeline (javac → bytecode → JVM) | | | |
| javap -c | | | |
| ClassLoader (Bootstrap / Platform / Application) | | | |
| Parent delegation model | | | |
| Interpreter vs JIT | | | |
| Runtime Data Areas | | | |
| Stack vs Heap | | | |
| Metaspace (thay PermGen) | | | |
| GC roots · Mark/Sweep/Compact | | | |
| Young/Old generation · Minor/Full GC | | | |
| Serial / Parallel / G1 / ZGC | | | |
| ClassNotFoundException vs NoClassDefFoundError | | | |
| StackOverflowError vs OutOfMemoryError | | | |
| 8 primitive types + default value | | | |
| Autoboxing / Unboxing | | | |
| Integer cache (-128..127) | | | |
| Widening vs Narrowing casting | | | |
| Numeric promotion & compound assignment | | | |
| Integer overflow | | | |
| double vs BigDecimal cho tiền | | | |
| String immutability | | | |
| String Pool · intern() | | | |
| == vs equals() | | | |
| StringBuilder vs StringBuffer | | | |

## S02

| Keyword | Tự tin | Ôn lần cuối | Ghi chú |
|---|---|---|---|
| if/else · ternary | | | |
| switch statement & fall-through | | | |
| switch expression · yield | | | |
| for / while / do-while / for-each | | | |
| break / continue / labeled break | | | |
| Array (1-D, 2-D, jagged) | | | |
| Arrays utility methods | | | |
| Pass-by-value (reference copy) | | | |
| Class vs Object · field vs local | | | |
| Constructor · default constructor | | | |
| this keyword | | | |
| Constructor overloading | | | |
| Constructor chaining this() / super() | | | |
| Access modifiers (private/default/protected/public) | | | |
| static field / method / block | | | |
| Encapsulation (getter/setter + validation) | | | |

## S03

| Keyword | Tự tin | Ôn lần cuối | Ghi chú |
|---|---|---|---|
| 4 OOP pillars | | | |
| Encapsulation vs Abstraction | 2 | 28/09 | Encapsulation ≠ setter cho mọi field |
| Inheritance · is-a vs has-a (composition) | | | |
| Upcasting / Downcasting · ClassCastException | | | |
| instanceof pattern matching | | | |
| Compile-time vs runtime polymorphism | 2 | 28/09 |  |
| Overriding rules (@Override, access, exception, covariant) | 3 | 28/09 |  |
| Field hiding · static method hiding | 1 | 28/09 | Quiz: quên field trong method cha đọc theo class cha |
| Overloading rules · varargs · resolution order | 1 | 28/09 | Quiz: ôn lại 3 phase (exact/widening → boxing → varargs) |
| Abstract class | | | |
| Interface · default / static / private method | | | |
| Abstract class vs Interface | 2 | 28/09 |  |
| Diamond problem với default method | 2 | 28/09 | Nhớ quy tắc: class wins → sub-interface wins → phải override |
| Functional interface | | | |
| equals / hashCode contract | 2 | 28/09 |  |
| toString | | | |
| SRP | 2 | 28/09 |  |
| OCP | 2 | 28/09 |  |
| LSP | | | |
| ISP | 2 | 28/09 |  |
| DIP | 2 | 28/09 |  |
| Anti-pattern: God class, deep inheritance | | | |

## Ngoài lớp

| Keyword | Tự tin | Ôn lần cuối | Ghi chú |
|---|---|---|---|
| HashSet mutable key (S04) | | | ngoài lớp · gaps.md |
| TreeSet không Comparable (S04) | | | ngoài lớp · gaps.md |
| finally + return (S04) | | | ngoài lớp · gaps.md |
| Stream lazy / vertical / short-circuit / single-use (S04) | | | ngoài lớp · gaps.md |
| Fail-fast / modCount (S04) | | | ngoài lớp · gaps.md |
| Iterator & lỗ hổng fail-fast (S04) | | | ngoài lớp · gaps.md |
| Checked exception trong lambda (S04) | | | ngoài lớp · gaps.md |
| Collectors.toMap trùng key / null (S04) | | | ngoài lớp · gaps.md |
| Effectively final capture (S04) | | | ngoài lớp · gaps.md |
| Identity hashCode không duy nhất (pretest) | | | ngoài lớp · gaps.md |
| Map.of / Set.of / List.of (Java 9) (pretest) | | | ngoài lớp · gaps.md |
| Map.merge / compute* & null (pretest) | | | ngoài lớp · gaps.md |
| Checked vs unchecked theo hierarchy (pretest) | | | ngoài lớp · gaps.md |
| try-with-resources: thứ tự close (pretest) | | | ngoài lớp · gaps.md |
| Override & throws (pretest) | | | ngoài lớp · gaps.md |
| Chọn checked hay unchecked (pretest) | | | ngoài lớp · gaps.md |
| Precise rethrow (Java 7) (pretest) | | | ngoài lớp · gaps.md |
