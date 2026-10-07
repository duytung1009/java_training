# Ecommerce Backlog

Mỗi buổi, project được **mở rộng hoặc refactor** chứ không làm lại từ đầu. Ví dụ `Product` ở S02 là class thường, đến S03 được refactor thành abstract class. Nhờ vậy khi review sẽ thấy rõ vì sao thiết kế thay đổi.

Quy trình: tạo nhánh `sXX/...` → code → PR (dùng template) → review T4 → merge → `git tag sXX-done`.

---

## S01 – JVM, Data & String
**Nhánh:** `s01/text-utils`

| # | Task | Package |
|---|---|---|
| 1 | `TextUtils.toTitleCase`, `slugify(productName)` (ví dụ "Áo Thun Nam" → "ao-thun-nam", phần bỏ dấu là bonus) | `shared/text` |
| 2 | `WordCounter`, `TextTransformer` (bài practice) | `playground/session01` |

**Acceptance**
- [ ] Không dùng `+=` với String trong vòng lặp, dùng `StringBuilder`
- [ ] So sánh chuỗi bằng `equals`, xử lý input `null` / rỗng
- [ ] Unit test cho mỗi method public

## S02 – Case Study 1: Inventory & Order Line Calculator
**Nhánh:** `s02/order-line-calculator`

| # | Class | Package | Ghi chú |
|---|---|---|---|
| 1 | `Product` (id, name, price, stock) | `domain/catalog` | private field, constructor chaining |
| 2 | `InventoryManager` | `domain/inventory` | catalog lưu bằng **`Product[]`** (không dùng ArrayList), `findById`, `decreaseStock` |
| 3 | `OrderLine` (product, quantity) | `domain/order` | `lineTotal()` |
| 4 | `Order` | `domain/order` | `subtotal → discount → tax → finalAmount` |
| 5 | `ReceiptPrinter` | `interfaces/cli` | StringBuilder + `String.format("%-20s%5d%12.2f")` |
| 6 | `App` | `interfaces/cli` | demo: tạo catalog, đặt hàng, in receipt |

**Acceptance** (theo tiêu chí chấm của slide)
- [ ] Catalog dùng array
- [ ] Công thức: subtotal = Σ(qty × price) → áp discount → cộng tax → final
- [ ] Receipt: tên căn trái 20 ký tự, qty/price căn phải
- [ ] Xử lý: product không tồn tại, thiếu hàng tồn, quantity âm hoặc bằng 0
- [ ] Encapsulation đúng, có constructor chaining, tên biến có nghĩa
- [ ] Bonus: dùng `BigDecimal` cho tiền thay vì `double` (xem S01 Q10). Nếu làm thì ghi rõ trong PR để bàn khi review.

## S03 – Case Study 2: Product Pricing (refactor Product)
**Nhánh:** `s03/product-pricing`

| # | Type | Package | Ghi chú |
|---|---|---|---|
| 1 | `abstract class Product` (id, name, basePrice, `abstract calculateFinalPrice()`) | `domain/catalog` | refactor từ S02 |
| 2 | `PhysicalProduct extends Product implements Shippable, Discountable` | `domain/catalog` | có weight |
| 3 | `DigitalProduct extends Product implements Discountable` | `domain/catalog` | không Shippable (ISP) |
| 4 | `SubscriptionProduct extends Product` | `domain/catalog` | months, monthlyPrice |
| 5 | `interface Discountable { applyDiscount(DiscountCode) }` | `domain/pricing` | |
| 6 | `interface Shippable { calculateShipping(Address) }` | `domain/shipping` | |
| 7 | `DiscountCode`, `Address` | `domain/pricing`, `domain/customer` | |
| 8 | `PricingEngine.calculateRevenue(List<Product>)` | `application` | không dùng `instanceof` trong phần tính giá |
| 9 | `equals`/`hashCode` theo `id`, `toString` | `domain/catalog` | |

**Acceptance**
- [ ] Demo: list có đủ các loại product → in tổng doanh thu
- [ ] Thêm `BundleProduct` mà **không sửa** `PricingEngine` (chứng minh OCP)
- [ ] Test: 2 product cùng id → `HashSet.size() == 1`
- [ ] Mô tả PR có mục "SOLID check": S/O/L/I/D, mỗi nguyên tắc 1 dòng
- [ ] Không có cây kế thừa sâu quá 3 tầng
- [ ] Code S02 (`OrderLine`, `InventoryManager`) vẫn chạy với `Product` mới

## S04 – Generics & Collections: List
_Chờ buổi học. Dự kiến: thay `Product[]` bằng `List<Product>`, có thể tạo generic `Repository<T, ID>` ở `infrastructure/persistence/inmemory`._
