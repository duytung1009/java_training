# Ecommerce Backlog

Mỗi buổi, project được **mở rộng hoặc refactor** chứ không làm lại từ đầu. Ví dụ `Product` ở S02 là class thường, đến S03 được refactor thành abstract class. Nhờ vậy khi review sẽ thấy rõ vì sao thiết kế thay đổi.

Backlog chỉ gồm task trong project ecommerce (module `domain/` và `app/`). Bài practice ở `playground/` không đưa vào backlog và không tính tiến độ.

Quy trình: tạo nhánh `sXX/...` → code → PR (dùng template) → review T4 → merge → `git tag sXX-done`.

---

## S01 – JVM, Data & String
**Nhánh:** `s01/text-utils`

| # | Task | Package |
|---|---|---|
| 1 | `TextUtils.toTitleCase`, `slugify(productName)` (ví dụ "Áo Thun Nam" → "ao-thun-nam", phần bỏ dấu là bonus) | `shared/text` |

> Đã merge (tag `s01-done`).

**Acceptance**
- [x] Không dùng `+=` với String trong vòng lặp, dùng `StringBuilder`
- [x] So sánh chuỗi bằng `equals`, xử lý input `null` / rỗng
- [x] Unit test cho mỗi method public

**Spec hành vi** (chốt 07/10, test: `domain/src/test/java/com/training/ecommerce/shared/text/TextUtilsTest.java`)

API: `public static String toTitleCase(String)`, `public static String slugify(String)` trong `com.training.ecommerce.shared.text.TextUtils` (utility class, constructor `private`).

| Method | Input | Kết quả mong đợi | Test |
|---|---|---|---|
| `toTitleCase` | `"áo sơ mi"`, `"hello world"`, `"123 abc"` | `"Áo Sơ Mi"`, `"Hello World"`, `"123 Abc"` | `capitalizesFirstLetterOfEachWord` |
| `toTitleCase` | `"hELLO woRLD"`, `"ÁO THUN NAM"` | `"Hello World"`, `"Áo Thun Nam"`: các chữ còn lại về chữ thường | `lowercasesRemainingLetters`, `convertsUppercaseProductName` |
| `toTitleCase` | `""`, `"   "`, `"  hello   world  "` | giữ nguyên mọi khoảng trắng | `preservesLeadingTrailingAndMultipleSpaces`, `keepsWhitespaceOnlyInput` |
| `toTitleCase` | `"hello\tworld"`, `"hello\nworld"`, NBSP | space, tab, newline và NBSP tách từ, và được giữ nguyên | `treatsTabAndNewlineAsSeparator`, `treatsNonBreakingSpaceAsSeparator` |
| `toTitleCase` | `"áo-thun nam"` | `"Áo-thun Nam"`: dấu `-` không tách từ | `doesNotTreatHyphenAsSeparator` |
| `toTitleCase` | `null` | `null` | `returnsNullForNull` |
| `slugify` | `"Hello World"`, `"iPhone 15 Pro"`, `""` | `"hello-world"`, `"iphone-15-pro"`, `""` | `convertsToLowercaseHyphenated` |
| `slugify` | `"Áo Thun Nam"`, `"ĐẠO ƯỚC"`, mọi nguyên âm có dấu, `đ` | `"ao-thun-nam"`, `"dao-uoc"`… (**bonus**) | `removesVietnameseDiacritics…`, `handlesUppercaseVietnameseCharacters` |
| `slugify` | `"hello   world"`, `"  hello world  "` | gộp thành một `-`, không có `-` ở đầu và cuối | `collapses…`, `trims…` |
| `slugify` | `"Hello, World!"`, `"Áo & quần"` | dấu câu bị loại: `"hello-world"`, `"ao-quan"` | `removesPunctuation`, `collapsesHyphensLeftByRemovedPunctuation` |
| `slugify` | `"ao-thun-nam"`, `"Ao_thun nam"`, `"Iphone—15"`, `"ao\tthun\nnam"` | mọi ký tự không phải chữ hoặc số đều là ký tự tách: `"ao-thun-nam"`, `"iphone-15"` | `treatsNonAlphanumericAsSeparator`, `treatsTabAndNewlineAsSeparator` |
| `slugify` | `"   "`, `" ?! "`, `"---"` | `""` | `returnsEmptyWhenOnlySpacesOrPunctuation` |
| `slugify` | `null` | `null` | `returnsNullForNull` |

Ngoài spec: ký tự có dấu không thuộc tiếng Việt (ví dụ `ñ`, `ü`), input Unicode dạng NFD, và các khoảng trắng khác (`\r`, `\f`, `\u000B`) trong `toTitleCase`.

**Kiểm khi review:** không dùng `+=` với String trong vòng lặp (dùng `StringBuilder`); so sánh chuỗi bằng `equals`; `TextUtils` có constructor `private`.

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

**Spec hành vi** (chốt 20/10, test: `domain/src/test/java/com/training/ecommerce/domain/{catalog/ProductTest, inventory/InventoryManagerTest, order/OrderLineTest, order/OrderTest}.java`, `app/src/test/java/com/training/ecommerce/interfaces/cli/ReceiptPrinterTest.java`)

API (quyết định 20/10: tiền dùng **`BigDecimal`** ngay từ S02, mọi số tiền trả về có scale 2, làm tròn HALF_UP):
- `Product(String id, String name, BigDecimal price, int stock)` · `Product(String id, String name, BigDecimal price)` (stock = 0, chaining) · `getId/getName/getPrice/getStock`
- `InventoryManager()` · `void addProduct(Product)` · `Product findById(String)` · `void decreaseStock(String productId, int quantity)`
- `OrderLine(Product, int quantity)` · `getProduct/getQuantity` · `BigDecimal lineTotal()`
- `Order(BigDecimal discountRate, BigDecimal taxRate)` · `void addLine(OrderLine)` · `OrderLine[] getLines()` · `subtotal/discountAmount/taxAmount/finalAmount()`
- `ReceiptPrinter.print(Order)` trả về `String`

| Method | Input | Kết quả mong đợi | Test |
|---|---|---|---|
| `Product` | `("P001","Áo thun nam",150000,50)` / bản 3 tham số | getter đúng giá trị; bản 3 tham số có stock 0 | `exposesGivenValues`, `defaultsStockToZero` |
| `Product` | price 0, stock 0 | hợp lệ | `acceptsZeroPrice`, `acceptsZeroStock` |
| `Product` | price âm/null, stock âm, id/name null/rỗng/blank (cả hai constructor) | `IllegalArgumentException` | `rejects…` |
| `findById` | id đã add, `new String("P001")` | đúng instance đã add (so bằng `equals`) | `returnsSameInstanceThatWasAdded`, `comparesIdByValueNotReference` |
| `findById` | id không tồn tại, `null`, kho rỗng | `null` (D2) | `returnsNullFor…` |
| `addProduct` | 15 product | mảng tự nới, tìm thấy đủ | `growsBeyondInitialCapacity` |
| `addProduct` | `null`, trùng id | `IllegalArgumentException`, product cũ giữ nguyên | `rejectsNullProduct`, `rejectsDuplicateId` |
| `decreaseStock` | P001 −3, giảm liên tiếp, giảm đúng bằng stock | 47 (thấy qua reference product), cộng dồn, về 0; product khác không đổi | `decreasesStockOfStoredProduct`, `accumulatesSuccessiveDecreases`, `allowsDecreasingToZero`, `doesNotAffectOtherProducts` |
| `decreaseStock` | quantity > stock | `IllegalStateException`, stock giữ nguyên (D3) | `throwsWhenInsufficient…` |
| `decreaseStock` | quantity 0/−1/−5, id không tồn tại/null | `IllegalArgumentException`, stock giữ nguyên | `rejectsNonPositiveQuantity`, `rejectsUnknownId`, `rejectsNullId` |
| `OrderLine` | quantity = stock | hợp lệ; tạo line **không** giảm stock | `allowsQuantityEqualToStock`, `doesNotChangeStock` |
| `OrderLine` | quantity > stock | `IllegalStateException` (D8: OrderLine có kiểm tồn kho) | `rejectsQuantityGreaterThanStock` |
| `OrderLine` | product null, quantity 0/−5 | `IllegalArgumentException` | `rejectsNullProduct`, `rejectsNonPositiveQuantity` |
| `lineTotal` | 150000×2, 12345.67×3, 19999.995×1, 0×5 | `300000.00`, `37037.01`, `20000.00`, `0.00` | `multipliesPriceByQuantity` |
| `Order` | chưa có line | mọi số tiền `0.00` | `emptyOrderIsZero` |
| `Order` | 2×P001 + 1×P002, discount 0.10, tax 0.08 | 750000.00 / 75000.00 / 54000.00 / 729000.00 (D4: thuế tính trên số tiền sau giảm) | `calculatesStandardOrder`, `taxIsCalculatedOnDiscountedAmount` |
| `Order` | 3×12345.67, discount 0.075, tax 0.08 | 37037.01 / 2777.78 / 2740.74 / 36999.97 (làm tròn từng bước; final = subtotal − discount + tax) | `roundsHalfUpAtEachStep` |
| `Order` | discount 0 / 1, tax 0 | biên đúng | `zeroDiscount`, `fullDiscount`, `zeroTax` |
| `Order` | discount ngoài [0,1], tax âm, rate null, `addLine(null)` | `IllegalArgumentException` | `Validation.*` |
| `getLines` | 2 line | độ dài 2, đúng thứ tự; sửa mảng trả về không ảnh hưởng Order | `returnsLinesInInsertionOrder`, `returnedArrayIsACopy` |
| `print` | mỗi line | `String.format("%-20s%5d%12.2f", name, qty, lineTotal)` (D5), đúng thứ tự | `formatsEachItemRow`, `printsItemRowsInOrder` |
| `print` | tên > 20 ký tự | giữ nguyên, không cắt (D6) | `keepsLongNameInFull` |
| `print` | luôn có | các dòng bắt đầu bằng `Subtotal`, `Discount`, `Tax`, `Total` kèm số tiền `%.2f` | `printsSummaryLines` |
| `print` | Order rỗng / `null` | in được, Total 0.00 / `IllegalArgumentException` | `printsEmptyOrder`, `rejectsNullOrder` |
| `print` | default locale `vi-VN` | vẫn dùng dấu chấm thập phân (D7, **bonus**) | `usesDotDecimalSeparatorRegardlessOfDefaultLocale` |

Ngoài spec: cùng product add 2 lần (gộp hay tách line), header receipt, cách hiển thị dấu của Discount, tax > 100%.

**Kiểm khi review:** catalog dùng `Product[]` (không `ArrayList`); field `private`; `Product` 3 tham số gọi `this(...)`; tên biến có nghĩa; `ReceiptPrinter` dùng `StringBuilder`; `Product` không mở setter tùy tiện cho stock (giảm có kiểm tra); `App` demo chạy được.

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
