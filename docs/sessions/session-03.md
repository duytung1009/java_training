# S03 – OOP Principles & SOLID

- **Module:** M2 – OOP Concepts and Implementation · **Ngày T7:** 26/09 · **Slide:** `session-3-v3.pptx`
- **Demo:** `java playground/session03/OopPitfalls.java`

## 1. Tóm tắt 1 phút
1. **4 trụ cột OOP**: Encapsulation (ẩn *state*), Abstraction (ẩn *implementation*, chỉ lộ ra hành vi), Inheritance (is-a), Polymorphism (cùng một lời gọi, hành vi khác nhau).
2. **Overloading** được quyết định lúc *compile*, theo kiểu khai báo của tham số. **Overriding** được quyết định lúc *runtime*, theo kiểu thật của object (dynamic dispatch).
3. **Abstract class** dùng cho quan hệ "is a type of", có state chung. **Interface** dùng cho quan hệ "can do", một class implement được nhiều interface.
4. Override `equals()` thì **bắt buộc** override `hashCode()` luôn.
5. **SOLID** là guideline, không phải luật cứng. Anti-pattern cần tránh: God class, cây kế thừa sâu hơn 3 tầng.

## 2. Keyword checklist

### 4 trụ cột
- [ ] **Encapsulation**: private + getter/setter + validation. (Reflection vẫn phá được private.)
- [ ] **Abstraction** khác Encapsulation: abstraction là *đơn giản hóa* (lộ ra cái gì), encapsulation là *bảo vệ* (giấu và kiểm soát state).
- [ ] **Inheritance**: `extends` (mỗi class chỉ extends được 1 class), `super`, quan hệ **is-a**. *Composition over inheritance* khi quan hệ thực chất là has-a.
- [ ] **Polymorphism**: compile-time (overloading) vs runtime (overriding).
- [ ] **Upcasting** (ngầm định, luôn an toàn) / **Downcasting** (phải ép tường minh, có thể gặp `ClassCastException`).
- [ ] **`instanceof` pattern matching** `if (a instanceof Dog d)`: chính thức từ Java 16.
- [ ] **"Program to interface, not implementation"**

### Overriding
- [ ] **@Override**: nhờ annotation này, compiler báo lỗi nếu signature không khớp.
- [ ] **Signature** = tên + danh sách kiểu tham số. **Covariant return type** được phép (trả về subtype).
- [ ] **Không được thu hẹp access** (ví dụ public → protected là lỗi).
- [ ] **Checked exception**: method override không được throw checked exception rộng hơn method cha.
- [ ] **Không override được** method `final`, `static` (static chỉ bị *hiding*), `private`.
- [ ] **Field hiding**: field **không có polymorphism**, được chọn theo kiểu của *reference*. Vì vậy tránh đặt field trùng tên với class cha.
- [ ] **Dynamic dispatch**: gọi method trong class cha vẫn chạy bản override của class con (Template Method).

### Overloading
- [ ] Khác nhau ở **danh sách tham số**. Chỉ khác kiểu trả về là lỗi compile.
- [ ] **Varargs** `String... items` phải là tham số cuối cùng.
- [ ] **Thứ tự chọn overload**: khớp chính xác → widening → boxing → varargs.
- [ ] Overload chọn theo kiểu **compile-time**: `Object o = "s"; print(o)` sẽ gọi `print(Object)`.
- [ ] **`print(null)` bị ambiguous** khi có nhiều overload kiểu reference mà không cái nào cụ thể hơn cái nào.

### Abstract class & Interface
- [ ] **abstract class**: không `new` được. Có thể có field, constructor, method có code và method abstract.
- [ ] **interface**: method mặc định là `public abstract`, hằng số mặc định là `public static final`.
- [ ] **default method** (Java 8, dùng để thêm method mà không phá các class đang implement) · **static method** (Java 8) · **private method** (Java 9).
- [ ] **Diamond với default method**: nếu 2 interface có default method trùng signature thì class phải override lại, có thể gọi `A.super.m()`.
- [ ] **Functional interface**: có đúng 1 abstract method, là nền tảng của lambda (học ở M5).
- [ ] **Rule of thumb**: "Is it a type of?" → abstract class · "Can it do?" → interface.

### Object methods
- [ ] **equals() contract**: reflexive, symmetric, transitive, consistent, `x.equals(null) == false`.
- [ ] **hashCode() contract**: hai object equal thì bắt buộc có hashCode bằng nhau (chiều ngược lại không bắt buộc). Vi phạm thì `HashSet`/`HashMap` chứa trùng hoặc tìm không thấy.
- [ ] **`Objects.equals` / `Objects.hash`**
- [ ] **toString()**: dùng khi debug/log. Nếu không override sẽ in `ClassName@hexHash`.

### SOLID
- [ ] **S** – Single Responsibility: mỗi class chỉ có *một lý do để thay đổi*.
- [ ] **O** – Open/Closed: thêm tính năng bằng cách mở rộng (class mới), không sửa code cũ.
- [ ] **L** – Liskov Substitution: object của class con thay được class cha mà không làm hỏng hành vi (ví dụ kinh điển: Square/Rectangle).
- [ ] **I** – Interface Segregation: dùng interface nhỏ, tập trung. Không ép class implement method nó không cần.
- [ ] **D** – Dependency Inversion: phụ thuộc vào abstraction, không phụ thuộc vào class cụ thể.
- [ ] **Anti-pattern**: God class, cây kế thừa sâu hơn 3 tầng.

## 3. Quiz tự kiểm tra
Dùng chung đoạn code sau cho Q1–Q4:
```java
class Animal { String name = "animal"; static String kind(){return "Animal";}
               String sound(){return "...";} String describe(){return name + " says " + sound();} }
class Dog extends Animal { String name = "dog"; static String kind(){return "Dog";}
               @Override String sound(){return "woof";} }
Animal a = new Dog();
```

**Q1.** `a.sound()`?
<details><summary>Đáp án</summary>"woof". Overriding được quyết định lúc runtime theo kiểu thật của object.</details>

**Q2.** `a.name` và `((Dog) a).name`?
<details><summary>Đáp án</summary>"animal" và "dog". Field không có polymorphism, được chọn theo kiểu của reference.</details>

**Q3.** `a.describe()`?
<details><summary>Đáp án</summary>"animal says woof". `name` trong `Animal` là field của Animal, còn `sound()` được dispatch sang Dog.</details>

**Q4.** Static method `kind()` được override hay hide?
<details><summary>Đáp án</summary>Hide. Static method được gọi theo kiểu compile-time, không có dynamic dispatch.</details>

**Q5.** Có các overload `print(Object)`, `print(String)`, `print(long)`, `print(Integer)`, `print(int...)`. Gọi `print(5)` thì chạy overload nào?
<details><summary>Đáp án</summary>`print(long)`: widening được ưu tiên hơn boxing, và boxing được ưu tiên hơn varargs.</details>

**Q6.** Với các overload ở Q5, `print(null)` có compile được không?
<details><summary>Đáp án</summary>Không, vì ambiguous: `String`, `Integer`, `int[]` đều nhận được null và không cái nào là subtype của cái nào. Nếu chỉ có `Object` và `String` thì chọn `String`.</details>

**Q7.** Method cha là `public void pay() throws IOException`. Method override trong class con có được viết `protected` không? Có được `throws Exception` không?
<details><summary>Đáp án</summary>Cả hai đều không: không được thu hẹp access, không được throw checked exception rộng hơn. Được phép throw hẹp hơn (`FileNotFoundException`), không throw gì, hoặc throw unchecked exception.</details>

**Q8.** Chỉ khác kiểu trả về (`int sum(int a)` và `long sum(int a)`) có phải là overload không?
<details><summary>Đáp án</summary>Không, đây là compile error vì trùng signature.</details>

**Q9.** Override `equals` theo `id` nhưng quên `hashCode`, sau đó add 2 product cùng id vào `HashSet`. `size()` bằng bao nhiêu?
<details><summary>Đáp án</summary>Thường là 2, vì hashCode mặc định khác nhau nên 2 object rơi vào 2 bucket khác nhau. Đây là vi phạm contract.</details>

**Q10.** Abstract class có constructor không? Để làm gì?
<details><summary>Đáp án</summary>Có. Dùng để khởi tạo state chung, được gọi qua `super(...)` từ constructor của class con.</details>

**Q11.** Interface có field instance không? Có private method không?
<details><summary>Đáp án</summary>Không có field instance, chỉ có hằng số `public static final`. Có private method (từ Java 9), dùng để tái sử dụng code giữa các default method.</details>

**Q12.** Hai interface A, B cùng có `default void hello()`. Class C implements A, B thì sao?
<details><summary>Đáp án</summary>Compile error, trừ khi C override `hello()`. Trong đó có thể gọi `A.super.hello()`.</details>

**Q13.** Trong Case Study 2, vì sao `DigitalProduct` không implement `Shippable`? Đây là nguyên tắc nào?
<details><summary>Đáp án</summary>ISP: không ép class implement hành vi nó không có. (Đồng thời thể hiện LSP: tránh viết `calculateShipping` rồi throw `UnsupportedOperationException`.)</details>

**Q14.** Thêm `BundleProduct` mà không phải sửa `PricingEngine`. Đây là nguyên tắc nào, và cần điều kiện gì?
<details><summary>Đáp án</summary>OCP, cùng với DIP. `PricingEngine` chỉ phụ thuộc vào abstraction `Product` và gọi `calculateFinalPrice()` một cách đa hình, không dùng `instanceof` hay `switch` theo type.</details>

## 4. Bài tập & homework
- [ ] Practice: Shape → Circle, Rectangle, Triangle (`area()`, `perimeter()`, `describe()`) → `playground/session03/`
- [ ] Homework: thêm subclass cho Shape (Square? Ellipse?). Nghĩ xem Square extends Rectangle có vi phạm LSP không.
- [ ] **Case Study 2** – Product Pricing → làm trong ecommerce (xem backlog **S03**)

## 5. Áp dụng vào ecommerce
Xem `docs/ecommerce-backlog.md` → **S03**.

## 6. Ghi chú cá nhân / câu hỏi mang lên lớp
- 

## 7. Feedback review T4
- 
