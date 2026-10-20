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
| 6 | `S02Demo` | `demo` | demo: tạo catalog, đặt hàng, in receipt |

> Đã merge (tag `s02-done`).

**Acceptance** (theo tiêu chí chấm của slide)
- [x] Catalog dùng array
- [x] Công thức: subtotal = Σ(qty × price) → áp discount → cộng tax → final
- [x] Receipt: tên căn trái 20 ký tự, qty/price căn phải
- [x] Xử lý: product không tồn tại, thiếu hàng tồn, quantity âm hoặc bằng 0
- [x] Encapsulation đúng, có constructor chaining, tên biến có nghĩa
- [x] Bonus: dùng `BigDecimal` cho tiền thay vì `double` (xem S01 Q10). Nếu làm thì ghi rõ trong PR để bàn khi review.

**Spec hành vi** (chốt 20/10, test: `domain/src/test/java/com/training/ecommerce/domain/{catalog/ProductTest, inventory/InventoryManagerTest, order/OrderLineTest, order/OrderTest}.java`, `app/src/test/java/com/training/ecommerce/interfaces/cli/ReceiptPrinterTest.java`)

API (quyết định 20/10: tiền dùng **`BigDecimal`** ngay từ S02, mọi số tiền trả về có scale 2, làm tròn HALF_UP):
- `Product(String id, String name, BigDecimal price, int stock)` · `Product(String id, String name, BigDecimal price)` (stock = 0, chaining) · `getId/getName/getPrice/getStock`
- `InventoryManager()` · `void addProduct(Product)` · `Product findById(String)` · `void decreaseStock(Order order)` (sửa 20/10: thay cho `(productId, quantity)`)
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
| `decreaseStock(Order)` | 3×P001 + 2×P002; nhiều order liên tiếp; đặt đúng bằng tồn | 47 / 18 (thấy qua reference product), cộng dồn, về 0; product không có trong order không đổi; order rỗng không đổi | `decreasesStockForEachLine`, `accumulatesSuccessiveOrders`, `allowsDecreasingToZero`, `doesNotAffectProductsNotInOrder`, `emptyOrderChangesNothing` |
| `decreaseStock(Order)` | một line thiếu hàng; cùng product ở 2 line vượt tồn (30+30 > 50) | `IllegalStateException`, **all-or-nothing**: không line nào bị trừ; trùng product thì cộng dồn (20+30 = 50 → về 0) | `throwsAndChangesNothingWhenAnyLineIsInsufficient`, `accumulatesQuantityOfSameProductAcrossLines`, `allowsSameProductAcrossLinesWithinStock` |
| `decreaseStock(Order)` | `null`, line có product không có trong kho | `IllegalArgumentException`, kho giữ nguyên | `rejectsNullOrder`, `rejectsProductNotInInventory` |
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

**Kiểm khi review:** catalog dùng `Product[]` (không `ArrayList`); field `private`; `Product` 3 tham số gọi `this(...)`; tên biến có nghĩa; `ReceiptPrinter` dùng `StringBuilder`; `Product` không mở setter tùy tiện cho stock (giảm có kiểm tra); `S02Demo` chạy được.

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

> Đã merge (tag `s03-done`).

**Acceptance**
- [x] Demo: list có đủ các loại product → in tổng doanh thu
- [x] Thêm `BundleProduct` mà **không sửa** `PricingEngine` (chứng minh OCP)
- [x] Test: 2 product cùng id → `HashSet.size() == 1`
- [ ] Mô tả PR có mục "SOLID check": S/O/L/I/D, mỗi nguyên tắc 1 dòng _(chưa có PR: merge trực tiếp 21/10)_
- [x] Không có cây kế thừa sâu quá 3 tầng
- [x] Code S02 (`OrderLine`, `InventoryManager`) vẫn chạy với `Product` mới

**Spec hành vi** (chốt 20/10, test: `domain/src/test/java/com/training/ecommerce/domain/{catalog/PhysicalProductTest, catalog/DigitalProductTest, catalog/SubscriptionProductTest, catalog/BundleProductTest, catalog/ProductIdentityTest, pricing/DiscountCodeTest, customer/AddressTest}.java`, `app/src/test/java/com/training/ecommerce/application/PricingEngineTest.java`; nâng cấp test S02)

API (quyết định 20/10, theo khuyến nghị):
- `abstract class Product`: constructor `protected (id, name, basePrice, stock)` và `(id, name, basePrice)`; `getId/getName/getBasePrice/getStock`, `decreaseStock(int)`; `public abstract BigDecimal calculateFinalPrice()`; `equals`/`hashCode` **chỉ theo id** (final, bất kể subtype); `toString()` có id + tên. `getPrice()` đổi thành `getBasePrice()`.
- `PhysicalProduct(id, name, basePrice, stock, BigDecimal weightKg)` implements `Shippable`, `Discountable` · `DigitalProduct(id, name, basePrice, stock)` implements `Discountable` · `SubscriptionProduct(id, name, monthlyPrice, int months, stock)` · `BundleProduct(id, name, stock, BigDecimal discountRate, Product... items)`
- `Discountable.applyDiscount(DiscountCode)` → giá sau giảm · `DiscountCode(String code, BigDecimal rate)` (không có hạn dùng) · `Shippable.calculateShipping(Address)` · `Address(String street, String city)`
- `PricingEngine` (app, `application`): instance method `BigDecimal calculateRevenue(List<Product>)`
- `OrderLine` chụp giá từ `calculateFinalPrice()` (không phải basePrice). Mọi số tiền scale 2, HALF_UP.

| Method | Input | Kết quả mong đợi | Test |
|---|---|---|---|
| `calculateFinalPrice` | Physical/Digital giá 150.000 / 1.000.000 | bằng giá gốc: `150000.00`, `1000000.00` (chưa gồm giảm giá, phí ship) | `equalsBasePrice`, `finalPriceEqualsBasePrice` |
| `calculateFinalPrice` | Subscription 499.000 × 1 / × 12; 99999.995 × 2 | `499000.00`, `5988000.00`, `199999.99` | `multipliesMonthlyPriceByMonths` |
| `calculateFinalPrice` | Bundle (áo 150k + quần 450k, giảm 10%); (Office + Claude Pro 12 tháng, giảm 20%); bundle lồng bundle; giảm 0 / 15% | `540000.00`, `5590400.00`, `1463000.00`, `12345.67` / `10493.82` | `BundleProductTest.*` |
| `BundleProduct` | items rỗng / chứa null; discountRate null hoặc ngoài [0, 1] | `IllegalArgumentException` | `rejects…` |
| `applyDiscount` | áo 150k với rate 0.10 / 0.25 / 0.333 / 1; Office 1tr với 0.25 / 0.5 / 1 | `135000.00`, `112500.00`, `100050.00`, `0.00`; `750000.00`, `500000.00`, `0.00`; giá bán của product không đổi | `appliesRate`, `doesNotChangeFinalPrice` |
| `applyDiscount` | `null` | `IllegalArgumentException` | `rejectsNullCode` |
| `DiscountCode` | rate 0 / −0.1 / 1.01 / null; code null/rỗng; rate 1 | `IllegalArgumentException`; rate 1 hợp lệ | `DiscountCodeTest.*` |
| `calculateShipping` | áo 0.3 kg: "Hà Nội" / "Hồ Chí Minh"; 2.5 kg, 0.0001 kg, 0.3333 kg ở Hà Nội | 20.000 (HN) hoặc 35.000 + 5.000 × kg: `21500.00`, `36500.00`, `32500.00`, `20000.50`, `21666.50` | `innerCityFee`, `otherCityFee`, `feeDependsOnWeight` |
| `calculateShipping` | `null` | `IllegalArgumentException` | `rejectsNullAddress` |
| `PhysicalProduct` | weightKg 0 / −0.5 / null; giá âm | `IllegalArgumentException` | `rejectsNonPositiveWeight`, `rejectsNullWeight`, `keepsProductRules` |
| `SubscriptionProduct` | months 0 / −1; getBasePrice / getMonths | `IllegalArgumentException`; basePrice = giá tháng | `rejectsMonthsBelowOne`, `exposesMonthlyPriceAndMonths` |
| `Address` | street/city null/rỗng | `IllegalArgumentException` | `AddressTest.*` |
| `equals`/`hashCode` | 2 product cùng id (khác dữ liệu, khác subtype); khác id; null; khác kiểu | cùng id → bằng nhau, `HashSet.size() == 1`, đối xứng; khác id → khác; `equals(null)` / `equals("P001")` = false | `ProductIdentityTest.*` |
| `toString` | áo P001 | có `P001` và `Áo thun nam` | `toStringContainsIdAndName` |
| `calculateRevenue` | áo + quần + Office + Claude Pro 12 tháng; có bundle; subtype mới chỉ định nghĩa trong test (GiftCard 95%) | `7588000.00`; `1540000.00`; `625000.00` (không sửa PricingEngine – OCP) | `PricingEngineTest.*` |
| `calculateRevenue` | list rỗng / null / chứa null | `0.00` / `IllegalArgumentException` | `emptyListIsZero`, `rejectsNull…` |
| `OrderLine.lineTotal` | Subscription 499.000 × 12 tháng, quantity 2 | `11976000.00` | `usesFinalPriceForSubscription` |
| Test S02 | `new Product(...)` | nâng cấp: `ProductTest` test qua subclass ẩn danh (`getPrice` → `getBasePrice`); các test còn lại dùng `PhysicalProduct` 0.5 kg; kỳ vọng S02 giữ nguyên | các file test S02 |

Ngoài spec: `getBasePrice()` của BundleProduct, so sánh city không phân biệt hoa thường/dấu, hạn dùng mã giảm giá, giảm giá cho gói dài hạn.

**Kiểm khi review:** không dùng `instanceof` trong phần tính giá (`PricingEngine`, `BundleProduct`); `Product` là abstract, constructor `protected`; `DigitalProduct` không implement `Shippable` (ISP); cây kế thừa không quá 3 tầng; field `private`, không có setter thừa; mô tả PR có mục "SOLID check" (S/O/L/I/D mỗi nguyên tắc 1 dòng); `S03Demo` in tổng doanh thu cho list đủ các loại product.

## S04 – Generics & Collections
_Tự nghiên cứu, không có slide. Task ecommerce sẽ chốt khi soạn session S04._

---

## S05 – Functional Programming, Lambda & Stream: refactor + ReportService
**Nhánh:** `s05/stream-report` · bài tập cuối slide S05 (tự trình bày 17/10)

| # | Class/Task | Package | Ghi chú |
|---|---|---|---|
| 1 | `InventoryManager`: thay vòng lặp bằng Stream (`findById`, `addProduct`, `decreaseStock`) | `domain/inventory` | refactor S02, giữ nguyên hành vi đã spec; catalog có thể chuyển `Product[]` → `List<Product>` (ràng buộc array chỉ áp cho S02) |
| 2 | `Order`: `subtotal` bằng `reduce(BigDecimal.ZERO, BigDecimal::add)` | `domain/order` | refactor S02 |
| 3 | `PricingRule` (`@FunctionalInterface`, `BigDecimal apply(BigDecimal price)`, default method để ghép) + cách gộp một danh sách rule thành một rule | `domain/pricing` | mới; ví dụ rule: thành viên −5%, flash sale −20.000đ |
| 4 | `PricingEngine`: doanh thu bằng Stream; áp chuỗi `PricingRule` lên giá bán | `application` | refactor S03 |
| 5 | `Category` cho sản phẩm | `domain/catalog` | mới, phục vụ báo cáo theo category (cách gắn vào `Product` chốt ở spec) |
| 6 | `ReportService`: doanh thu theo category, top N sản phẩm bán chạy (theo số lượng), tách đơn lớn / nhỏ theo ngưỡng | `application` | mới; pure, không sửa input |
| 7 | `S05Demo` | `demo` | in giá sau chuỗi rule + 3 báo cáo |

**Acceptance**
- [ ] Không còn vòng `for`/`while` trong `InventoryManager`, `Order`, `PricingEngine`, `ReportService`; dùng Stream, method reference khi làm code dễ đọc hơn
- [ ] Mỗi quy tắc giá là một hàm; ghép bằng `andThen` / `reduce(identity, andThen)`; thêm rule mới không sửa engine (OCP)
- [ ] Báo cáo dùng `groupingBy` + `reducing` cho `BigDecimal`, `partitioningBy`, top N bằng `sorted` + `limit`; key trùng không ném exception (merge function hoặc `groupingBy`)
- [ ] Logic tính toán pure: không sửa list / `Order` đầu vào, không đọc giờ hệ thống; test không cần mock
- [ ] Test S02 + S03 vẫn xanh sau refactor (hoặc được nâng cấp có chủ đích, ghi rõ)
- [ ] Không `forEach` để add vào list bên ngoài, không đặt logic trong `peek`, không `Optional.get()` trần
- [ ] Bonus: `findById` trả `Optional<Product>` (đổi quyết định D2 của S02, chốt ở `java-start-backlog`)
