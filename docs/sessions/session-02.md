# S02 – Control Flow, Arrays, Classes & Case Study 1

- **Module:** M1 – Java Fundamentals · **Ngày T7:** 19/09 · **Slide:** `session-2.pptx`
- **Demo:** `java playground/session02/ControlFlowPitfalls.java` (cần JDK 14+ vì có switch expression)

## 1. Tóm tắt 1 phút
1. Rẽ nhánh dùng `if/else` và `switch`. **switch expression** (Java 14+) dùng `->`, không fall-through, trả về giá trị (`yield` khi case là một block).
2. Vòng lặp: `for`, `while`, `do-while` (chạy ít nhất 1 lần), `for-each`. Điều khiển bằng `break`/`continue`, và **labeled break** để thoát nhiều vòng lồng nhau.
3. Mảng là **object**, kích thước cố định, `length` là property (không có ngoặc). Tiện ích nằm trong `java.util.Arrays`.
4. Class là blueprint, object là instance tạo bằng `new`. **Encapsulation**: field private, mở getter/setter khi cần.
5. Constructor có thể overload và **chaining** (`this()`/`super()` phải là lệnh đầu tiên). `static` thuộc về class, không thuộc về object.

## 2. Keyword checklist

### Control flow
- [ ] **if không có `{}`** chỉ ăn đúng 1 câu lệnh ngay sau nó, thụt lề không có ý nghĩa gì. Luôn viết `{}`.
- [ ] **switch fall-through**: case kiểu `:` mà thiếu `break` thì chạy tiếp xuống các case bên dưới.
- [ ] **switch expression** `case A, B -> ...;` · `yield` · compiler kiểm tra đủ case (exhaustive) khi switch trên enum.
- [ ] **Kiểu dùng được trong switch**: int và các kiểu nhỏ hơn (byte, short, char) cùng wrapper của chúng, `String`, `enum`. **Không** dùng được long/float/double/boolean. *(Java 21 có thêm pattern matching cho mọi kiểu reference.)*
- [ ] **for / while / do-while / for-each**: for-each chạy được trên mảng và mọi `Iterable`.
- [ ] **break / continue / labeled break (`break outer;`) / labeled continue**
- [ ] **Ternary** `cond ? a : b`

### Arrays
- [ ] **Khai báo**: `int[] a = new int[5];` · `int[] a = {1,2,3};`
- [ ] **Mảng là object** và nằm trên heap. Phần tử có giá trị mặc định (0 / false / null).
- [ ] **`arr.length`** (property) vs `str.length()` (method) vs `list.size()`
- [ ] **ArrayIndexOutOfBoundsException**
- [ ] **Arrays.sort / copyOf / fill / toString / deepToString / equals**. `println(arr)` chỉ in ra `[I@hash`.
- [ ] **Mảng 2 chiều** `int[3][4]` là *mảng của các mảng*. Có thể dùng **jagged array** (các hàng dài khác nhau).
- [ ] **Java luôn pass-by-value**: với object thì giá trị được copy là *reference*. Method sửa nội dung object thì caller thấy thay đổi, nhưng gán reference mới thì caller không thấy.

### Class & Object
- [ ] **Class vs Object**
- [ ] **Field (instance variable) vs local variable**
- [ ] **Constructor**: trùng tên class, không có kiểu trả về.
- [ ] **Default constructor**: compiler chỉ tự sinh khi class **không khai báo constructor nào**.
- [ ] **`this`**: trỏ tới object hiện tại, dùng để phân biệt field với tham số cùng tên.
- [ ] **Constructor overloading**: nhiều constructor khác nhau về danh sách tham số.
- [ ] **Constructor chaining** `this(...)` / `super(...)`: phải là **lệnh đầu tiên**, nên không dùng cả hai trong cùng một constructor. Gọi vòng tròn (A→B→A) sẽ **compile error** "recursive constructor invocation".
- [ ] **Nếu không gọi `super(...)`** thì compiler tự chèn `super()`. Nếu class cha không có constructor không tham số thì lỗi compile.
- [ ] **Access modifiers**: `private` (trong class) < *default / package-private* (trong package) < `protected` (package + subclass) < `public`. Nguyên tắc: bắt đầu từ mức hẹp nhất.
- [ ] **static field**: dùng chung cho mọi instance · **static method**: không truy cập được `this` và instance member · **static block**: chạy một lần khi class được initialize.
- [ ] **Encapsulation**: private field + getter/setter có validation. *Effective Java Item 15: Minimize accessibility.*

## 3. Quiz tự kiểm tra

**Q1.** Output?
```java
int x = 5;
if (x > 10)
    System.out.println("A");
    System.out.println("B");
```
<details><summary>Đáp án</summary>Chỉ in "B". Không có `{}` nên `if` chỉ ăn lệnh in "A".</details>

**Q2.** Output của `switch (10) { case 10: print("Ten"); case 20: print("Twenty"); default: print("Other"); }`?
<details><summary>Đáp án</summary>Ten, Twenty, Other: fall-through vì không có `break`.</details>

**Q3.** Viết lại Q2 bằng switch expression để trả về một String.
<details><summary>Đáp án</summary>

```java
String s = switch (x) { case 10 -> "Ten"; case 20 -> "Twenty"; default -> "Other"; };
```
</details>

**Q4.** Có switch được trên `long` hay `boolean` không?
<details><summary>Đáp án</summary>Không (với switch kiểu cổ điển). Chỉ dùng được int và các kiểu nhỏ hơn, wrapper của chúng, String và enum.</details>

**Q5.** Output?
```java
outer:
for (int i = 0; i < 3; i++)
  for (int j = 0; j < 3; j++) {
    if (j == 1) continue outer;
    if (i == 2) break outer;
    System.out.println(i + "," + j);
  }
```
<details><summary>Đáp án</summary>`0,0` rồi `1,0`. Đến i=2 thì ngay lần j=0 đã `break outer`.</details>

**Q6.** Sau `modify(arr, n)`, `arr` và `n` là gì? (method gán `arr[0]=99; arr=new int[]{-1}; n=100;`, ban đầu `arr={1,2,3}`, `n=1`)
<details><summary>Đáp án</summary>`[99, 2, 3]` và `n=1`. Pass-by-value: bản copy của reference vẫn sửa được object gốc, nhưng gán lại reference thì không ảnh hưởng tới caller.</details>

**Q7.** `int[] a = new int[3]; String[] s = new String[2];`: giá trị các phần tử? `a.length` hay `a.length()`?
<details><summary>Đáp án</summary>`[0,0,0]`, `[null,null]`. Viết `a.length` vì là property.</details>

**Q8.** Class chỉ có constructor `Product(String name)`. Gọi `new Product()` thì sao?
<details><summary>Đáp án</summary>Compile error. Đã khai báo constructor rồi thì compiler không sinh default constructor nữa.</details>

**Q9.** Constructor gọi được cả `this(...)` và `super(...)` không? Vì sao?
<details><summary>Đáp án</summary>Không. Cả hai đều bắt buộc là lệnh đầu tiên. Constructor nào gọi `this(...)` thì đến cuối chuỗi, constructor cuối cùng sẽ gọi `super(...)`.</details>

**Q10.** `protected` khác default (package-private) ở điểm nào?
<details><summary>Đáp án</summary>`protected` cho phép thêm subclass *ở package khác* truy cập (thông qua kế thừa). Default chỉ cho truy cập trong cùng package.</details>

**Q11.** Static method có gọi được instance method không?
<details><summary>Đáp án</summary>Không gọi trực tiếp được vì không có `this`. Muốn gọi thì phải có một object: `obj.method()`.</details>

**Q12.** Tại sao Case Study 1 dùng `StringBuilder` cho receipt, và định dạng "tên căn trái 20 ký tự, số căn phải" thế nào?
<details><summary>Đáp án</summary>Vì nối nhiều dòng trong vòng lặp. Định dạng dùng `String.format("%-20s%5d%12.2f%n", name, qty, price)`: `-` là căn trái, không có dấu `-` là căn phải.</details>

## 4. Bài tập & homework
- [ ] **Case Study 1** – Inventory & Order Line Calculator → làm trong ecommerce (xem backlog **S02**)
- [ ] Đọc *Effective Java* Item 15 – Minimize the accessibility of classes and members

## 5. Áp dụng vào ecommerce
Xem `docs/ecommerce-backlog.md` → **S02**.

## 6. Ghi chú cá nhân / câu hỏi mang lên lớp
- Slide 16 ghi "Can call in circle": thực tế gọi constructor vòng tròn là compile error. Hỏi lại mentor ý của slide.

## 7. Feedback review T4
- 
