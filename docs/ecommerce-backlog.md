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
| `decreaseStock(Order)` | một line thiếu hàng; cùng product ở 2 line vượt tồn (30+30 > 50) | `IllegalStateException` *(nay `OutOfStockException`, xem Extra – Gom lỗi nghiệp vụ)*, **all-or-nothing**: không line nào bị trừ; trùng product thì cộng dồn (20+30 = 50 → về 0) | `throwsAndChangesNothingWhenAnyLineIsInsufficient`, `accumulatesQuantityOfSameProductAcrossLines`, `allowsSameProductAcrossLinesWithinStock` |
| `decreaseStock(Order)` | `null`, line có product không có trong kho | `IllegalArgumentException` (product không có trong kho nay là `ProductNotFoundException`, xem Extra – Gom lỗi nghiệp vụ), kho giữ nguyên | `rejectsNullOrder`, `rejectsProductNotInInventory` |
| `OrderLine` | quantity = stock | hợp lệ; tạo line **không** giảm stock | `allowsQuantityEqualToStock`, `doesNotChangeStock` |
| `OrderLine` | quantity > stock | `IllegalStateException` (D8: OrderLine có kiểm tồn kho; nay `OutOfStockException`, xem Extra – Gom lỗi nghiệp vụ) | `rejectsQuantityGreaterThanStock` |
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
| 8 | `PricingEngine.calculateRevenue(List<Product>)` | `application/pricing` (từ 28/10) | không dùng `instanceof` trong phần tính giá |
| 9 | `equals`/`hashCode` theo `id`, `toString` | `domain/catalog` | |

> Đã merge (tag `s03-done`).

**Acceptance**
- [x] Demo: list có đủ các loại product → in tổng doanh thu
- [x] Thêm `BundleProduct` mà **không sửa** `PricingEngine` (chứng minh OCP)
- [x] Test: 2 product cùng id → `HashSet.size() == 1`
- [–] Mô tả PR có mục "SOLID check": S/O/L/I/D, mỗi nguyên tắc 1 dòng _(bỏ qua: merge trực tiếp 21/10, không có PR)_
- [x] Không có cây kế thừa sâu quá 3 tầng
- [x] Code S02 (`OrderLine`, `InventoryManager`) vẫn chạy với `Product` mới

**Spec hành vi** (chốt 20/10, test: `domain/src/test/java/com/training/ecommerce/domain/{catalog/PhysicalProductTest, catalog/DigitalProductTest, catalog/SubscriptionProductTest, catalog/BundleProductTest, catalog/ProductIdentityTest, pricing/DiscountCodeTest, customer/AddressTest}.java`, `app/src/test/java/com/training/ecommerce/application/pricing/PricingEngineTest.java`; nâng cấp test S02)

API (quyết định 20/10, theo khuyến nghị):
- `abstract class Product`: constructor `protected (id, name, basePrice, stock)` và `(id, name, basePrice)`; `getId/getName/getBasePrice/getStock`, `decreaseStock(int)`; `public abstract BigDecimal calculateFinalPrice()`; `equals`/`hashCode` **chỉ theo id** (final, bất kể subtype); `toString()` có id + tên. `getPrice()` đổi thành `getBasePrice()`.
- `PhysicalProduct(id, name, basePrice, stock, BigDecimal weightKg)` implements `Shippable`, `Discountable` · `DigitalProduct(id, name, basePrice, stock)` implements `Discountable` · `SubscriptionProduct(id, name, monthlyPrice, int months, stock)` · `BundleProduct(id, name, stock, BigDecimal discountRate, Product... items)`
- `Discountable.applyDiscount(DiscountCode)` → giá sau giảm · `DiscountCode(String code, BigDecimal rate)` (không có hạn dùng) · `Shippable.calculateShipping(Address)` · `Address(String street, String city)`
- `PricingEngine` (app, `application.pricing` từ 28/10): instance method `BigDecimal calculateRevenue(List<Product>)`
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

## S04 – Generics, Collections & Exception: Repository, Cart, domain exception
**Nhánh:** `s04/collections-repository` · tự nghiên cứu (03/10), không có slide

| # | Class/Task | Package | Ghi chú |
|---|---|---|---|
| 1 | `Identifiable<ID>` (`ID getId()`) + `Repository<T extends Identifiable<ID>, ID>`: `save`, `findById`, `findAll`, `deleteById`, `count` | `shared/repository` | mới, generic; `Product implements Identifiable<String>` |
| 2 | `InMemoryRepository<T extends Identifiable<ID>, ID>` dùng `Map<ID, T>` | `infrastructure/persistence/inmemory` (app) | mới; giữ thứ tự thêm |
| 3 | `InventoryManager`: catalog `Product[]` → `Map<String, Product>` | `domain/inventory` | refactor S02, giữ nguyên hành vi đã spec |
| 4 | Domain exception unchecked: `ProductNotFoundException`, `OutOfStockException` (kèm productId, requested, available) | `domain/catalog` | thay `IllegalArgumentException` / `IllegalStateException` đang ném trong InventoryManager/OrderLine |
| 5 | `Cart`: `add(product, qty)` cộng dồn, `remove`, `changeQuantity` (về 0 thì bỏ), `getItems()` không sửa được từ ngoài, `toOrder(discountRate, taxRate)` | `domain/order` | mới; `Map` giữ thứ tự thêm |
| 6 | `S04Demo` | `demo` | catalog qua Repository, giỏ hàng, lỗi thiếu hàng / không tìm thấy |

> Đã merge (tag `s04-done`).

**Acceptance**
- [x] `Repository` generic dùng được cho `Product` mà không ép kiểu; không có raw type
- [x] `InventoryManager` không còn `Product[]`; tra theo id bằng `Map`
- [x] `Cart` gộp cùng sản phẩm bằng `Map.merge` (hoặc tương đương); `getItems()` trả về view / bản copy không sửa được
- [x] Domain exception là unchecked, message rõ ràng; code/test S02–S03 vẫn xanh (hoặc nâng cấp có chủ đích, ghi rõ)
- [x] Không sửa collection trong lúc for-each (dùng `removeIf` / `Iterator.remove`)
- [x] Key của `HashMap`/`HashSet` là bất biến (id `final`)
- [x] Bonus: API nhận nhiều product dùng wildcard, ví dụ `saveAll(Collection<? extends T>)`

**Spec hành vi** (chốt 21/10, test: `app/src/test/java/com/training/ecommerce/infrastructure/persistence/inmemory/InMemoryRepositoryTest.java`, `domain/src/test/java/com/training/ecommerce/domain/{order/CartTest, catalog/DomainExceptionTest}.java`; test S02–S03 giữ nguyên)

API (quyết định 21/10): `Identifiable<ID>` + `Repository<T extends Identifiable<ID>, ID>` (`save`, `saveAll(Collection<? extends T>)`, `findById` → `null` khi không có (S05: `Optional`), `findAll`, `deleteById`, `count`) ở `shared/repository`; `InMemoryRepository` ở app `infrastructure/persistence/inmemory`; `Product implements Identifiable<String>`. Exception **ở `domain/catalog`** (tránh vòng phụ thuộc catalog ↔ inventory): `ProductNotFoundException extends IllegalArgumentException` (`getProductId`), `OutOfStockException extends IllegalStateException` (`getProductId/getRequested/getAvailable`) *(nay cả hai extends `BusinessException`, xem Extra – Gom lỗi nghiệp vụ)*. `Cart` ở `domain/order`. Chưa dùng `Optional`/Stream (S05).

| Method | Input | Kết quả mong đợi | Test |
|---|---|---|---|
| `save` / `findById` | lưu áo, Office; tìm `"P001"`, `new String("P001")` | `save` trả chính entity; `findById` trả đúng instance (so id theo giá trị) | `savesAndFindsSameInstance`, `findsByIdValue` |
| `findById` | id không có / `null` | `null` (D1; **S05 đổi sang `Optional.empty()`**, xem Spec S05 D2) | `returnsNullWhenMissing` (S05: `returnsEmptyWhenMissing`) |
| `save` | cùng id P001 lần 2 | ghi đè (upsert, D2), `count` không đổi, giữ vị trí cũ trong `findAll` | `upsertsSameId` |
| `save` | `null` | `IllegalArgumentException` | `rejectsNullEntity` |
| `findAll` / `count` | thêm Office, áo, quần; repo rỗng | đúng thứ tự thêm; rỗng → list rỗng, count 0; list trả về không sửa được (UOE), repo không đổi | `findAllKeepsInsertionOrder`, `emptyRepository`, `findAllIsUnmodifiable` |
| `deleteById` | có / xóa lần 2 / không có | `true` rồi `false`, `false`; entity biến mất, count giảm | `deletesById` |
| Generic | `Repository<Customer, Long>` (entity khác, id Long) | dùng được không ép kiểu | `worksWithOtherEntityType` |
| `saveAll` | `List<PhysicalProduct>` vào `Repository<Product, String>` | lưu đủ (wildcard `? extends T`) — **bonus** | `saveAllAcceptsSubtypeCollection` |
| `Cart.add` | áo 2; thêm áo 3; cùng id khác instance; thứ tự quần → áo → tất → quần | 2 → 5 (cộng dồn, 1 dòng); cộng dồn theo id; `getItems` theo thứ tự thêm, thêm lại không đổi vị trí | `addsProduct`, `accumulatesSameProduct`, `accumulatesByProductId`, `keepsInsertionOrder` |
| `Cart.add` | `null`; quantity 0 / −1 | `IllegalArgumentException`, giỏ giữ nguyên | `rejectsNullProduct`, `rejectsNonPositiveQuantity` |
| `Cart.add` | quần 21 (tồn 20); áo 30 rồi thêm 21 (tồn 50); quần 15 + 5 | `OutOfStockException` (P002, 21, 20), giỏ rỗng; `OutOfStockException` (requested 51, available 50), giỏ giữ 30 (D4); đúng bằng tồn → hợp lệ | `rejectsQuantityOverStock`, `rejectsAccumulatedQuantityOverStock`, `allowsQuantityEqualToStock` |
| `changeQuantity` | 2 → 10; → 0; −1; sản phẩm chưa có; vượt tồn | đặt lại 10; bỏ khỏi giỏ; IAE giữ nguyên; `ProductNotFoundException` (D5); `OutOfStockException` giữ số cũ | `setsQuantity`, `zeroRemovesProduct`, `rejectsNegativeQuantity`, `rejectsProductNotInCart`, `rejectsChangeOverStock` |
| `remove` / `getQuantity` | remove có / chưa có; getQuantity chưa có | bỏ / không làm gì (D5); `0` | `removesProduct`, `quantityOfMissingProductIsZero` |
| `getItems` | `put` từ bên ngoài | `UnsupportedOperationException`, giỏ không đổi | `itemsAreUnmodifiable` |
| `toOrder` | áo 1, quần 1, áo 1; discount 0.10, tax 0.08 | 2 OrderLine (áo 2, quần 1) theo thứ tự thêm; final `729000.00`; không trừ kho | `createsOrderFromItems`, `doesNotDecreaseStock` |
| `toOrder` | giỏ rỗng; rate 1.5 | `IllegalStateException` (D6); `IllegalArgumentException` | `rejectsEmptyCart`, `rejectsInvalidRates` |
| Exception | `new OutOfStockException("P002", 21, 20)`, `new ProductNotFoundException("P999")` | unchecked, đúng lớp cha (ISE / IAE; nay `BusinessException` + category + code, xem Extra – Gom lỗi nghiệp vụ), getter đúng, message có id | `outOfStockCarriesData`, `notFoundCarriesId` |
| Nơi ném | `Product.decreaseStock(21)` (tồn 20); `new OrderLine(áo, 51)`; `InventoryManager.decreaseStock` 30 + 30 áo; product không có trong kho | `OutOfStockException` (21/20), (51/50), (60/50 – cộng dồn); `ProductNotFoundException` (P002) | `productDecreaseStock`, `orderLineOverStock`, `inventoryOutOfStock`, `inventoryProductNotFound` |

Ngoài spec: `Repository.saveAll(null)`, `Cart.changeQuantity(null, …)` trả về exception khác IAE, hiệu năng.

**Kiểm khi review:** `InventoryManager` không còn `Product[]` (dùng `Map`); không raw type; không sửa collection trong lúc for-each (`removeIf` / `Iterator.remove`); `Cart` cộng dồn bằng `Map.merge` (hoặc tương đương) với `Map` giữ thứ tự thêm; key bất biến (id `final`); `S04Demo` chạy được; module `domain` không import module khác (`domain.**` dùng `shared.repository` là hợp lệ vì cùng module).

---

## S05 – Functional Programming, Lambda & Stream: refactor + ReportService
**Nhánh:** `s05/stream-report` · bài tập cuối slide S05 (tự trình bày 17/10)

| # | Class/Task | Package | Ghi chú |
|---|---|---|---|
| 1 | `InventoryManager`: thay vòng lặp bằng Stream (`findById`, `addProduct`, `decreaseStock`) | `domain/inventory` | refactor S02/S04 (catalog đã là `Map` từ S04), giữ nguyên hành vi đã spec |
| 2 | `Order`: `subtotal` bằng `reduce(BigDecimal.ZERO, BigDecimal::add)` | `domain/order` | refactor S02 |
| 3 | `PricingRule` (`@FunctionalInterface`, `BigDecimal apply(BigDecimal price)`, default method để ghép) + cách gộp một danh sách rule thành một rule | `domain/pricing` | mới; ví dụ rule: thành viên −5%, flash sale −20.000đ |
| 4 | `PricingEngine`: doanh thu bằng Stream; áp chuỗi `PricingRule` lên giá bán | `application/pricing` (từ 28/10) | refactor S03 |
| 5 | `Category` cho sản phẩm | `domain/catalog` | mới, phục vụ báo cáo theo category (cách gắn vào `Product` chốt ở spec) |
| 6 | `ReportService`: doanh thu theo category, top N sản phẩm bán chạy (theo số lượng), tách đơn lớn / nhỏ theo ngưỡng | `application/report` (từ 28/10) | mới; pure, không sửa input |
| 7 | `S05Demo` | `demo` | in giá sau chuỗi rule + 3 báo cáo |

> Đã merge (tag `s05-done`).

**Acceptance**
- [x] Không còn vòng `for`/`while` trong `InventoryManager`, `Order`, `PricingEngine`, `ReportService`; dùng Stream, method reference khi làm code dễ đọc hơn. Ngoại lệ (chốt 21/10): bước có side effect (trừ tồn kho trong `decreaseStock`) dùng vòng `for` cho rõ ý, không dùng `forEach`
- [x] Mỗi quy tắc giá là một hàm; ghép bằng `andThen` / `reduce(identity, andThen)`; thêm rule mới không sửa engine (OCP)
- [x] Báo cáo dùng `groupingBy` + `reducing` cho `BigDecimal`, `partitioningBy`, top N bằng `sorted` + `limit`; key trùng không ném exception (merge function hoặc `groupingBy`)
- [x] Logic tính toán pure: không sửa list / `Order` đầu vào, không đọc giờ hệ thống; test không cần mock
- [x] Test S02 + S03 vẫn xanh sau refactor (hoặc được nâng cấp có chủ đích, ghi rõ)
- [x] Không `forEach` để add vào list bên ngoài, không đặt logic trong `peek`, không `Optional.get()` trần
- [x] `findById` trả `Optional` ở `InventoryManager` và `Repository` (đổi D1 của S04, chốt 21/10)

**Spec hành vi** (chốt 21/10, test: `domain/src/test/java/com/training/ecommerce/domain/{catalog/ProductCategoryTest, inventory/InventoryManagerSearchTest, pricing/PricingRuleTest}.java`, `app/src/test/java/com/training/ecommerce/application/{pricing/PricingEngineRuleTest, report/ReportServiceTest}.java`; nâng cấp có chủ đích `InventoryManagerTest`, `InMemoryRepositoryTest` cho `Optional` (D2), các test S02–S04 khác giữ nguyên)

API (quyết định 21/10):
- **D1** `enum Category { FASHION, ELECTRONICS, SOFTWARE, SUBSCRIPTION, OTHER }` ở `domain/catalog`. Mỗi subtype thêm constructor có `Category` ở cuối (`BundleProduct`: trước varargs `items`); constructor cũ giữ nguyên, mặc định `OTHER`. `Product.getCategory()`; category `null` → IAE.
- **D2** `InventoryManager.findById` và `Repository.findById` trả `Optional` (đổi D1 của S04). `InventoryManager.search(Predicate<Product>)` → list mới theo thứ tự thêm.
- `PricingRule` (`@FunctionalInterface`) ở `domain/pricing`: `apply`, default `andThen(next)`, static `identity()`, `percentOff(rate)` với rate trong (0, 1], `amountOff(amount)` với amount ≥ 0, `chain(List<PricingRule>)` = `reduce(identity, andThen)`. Rule giảm giá trả scale 2 HALF_UP.
- `PricingEngine.calculateRevenue(products, rule)` = Σ `rule.apply(calculateFinalPrice())` từng product, scale 2 (overload S03 giữ nguyên).
- `ReportService` ở `application.report` (từ 28/10): `revenueByCategory(List<Order>)` → `Map<Category, BigDecimal>` (Σ `lineTotal`, trước giảm giá/thuế của đơn); `topSellingProducts(orders, n)` → `List<Product>` (**D4**); `partitionByFinalAmount(orders, threshold)` → `Map<Boolean, List<Order>>`.
- Tham số `null` → IAE ở mọi method mới.

| Method | Input | Kết quả mong đợi | Test |
|---|---|---|---|
| `Category` | `values()` | đúng 5 giá trị theo thứ tự trên | `categoryValues` |
| Constructor cũ | Physical / Digital / Subscription / Bundle | `getCategory()` = `OTHER` | `oldConstructorsDefaultToOther` |
| Constructor mới | áo FASHION, Office SOFTWARE, Claude Pro SUBSCRIPTION, combo FASHION | lưu đúng category; weight/stock/giá như cũ (5988000.00, combo 1035000.00) | `storesCategory` |
| Constructor mới | category `null`; giá âm / months 0 | IAE (mọi subtype); validation cũ vẫn chạy | `rejectsNullCategory`, `keepsOldValidation` |
| `findById` | có / không có / `null` / kho rỗng | `Optional` chứa đúng instance; `Optional.empty()` (D2) | `presentWhenFound`, `emptyWhenMissing`, `InventoryManagerTest$FindById`, `InMemoryRepositoryTest` |
| `search` | category FASHION; `inStock.and(under500k)`, `inStock.negate()` | `[áo, quần]` theo thứ tự thêm; `[áo, quần]`, `[chuột]` | `filtersByCategoryInInsertionOrder`, `combinesPredicates` |
| `search` | không khớp; kho rỗng; sửa list trả về; `null` | list rỗng; catalog không đổi; IAE | `emptyWhenNothingMatches`, `resultIsSnapshot`, `rejectsNullCriteria` |
| `identity` / lambda | 150000 | giữ giá; gán lambda vào `PricingRule` được | `identityKeepsPrice`, `acceptsLambda` |
| `percentOff` | 10% của 150000; 15% của 12345.67; 100% | `135000.00`; `10493.82` (HALF_UP); `0.00` | `tenPercent`, `roundsHalfUp`, `fullDiscount` |
| `percentOff` | 0, −0.1, 1.01, `null` | IAE | `rejectsInvalidRate`, `rejectsNullRate` |
| `amountOff` | −50000; −200000; −0 trên 150000 | `100000.00`; `0.00`, không âm (**D6**); `150000.00` | `subtractsAmount`, `neverNegative`, `zeroAmount` |
| `amountOff` | âm / `null` | IAE | `rejectsInvalidAmount` |
| `andThen` | 10% rồi −50000; −50000 rồi 10%; với lambda; `null` | `85000.00`; `90000.00` (thứ tự quan trọng); chạy được; IAE | `andThenOrderMatters`, `andThenWithLambda`, `andThenRejectsNull` |
| `chain` | 2 rule hai thứ tự; 3 rule; `List.of()`; `null` / chứa `null` | `85000.00` / `90000.00`; `76500.00`; identity; IAE | `chainAppliesInOrder`, `chainThreeRules`, `emptyChainIsIdentity`, `chainRejectsNull` |
| `calculateRevenue(products, rule)` | identity; 10%; −200000 trên [áo, Office] | bằng bản S03 (`7138000.00`); `1035000.00`; `800000.00` (trừ từng product, không trừ trên tổng) | `identityMatchesOldOverload`, `appliesRulePerProduct` |
| `calculateRevenue(products, rule)` | lambda +10000; chain; list rỗng; `null` | `1170000.00`; `1055000.00`; `0.00`; IAE | `acceptsLambdaAndChain`, `emptyListIsZero`, `rejectsNulls` |
| `revenueByCategory` | o1 (áo 2, Office 1, −10%, +8%), o2 (quần 1, áo 3), o3 (chuột 5) | FASHION `1200000.00`, SOFTWARE `1000000.00`, ELECTRONICS `1250000.00`; chỉ 3 key | `sumsLineTotalsPerCategory`, `onlyCategoriesWithSales` |
| `revenueByCategory` | product constructor cũ; không có order; `null` | key `OTHER`; map rỗng; IAE | `defaultCategoryIsOther`, `emptyAndNull` |
| `topSellingProducts` | tổng SL: áo 5, chuột 5, Office 1, quần 1; n = 2, 3, 10 | `[chuột, áo]`, `[chuột, áo, Office]`, cả 4 (bằng nhau thì id tăng dần, **D3**; n lớn hơn thì trả hết) | `sortsByQuantityThenId`, `nLargerThanProductCount` |
| `topSellingProducts` | quần ở instance khác cùng id P002 thêm 9 | cộng chung theo id → quần đứng đầu | `groupsByProductId` |
| `topSellingProducts` | không có order; n 0 / −1; `null` | list rỗng; IAE; IAE | `emptyOrders`, `rejectsNonPositiveN`, `rejectsNullOrders` |
| `partitionByFinalAmount` | ngưỡng 1000000 (o1 1263600, o2 900000, o3 1250000) | true `[o1, o3]`, false `[o2]`, giữ thứ tự đơn | `partitionsByThreshold` |
| `partitionByFinalAmount` | ngưỡng `900000` với o2 `900000.00`; không có order; `null` | nhóm true (≥, so bằng `compareTo`, **D5**); đủ 2 key, list rỗng; IAE | `equalGoesToTrue`, `emptyOrdersHaveBothKeys`, `rejectsNulls` |
| Pure | gọi cả 3 báo cáo | list order và các order giữ nguyên | `doesNotMutateInput` |

Ngoài spec: list trả về của `search` / `topSellingProducts` có sửa được hay không, kiểu `Map` cụ thể (`EnumMap` hay `HashMap`), thứ tự key của `revenueByCategory`, hiệu năng.

**Kiểm khi review:** không còn `for`/`while` trong `InventoryManager`, `Order`, `PricingEngine`, `ReportService`; không `forEach` để add vào collection bên ngoài; không đặt logic trong `peek`; không `Optional.get()` trần (dùng `orElseThrow` / `map` / `orElse`); gộp `BigDecimal` bằng `reduce` / `reducing`, không `double`; `toMap` có merge function; `PricingRule.chain` dùng `reduce(identity(), PricingRule::andThen)`; constructor có category tái sử dụng validation cũ (constructor chaining, không chép lại kiểm tra); có `S05Demo` chạy được.

## S06 – Multi-threading & Concurrency: tồn kho an toàn, chuyển kho, gọi song song
**Nhánh:** `s06/concurrency-inventory` · bài tập cuối slide S06 (tự trình bày 24/10)

| # | Class/Task | Package | Ghi chú |
|---|---|---|---|
| 1 | `InventoryManager`: `addProduct`, `findById`, `search`, `decreaseStock(Order)` an toàn khi nhiều thread cùng gọi; all-or-nothing vẫn đúng khi chạy đồng thời | `domain/inventory` | refactor S05, giữ nguyên hành vi đơn luồng đã spec. Cách khóa (một lock chung, hay lock theo product + lock ordering) và việc có thêm `reserve(productId, quantity)` (slide gọi là `InventoryService.reserve()`) chốt ở spec |
| 2 | `Warehouse`: kho có id, tồn kho theo product id (đọc tồn, nhập, xuất) | `domain/inventory` | mới; id `String`, so thứ tự bằng `compareTo` |
| 3 | `WarehouseTransferService.transfer(from, to, productId, quantity)`: khóa cả 2 kho theo thứ tự id cố định | `domain/inventory` | mới; thiếu hàng ném `OutOfStockException` (S04). Cách xử lý `from == to` chốt ở spec |
| 4 | `OrderDetailService.loadDetail(orderId)`: gọi song song 3 nguồn (thanh toán, vận chuyển, các dòng đơn) qua `ExecutorService` được inject, gộp thành `OrderDetail` (record) | `application/{order, payment, shipping}` | mới; 3 nguồn là interface trong `application` (`order.OrderLineSource`, `payment.PaymentGateway`, `shipping.ShipmentTracker`), bản giả có độ trễ (`sleep`) để demo đặt ở `infrastructure`. Cách xử lý khi một nguồn lỗi hoặc bị interrupt chốt ở spec |
| 5 | `S06Demo` | `demo` | 100 thread cùng mua sản phẩm cuối; chuyển kho hai chiều song song; so thời gian `loadDetail` tuần tự / `newFixedThreadPool` / `newVirtualThreadPerTaskExecutor`. Bản nháp đang ở `git stash` (`S06Demo concurrency demo`) |

> Đã merge (tag `s06-done`).

**Acceptance**
- [x] 100 thread cùng mua sản phẩm còn 1: đúng 1 lần thành công, các lần còn lại ném `OutOfStockException`, stock cuối = 0, không bao giờ âm
- [x] Nhiều đơn nhiều dòng chạy đồng thời vẫn all-or-nothing: không đơn nào bị trừ một phần, tổng đã trừ = tổng số lượng của các đơn thành công
- [x] Kiểm tra và trừ nằm trong cùng một lock (không check-then-act bên ngoài lock); lock ngắn, không gọi IO hay `sleep` khi đang giữ lock
- [x] `transfer` hai chiều (A → B và B → A) chạy song song nhiều lần không treo (test có timeout); tổng tồn kho hai kho không đổi
- [x] `OrderDetailService`: 3 nguồn chạy song song, tổng thời gian ≈ nguồn chậm nhất; service không tự `new Thread()` và không tự tạo pool; bên nào tạo `ExecutorService` thì bên đó đóng (try-with-resources / `shutdown`)
- [x] Không nuốt `InterruptedException` (bật lại cờ bằng `Thread.currentThread().interrupt()` hoặc ném tiếp); không dùng `volatile` cho thao tác đọc-sửa-ghi
- [x] Module `domain/` vẫn là Java thuần; test S02–S05 vẫn xanh

Ngoài phạm vi: transaction / optimistic lock ở DB (để buổi JDBC), structured concurrency (preview), jcstress.

**Spec hành vi** (chốt 28/10, test: `domain/src/test/java/com/training/ecommerce/domain/inventory/{InventoryManagerConcurrencyTest, WarehouseTest, WarehouseTransferServiceTest}.java` (+ helper `Concurrently`), `app/src/test/java/com/training/ecommerce/application/order/OrderDetailServiceTest.java`; test S02–S05 giữ nguyên)

API (quyết định 28/10):
- **D1** Không thêm `reserve`: flash sale dùng luôn `InventoryManager.decreaseStock(Order)`. Mọi method public của `InventoryManager` an toàn khi nhiều thread cùng gọi; cách khóa do Tùng chọn (test chỉ kiểm hành vi).
- `Warehouse(String id)` ở `domain/inventory`: `getId()`, `stockOf(productId)` (chưa có → 0), `add(productId, quantity)`, `remove(productId, quantity)` (thiếu → `OutOfStockException`). Tự thân an toàn khi nhiều thread cùng gọi.
- `WarehouseTransferService.transfer(from, to, productId, quantity)` ở `domain/inventory`. **D2** `from == to` hoặc hai kho trùng id → IAE.
- `application` (tách package 28/10): `payment.PaymentGateway.findPayment` + record `payment.PaymentInfo(status, amount)`; `shipping.ShipmentTracker.track` + record `shipping.ShipmentInfo(carrier, status)`; `order.OrderLineSource.findLines` (functional interface); record `order.OrderDetail(orderId, payment, shipment, lines)` (field null → IAE, `lines` là bản copy không sửa được); `OrderDetailService(payments, shipments, lines, executor)` + `loadDetail(orderId)`.
- **D3** Một nguồn lỗi → `OrderDetailLoadException` (unchecked, mới, ở `application.order`), `getCause()` là exception gốc. Thread gọi bị interrupt → cũng `OrderDetailLoadException`, cờ interrupt vẫn bật.
- **D4** Nguồn lỗi thì cancel các task còn lại: test `@Tag("bonus")`.
- Tham số `null` / quantity ≤ 0 / id rỗng → IAE ở mọi method mới.

| Method | Input | Kết quả mong đợi | Test |
|---|---|---|---|
| `decreaseStock` | 100 thread cùng mua áo còn 1 (×20 lần) | đúng 1 thành công, 99 `OutOfStockException`, stock 0 | `lastItem_exactlyOneBuyerWins` |
| `decreaseStock` | kho 10.000 áo, 100 thread × 100 đơn 1 áo (×10) | mọi đơn thành công, stock đúng 0 (không mất lần trừ) | `manyOrders_noLostUpdate` |
| `decreaseStock` | áo 30, quần 30; 30 đơn (2 áo + 1 quần), 30 đơn (2 quần) song song (×20) | tồn khớp số đơn thành công, không âm, không đơn nào trừ một phần | `multiLineOrders_allOrNothingUnderConcurrency` |
| `addProduct` | 100 thread × 100 id khác nhau; 50 thread cùng 1 id (×10) | đủ 100, tìm được từng cái; đúng 1 thành công, 49 IAE | `addDistinctIds_noneLost`, `addSameId_exactlyOneWins` |
| `search` | 4 thread `search` trong lúc 1 thread thêm 3000 product | không exception, cuối cùng đủ 3000 | `searchWhileAdding_noException` |
| `Warehouse` | id `null` / `""` / `"   "` | IAE | `rejectsBlankId` |
| `stockOf` / `add` | chưa có; add 30 + 20, quần 5; quantity 0 / −1 / −100; productId `null` | 0; 50, 5; IAE, tồn giữ nguyên; IAE | `unknownProductHasZeroStock`, `addAccumulatesPerProduct`, `addRejectsNonPositiveQuantity`, `rejectsNullProductId` |
| `remove` | 50 − 20, rồi − 30; − 51; product chưa có; quantity 0 / −5; `null` | 30, 0; `OutOfStockException` (productId, requested 51, available 50), tồn giữ nguyên; `OutOfStockException` available 0; IAE; IAE | `removesQuantity`, `removeMoreThanStock`, `removeUnknownProduct`, `rejectsNonPositiveQuantity`, `rejectsNullProductId` |
| `Warehouse` song song | 100 thread add 10 (×10); tồn 50, 100 thread remove 1 (×10) | 1000; đúng 50 thành công, tồn 0 | `concurrentAdds`, `concurrentRemoves` |
| `transfer` | HN 100 → HCM 30; HCM đã có 5 | HN 70, HCM 30; HCM 35 | `movesStock`, `addsToExistingStock` |
| `transfer` | HN 10 chuyển 11; HN chưa có product | `OutOfStockException`, hai kho giữ nguyên | `notEnoughStock`, `unknownProductInSource` |
| `transfer` | `null` (from / to / productId); quantity 0 / −5; `from == to`; hai kho cùng id `HN` | IAE, tồn giữ nguyên (**D2**) | `rejectsNulls`, `rejectsNonPositiveQuantity`, `rejectsSameWarehouse`, `rejectsSameId` |
| `transfer` song song | HN→HCM và HCM→HN mỗi chiều 10.000 lần (×5) | xong trong 10 giây (không deadlock), tổng 2000 | `opposingTransfers_noDeadlock` |
| `transfer` song song | transfer + `add` HN + `remove` HCM trực tiếp, mỗi luồng 5000 lần (×5) | tổng = 2000 + 5000 − số lần remove thành công | `transferWithDirectAddRemove` |
| Records | giá trị hợp lệ; field `null`; sửa list gốc / list của `OrderDetail` | giữ đúng; IAE; không ảnh hưởng / `UnsupportedOperationException` | `keepValues`, `rejectNulls`, `orderDetailCopiesLines` |
| `loadDetail` | 3 nguồn trả dữ liệu | gộp đủ 3 phần, mỗi nguồn nhận đúng orderId | `combinesThreeSources` |
| `loadDetail` | 3 nguồn chờ nhau bằng `CountDownLatch(3)`, với `newFixedThreadPool(3)` và `newVirtualThreadPerTaskExecutor()` | chạy xong (gọi tuần tự → nguồn ném "không chạy song song") | `callsSourcesInParallel` |
| `loadDetail` | gọi 2 lần trên cùng executor | executor không bị shutdown, lần 2 vẫn chạy | `doesNotShutDownInjectedExecutor` |
| `loadDetail` | constructor nhận `null`; orderId `null` / `""` / `"   "` | IAE; IAE và không gọi nguồn nào | `constructorRejectsNulls`, `rejectsBlankOrderId` |
| `loadDetail` | payment / shipment / lines ném `IllegalStateException` | `OrderDetailLoadException`, `getCause()` là đúng exception đó (**D3**) | `sourceFailureIsWrapped` |
| `loadDetail` | thread gọi bị interrupt khi 3 nguồn đang chờ | ném `OrderDetailLoadException` trong 5 giây, cờ interrupt vẫn bật | `interruptIsNotSwallowed` |
| `loadDetail` (bonus) | payment lỗi ngay, 2 nguồn kia chậm 5 giây | ném trong < 2 giây, 2 task còn lại bị cancel (không chạy hết) (**D4**) | `failureCancelsRemainingTasks` |

Ngoài spec: đo thời gian chạy (để `S06Demo` so sánh), fairness, hiệu năng của từng cách khóa, nguồn trả `null`, `OrderDetail.lines` chứa phần tử `null`, structured concurrency. Test concurrency xanh không chứng minh hết race; đã kiểm rằng bản `InventoryManager` chưa khóa đỏ ổn định (lost update 10/10, search khi add), bản khóa from → to bị bắt deadlock, gọi tuần tự / nuốt interrupt / shutdown executor đều bị bắt.

**Kiểm khi review:** check và trừ nằm trong cùng một lock (không check-then-act ngoài lock); không gọi IO / `sleep` khi giữ lock; transfer khóa theo thứ tự id cố định và khóa đúng object mà `Warehouse` dùng (hoặc lý do nếu khác); `InventoryManager` không bọc riêng từng method mà để hở bước giữa (ví dụ khóa `findById` nhưng `decreaseStock` thì không); service không tự `new Thread()` hay tạo pool; không `volatile` cho đọc-sửa-ghi; `unlock()` trong `finally` nếu dùng `ReentrantLock`; catch `InterruptedException` thì bật lại cờ; `S06Demo` chạy được và so thời gian tuần tự / fixed pool / virtual thread; `domain/` vẫn Java thuần.

---

## S07 – JDBC, PostgreSQL & Docker: lưu Product/Order, REST API, profile
**Nhánh:** `s07/jdbc-docker` · làm trọn (không theo luồng spec đỏ), chốt 28/10

| # | Class/Task | Package | Ghi chú |
|---|---|---|---|
| 1 | `Order` có id (`Identifiable<String>`), `OrderLine` có constructor khôi phục giá snapshot | `domain/order` | constructor cũ giữ nguyên, sinh id bằng UUID |
| 2 | Port `StockStore.decreaseStock(Order)` (all-or-nothing), `ProductService`, `OrderService.placeOrder` (`@Transactional`) | `application/{inventory, catalog, order}` | mới |
| 3 | `JdbcProductRepository`, `JdbcOrderRepository`, `JdbcStockStore` bằng `JdbcClient`; `schema.sql` + `data.sql` | `infrastructure/persistence/jdbc` | product lưu một bảng + cột `type`; bundle có bảng `bundle_items`. Trừ kho bằng `UPDATE … WHERE stock >= ?`, không đọc-sửa-ghi |
| 4 | `InMemoryStockStore` + config theo profile | `infrastructure/{persistence/inmemory, config}` | `local`: `InMemoryRepository`, không cần DB; `dev`: PostgreSQL |
| 5 | REST `/api/products`, `/api/orders` + `RestExceptionHandler` + OpenAPI | `interfaces/rest` | request/response là record; 404 / 409 / 400 bằng `ProblemDetail` |
| 6 | `Dockerfile` + `compose.yaml` (PostgreSQL + backend) | root | profile `dev` chạy hoàn toàn trên Docker |

> Đã merge (tag `s07-done`).

**Acceptance**
- [x] Profile `local` (mặc định): BE chạy trên IDE, không cần DB, có sẵn dữ liệu mẫu
- [ ] Profile `dev`: `docker compose up --build` chạy cả DB lẫn BE; dữ liệu còn sau `docker compose down` / `up` _(chưa kiểm lại 13/11: cổng 8080 đang bận)_
- [x] Đặt đơn nhiều dòng là all-or-nothing trong DB: một dòng thiếu hàng thì không dòng nào bị trừ (rollback)
- [x] Nhiều request cùng mua sản phẩm cuối: đúng một đơn thành công, stock không âm
- [x] Đơn đọc lại từ DB giữ giá snapshot lúc đặt, kể cả khi giá sản phẩm đổi sau đó
- [x] Swagger UI ở `/swagger-ui.html` có đủ API product và order
- [x] Module `domain/` vẫn Java thuần; test S02–S06 vẫn xanh

Ngoài phạm vi: JPA / Spring Data, Flyway, optimistic lock bằng cột version, phân trang, auth.

**Kiểm khi review:** SQL dùng tham số (không nối chuỗi); trừ kho là một câu `UPDATE` có điều kiện; transaction bao cả trừ kho lẫn lưu đơn; repository không trả domain object ra REST trực tiếp; secret DB lấy từ biến môi trường; test JDBC chạy trên PostgreSQL thật (Testcontainers, `@Tag("docker")`).

---

## Extra – Nối Warehouse ↔ InventoryManager (hub): nhập hàng từ kho lên hub
**Nhánh:** `extra/warehouse-hub` · đề xuất 30/10, theo luồng spec đỏ

Hiện có hai nguồn tồn kho rời nhau: `Product.stock` trong `InventoryManager` (bị `decreaseStock(Order)` trừ khi bán) và `Map<productId, qty>` của `Warehouse` (chỉ `WarehouseTransferService` đụng tới). Dữ liệu mẫu visualizer vì vậy lệch nhau (LAPTOP hub 5, HN 10 + HCM 2), đường `WH → hub` đang là nét chấm "chưa có" (D6 trong `docs/extras/s07-domain-visualizer.md`).

Mô hình: **hub = hàng sẵn bán, kho = hàng dự trữ**, nối bằng luồng nhập hàng kho → hub. Không đổi nghĩa `Product.stock` nên test S02–S06 giữ nguyên. Đã loại: bỏ `Product.stock` để tồn = tổng các kho (phá API/test từ S02, `decreaseStock` phải tự chọn kho); đơn chọn kho để trừ (fulfillment, phạm vi lớn, để buổi sau).

| # | Class/Task | Package | Ghi chú |
|---|---|---|---|
| 1 | `Product.increaseStock(int quantity)` | `domain/catalog` | mới; `quantity ≤ 0` → IAE |
| 2 | `InventoryManager.replenish(Warehouse from, String productId, int quantity)` | `domain/inventory` | mới; kho xuất, hub cộng đúng số đó, all-or-nothing |
| 3 | Visualizer: kịch bản `replenish`, đường `WH → hub` nét liền, sửa dữ liệu mẫu `WorldFactory` | `visualizer` | sau khi domain xanh, chạy skill `visualizer-sync` |

> Đã merge vào `main`.

**Acceptance**
- [x] `replenish` thành công: kho giảm và hub tăng đúng `quantity`; lỗi thì cả kho lẫn hub giữ nguyên
- [x] Không ai thấy trạng thái "kho đã trừ, hub chưa cộng"
- [x] `replenish`, `transfer` hai chiều và `decreaseStock` chạy song song không treo (test có timeout); tổng hub + các kho + đã bán không đổi
- [x] Thứ tự khóa toàn cục ghi trong Javadoc `InventoryManager` và `Warehouse`
- [x] `domain/` vẫn Java thuần; test S02–S07 vẫn xanh

**Spec hành vi** (đề xuất 30/10, test: `domain/src/test/java/com/training/ecommerce/domain/inventory/InventoryManagerReplenishTest.java`, test `increaseStock` thêm vào test sẵn có của `Product`)

API (quyết định đề xuất 30/10):
- **D1** `replenish` đặt trên `InventoryManager`, không tách service: lock của hub là `this`, service riêng sẽ phải `synchronized(inventoryManager)` từ bên ngoài (lộ chi tiết khóa).
- **D2** Thứ tự khóa toàn cục: **hub trước, kho sau**. Không code nào giữ lock kho rồi gọi vào hub. `WarehouseTransferService` chỉ khóa kho ↔ kho (id nhỏ trước) nên không tạo vòng chờ.
- **D3** Giữ cả hai lock trong suốt lần nhập (không nhả giữa hai bước).
- **D4** Product chưa có trên hub → `ProductNotFoundException`, kiểm trước khi đụng tới kho; không tự tạo product từ kho (kho chỉ biết productId, không biết tên/giá).
- Tham số `null` / quantity ≤ 0 → IAE, cả hai bên giữ nguyên.

| Method | Input | Kết quả mong đợi | Test |
|---|---|---|---|
| `increaseStock` | stock 5, cộng 3; quantity 0 / −1 | 8; IAE, stock giữ nguyên | `increaseAddsStock`, `increaseRejectsNonPositive` |
| `replenish` | HN LAPTOP 10, hub LAPTOP 5, nhập 4 | HN 6, hub 9 | `movesStockFromWarehouseToHub` |
| `replenish` | HN có 3, nhập 4; HN chưa có product | `OutOfStockException`, hai bên giữ nguyên | `notEnoughInWarehouse`, `unknownProductInWarehouse` |
| `replenish` | productId không có trên hub | `ProductNotFoundException`, kho giữ nguyên (**D4**) | `productNotInHub` |
| `replenish` | `null` (from / productId); quantity 0 / −5 | IAE, hai bên giữ nguyên | `rejectsNulls`, `rejectsNonPositiveQuantity` |
| song song | `replenish` HN → hub + `transfer` HN ↔ HCM hai chiều + `decreaseStock` đơn 1 cái, mỗi luồng 5000 lần (×5) | xong trong 10 giây (không deadlock); không thread nào ném exception khác `OutOfStockException`; hub + HN + HCM + số đã bán = tổng ban đầu | `replenishTransferSell_conserveTotal` |
| song song | hub còn 0; 1 thread `replenish` 1 cái, 100 thread mua (×20) | replenish thành công, HN còn 0; số lần mua thành công + stock hub = 1 (tối đa 1 người mua được, không âm) | `replenishWhileBuyingLastItem` |

Ngoài spec: trả hàng hub → kho, tự động nhập khi hub sắp hết, chọn kho cho từng đơn, lưu xuống DB (`StockStore` ở `app` chưa đụng), tràn `int`.

**Kiểm khi review:** thứ tự khóa đúng **D2** ở mọi chỗ; kiểm tra (product có trên hub, kho đủ hàng) và cập nhật nằm trong cùng lock; `Product.increaseStock` chỉ được gọi khi đang giữ lock hub; không gọi IO / `sleep` khi giữ lock; `domain/` vẫn Java thuần; đã chạy `visualizer-sync`, banner lệch API không còn.

---

## Extra – Chốt giỏ bằng mã giảm giá: `Cart.toOrder(DiscountCode, taxRate)`
**Nhánh:** `extra/warehouse-hub` · đề xuất 30/10, theo luồng spec đỏ

Hiện `DiscountCode` (`domain.pricing`) chỉ dùng được qua `Discountable.applyDiscount` để giảm giá từng sản phẩm; giảm giá cả đơn thì `Cart.toOrder(discountRate, taxRate)` nhận một tỉ lệ trần, không biết tới mã. Visualizer (kịch bản `cart-to-order`) muốn có bước "nhập mã" rồi chốt đơn bằng mã đó.

| # | Class/Task | Package | Ghi chú |
|---|---|---|---|
| 1 | `Cart.toOrder(DiscountCode code, BigDecimal taxRate)` | `domain/order` | overload mới; `order` phụ thuộc `pricing` (không vòng: `pricing` không import `order`) |
| 2 | Visualizer: bước `new DiscountCode("SALE10", 10%)` trong `cart-to-order`, chốt đơn bằng mã, wire `pricing → orders` | `visualizer` | sau khi domain xanh, chạy skill `visualizer-sync` |

> Đã merge vào `main` (cùng nhánh `extra/warehouse-hub`).

**Spec hành vi** (đề xuất 30/10, test: nhóm `ToOrderWithDiscountCode` trong `domain/src/test/java/com/training/ecommerce/domain/order/CartTest.java`)

- **D1** Tương đương `toOrder(code.getRate(), taxRate)`: `Order` chỉ giữ `discountRate`, không giữ mã (mã là chuyện của lúc chốt, đơn đã lưu không cần biết).
- **D2** `code` null → IAE (giống `Discountable.applyDiscount`), kiểm trước khi kiểm giỏ rỗng; không coi null là "không giảm giá" — muốn không giảm thì gọi `toOrder(BigDecimal.ZERO, taxRate)`.
- Giỏ rỗng → `IllegalStateException`; taxRate không hợp lệ → IAE (theo quy tắc của `Order`), như overload cũ.

| Method | Input | Kết quả mong đợi | Test |
|---|---|---|---|
| `toOrder(code, tax)` | áo 1, quần 1, áo 1; mã SALE10 (0.10), tax 0.08 | như `toOrder(0.10, 0.08)`: 2 dòng theo thứ tự thêm, `discountRate` 0.10, final `729000.00`; không trừ kho | `appliesCodeRate`, `doesNotDecreaseStockWithCode` |
| `toOrder(code, tax)` | mã giảm 100% (rate 1) | final `0.00` | `fullDiscountCode` |
| `toOrder(code, tax)` | code `null` (giỏ có hàng và giỏ rỗng); giỏ rỗng với mã hợp lệ; tax −0.01 | IAE (**D2**); `IllegalStateException`; IAE | `rejectsNullCode`, `nullCodeCheckedBeforeEmptyCart`, `rejectsEmptyCartWithCode`, `rejectsInvalidTaxWithCode` |

Ngoài spec: mã hết hạn / giới hạn số lần dùng, cộng dồn nhiều mã, mã chỉ áp cho một số sản phẩm, lưu mã vào `Order` hay DB.

**Kiểm khi review:** overload mới gọi lại overload cũ (không lặp logic tạo dòng); `domain/` vẫn Java thuần; đã chạy `visualizer-sync`.

---

## Extra – Vận đơn & giao hàng: tạo vận đơn, giao thành công / thất bại / hoàn
**Nhánh:** `extra/shipment-lifecycle` · đề xuất 30/10, theo luồng spec đỏ

Hiện luồng mới dừng ở chốt đơn: `Order` không có trạng thái, địa chỉ hay phí ship. `Shippable.calculateShipping(Address)` chỉ tính phí cho **từng sản phẩm** (mỗi sản phẩm cộng một lần `baseFee`), chưa ai gọi trong luồng đặt đơn. `ShipmentTracker`/`ShipmentInfo` ở `application/shipping` chỉ là nguồn giả S06 để gọi song song. Kho chỉ trừ (`decreaseStock(Order)`), chưa hoàn khi huỷ/trả.

Mô hình: **vận đơn là aggregate riêng `Shipment` trong `domain.shipping`**, tham chiếu đơn qua `orderId`, giữ địa chỉ, phí ship và trạng thái giao. `Order` giữ nguyên (không đổi bảng `orders`, test S02–S07 không đụng). `shipping` được import `order`/`catalog`/`customer`; `order` không import `shipping` (không vòng). Đã loại: thêm `OrderStatus` + địa chỉ vào `Order` (đổi constructor, JDBC, test cũ; để khi có thanh toán/hoàn tiền); `Shipment` giữ cả object `Order` (khó lưu, lẫn ranh giới aggregate).

```
CREATED ──pickUp()──▶ IN_TRANSIT ──markDelivered()──▶ DELIVERED
   │                     │  ▲
 cancel()   failAttempt(lý do): attempts+1, vẫn IN_TRANSIT
   ▼                     │  (lần thứ 3 tự chuyển RETURNING)
CANCELLED                ├──refuse(lý do)──▶ RETURNING ──markReturned()──▶ RETURNED
```

| # | Class/Task | Package | Ghi chú |
|---|---|---|---|
| 1 | `ShipmentStatus` (enum) | `domain/shipping` | `CREATED`, `IN_TRANSIT`, `DELIVERED`, `RETURNING`, `RETURNED`, `CANCELLED`; `isTerminal()` |
| 2 | `ShipmentEvent` (record `status`, `note`) | `domain/shipping` | mỗi lần chuyển trạng thái / giao thất bại thêm một event |
| 3 | `Shipment implements Identifiable<String>` | `domain/shipping` | `orderId`, `Address`, `fee`, `trackingCode`, `status`, `failedAttempts`, `history()` (bản copy không sửa được); method chuyển trạng thái như sơ đồ |
| 4 | `ShippingFeeCalculator.feeFor(Order, Address)` | `domain/shipping` | `ShippingZone.of(address).feeFor(tổng cân nặng)`: cộng `weightKg × quantity` các dòng `PhysicalProduct`, bundle tính item physical bên trong; `baseFee` tính một lần |
| 5 | `InventoryManager.restock(Order)` | `domain/inventory` | đối xứng `decreaseStock(Order)`, `synchronized`, dùng `Product.increaseStock` |
| 6 | Port `StockStore.increaseStock(Order)` + `ShipmentRepository` (`findByOrderId`) | `application/{inventory, shipping}` | service tự lọc vận đơn còn hiệu lực |
| 7 | `ShipmentService`: `create(orderId, address)`, `pickUp`, `deliver`, `failAttempt`, `refuse`, `markReturned`, `cancel` | `application/shipping` | `markReturned`/`cancel` là `@Transactional`: đổi trạng thái + hoàn kho cùng commit/rollback |
| 8 | JDBC + in-memory (`shipments`, `shipment_events`), REST `/api/shipments` | `infrastructure`, `interfaces/rest/shipping` | `IllegalShipmentTransitionException` (domain) / `ActiveShipmentExistsException` (application) → 409, IAE → 400, không tìm thấy → 404; ISE chung (dữ liệu hỏng) vẫn 500 |
| 9 | Visualizer: kịch bản giao thành công và giao thất bại 3 lần → hoàn kho | `visualizer` | sau khi domain xanh, chạy skill `visualizer-sync` |

> Đã merge vào `main`.

**Spec hành vi** (đề xuất 30/10, test: `ShipmentTest`, `ShippingFeeCalculatorTest` trong `domain/src/test/java/com/training/ecommerce/domain/shipping/`, `InventoryManagerRestockTest` trong `domain/src/test/java/com/training/ecommerce/domain/inventory/`, `ShipmentServiceTest` ở `app`)

- **G1** Vận đơn mới: `CREATED`, `failedAttempts = 0`, `trackingCode` không rỗng, `fee` scale 2, history có đúng một event `CREATED`.
- **G2** Phí theo cả đơn, `baseFee` một lần: không cộng `calculateShipping` từng dòng.
- **G3** Đơn không có dòng hàng vật lý nào (chỉ digital/subscription) → IAE khi tính phí / tạo vận đơn; `order`, `address`, `orderId` null/rỗng → IAE.
- **G4** `failAttempt(lý do)` chỉ ở `IN_TRANSIT`: `failedAttempts + 1`, giữ `IN_TRANSIT`; lần thứ 3 (`MAX_ATTEMPTS = 3`) tự chuyển `RETURNING`. Lý do null/rỗng → IAE (kiểm trước trạng thái).
- **G5** `cancel()` chỉ ở `CREATED`; đã lấy hàng thì phải `refuse` → `markReturned`.
- **G6** Chuyển trạng thái không có trong sơ đồ → `IllegalShipmentTransitionException` (kế thừa `IllegalStateException`, như `OutOfStockException`; nay kế thừa `BusinessException`, xem Extra – Gom lỗi nghiệp vụ), message nêu trạng thái hiện tại; trạng thái kết thúc (`DELIVERED`, `RETURNED`, `CANCELLED`) không chuyển đi đâu được. Ném lỗi thì trạng thái và history không đổi.
- **G7** `restock(Order)` cộng lại đúng số lượng theo product của đơn; product không có trong hub → `ProductNotFoundException`, không product nào được cộng (all-or-nothing như `decreaseStock`). Ở `ShipmentService`: `RETURNED` và `CANCELLED` hoàn kho, `DELIVERED` không.
- **G8** Mỗi đơn có tối đa một vận đơn "còn hiệu lực" (khác `CANCELLED`/`RETURNED`): tạo thêm → ISE (REST 409). `DELIVERED` cũng chặn (đơn đã giao xong không giao lại); vận đơn trước đã `CANCELLED`/`RETURNED` thì được tạo mới. DB có partial unique index cùng điều kiện làm lớp phòng thủ cuối.

| Method | Input | Kết quả mong đợi | Test |
|---|---|---|---|
| `new Shipment(orderId, address, fee)` | `"o-1"`, Hà Nội, `30000` | `CREATED`, attempts 0, history 1 event (**G1**) | `startsCreated` |
| `ShippingFeeCalculator.feeFor` | áo 0.5 kg × 2 + quần 1 kg; Hà Nội | `30000.00` (20000 + 5000 × 2) (**G2**) | `sumsWeightOnceBaseFee` |
| `ShippingFeeCalculator.feeFor` | đơn trên, giao Đà Nẵng | `45000.00` | `otherCityFee` |
| `ShippingFeeCalculator.feeFor` | bundle gồm áo 0.5 kg + ebook, × 1; Hà Nội | `22500.00` | `countsPhysicalItemsInBundle` |
| `ShippingFeeCalculator.feeFor` | chỉ ebook; `order`/`address` null | IAE (**G3**) | `rejectsOrderWithoutPhysicalLines`, `rejectsNullArgs` |
| `pickUp` → `markDelivered` | vận đơn mới | `DELIVERED`, history 3 event | `deliversSuccessfully` |
| `failAttempt` × 2 | đang `IN_TRANSIT` | `IN_TRANSIT`, attempts 2 (**G4**) | `staysInTransitBelowMax` |
| `failAttempt` × 3 | đang `IN_TRANSIT` | `RETURNING`, attempts 3 | `autoReturnsAfterThirdFailure` |
| `failAttempt("")` | đang `IN_TRANSIT`; ở `CREATED` | IAE; ISE | `rejectsBlankReason`, `failAttemptRequiresInTransit` |
| `refuse(lý do)` → `markReturned` | đang `IN_TRANSIT` | `RETURNING` → `RETURNED` | `returnsAfterRefusal` |
| `cancel` | ở `CREATED`; ở `IN_TRANSIT` | `CANCELLED`; ISE (**G5**) | `cancelsBeforePickUp`, `cannotCancelAfterPickUp` |
| `markDelivered`, `pickUp`, `cancel` | ở `DELIVERED` / `CANCELLED` / `RETURNED` | ISE, trạng thái + history không đổi (**G6**) | `terminalStatesAreFinal` (parameterized) |
| `restock(order)` | áo 1 + quần 1 + áo 1, hub áo 3 quần 5 | áo 5, quần 6 (**G7**) | `restocksOrderQuantities` |
| `restock(order)` | một dòng có product không có trong hub | `ProductNotFoundException`, stock không đổi | `restockAllOrNothing` |
| `ShipmentService.markReturned` / `cancel` / `deliver` | đơn áo × 2 | stock +2 / +2 / không đổi (**G7**) | `returnRestocks`, `cancelRestocks`, `deliverDoesNotRestock` |
| `ShipmentService.create` | đơn đã có vận đơn `IN_TRANSIT` / `DELIVERED`; vận đơn cũ `CANCELLED` / `RETURNED` | ISE; tạo được (**G8**) | `rejectsSecondActiveShipment`, `rejectsAfterDelivered`, `allowsNewAfterCancelled`, `allowsNewAfterReturned` |

Ngoài spec: trả hàng **sau khi đã giao** (`DELIVERED → RETURNING`, luồng đổi trả riêng), hoàn tiền (`PaymentGateway` hiện chỉ đọc), timestamp / `Clock` cho event, nhiều kiện cho một đơn, giao từng phần, phí ship cộng vào `finalAmount` của `Order`, tích hợp API hãng vận chuyển thật (nối `ShipmentTracker` vào `ShipmentRepository` để sau).

**Thứ tự làm:** domain #1–5 (G1–G7 phần domain, Java thuần, test đỏ trước) → application + persistence + REST #6–8 (G7 phần service, G8) → visualizer #9.

**Kiểm khi review:** chuyển trạng thái nằm trong `Shipment` (service không tự set status); lỗi chuyển trạng thái không để lại thay đổi nửa vời; `restock` là all-or-nothing và an toàn đa luồng như `decreaseStock`; hoàn kho cùng transaction với đổi trạng thái; `domain/` vẫn Java thuần; đã chạy `visualizer-sync`.

---

## Extra – Validate request REST bằng Bean Validation + constraint tự viết
**Nhánh:** `extra/request-validation` · đề xuất 11/11, theo luồng spec đỏ · checklist: **Annotation**

Hiện request REST được kiểm bằng tay, rải rác ở ba nơi: `ProductMapper.toDomain` (thiếu `type`, `months`, `itemIds`), constructor của domain (`weightKg > 0`, giá ≥ 0…) và `OrderService`. Lỗi trả về 400 nhưng mỗi lần chỉ báo **một** lỗi, dạng chuỗi tự do, không nói field nào sai. `app` chưa có `spring-boot-starter-validation`.

Mô hình: kiểm **hình dạng request** ở tầng `interfaces` bằng Bean Validation (annotation có sẵn + 2 constraint tự viết); domain vẫn tự bảo vệ bất biến của nó (lớp phòng thủ thứ hai, không bỏ). Logic "spec → Product" chuyển từ `ProductMapper` xuống `application/catalog` để task import CSV dùng lại. Đã loại: tự viết bộ xử lý annotation bằng reflection (phát minh lại Hibernate Validator; để làm bài thử trong `playground/`); validate trong service (service không biết request JSON).

| # | Class/Task | Package | Ghi chú |
|---|---|---|---|
| 1 | Thêm `spring-boot-starter-validation` | `app/pom.xml` | không đụng `domain/pom.xml` (enforcer chặn) |
| 2 | `@EnumValue(enumClass, ignoreCase = true)` + `EnumValueValidator` | `application/validation` | constraint tự viết có **thuộc tính**; null/rỗng coi là hợp lệ (để `@NotBlank` lo) |
| 3 | Interface `ProductSpec` (`type()`, `basePrice()`, `weightKg()`, `months()`, `discountRate()`, `itemIds()`) | `application/catalog` | request REST và dòng CSV cùng implement |
| 4 | `@ValidProductSpec` (mức class) + `ProductSpecValidator implements ConstraintValidator<ValidProductSpec, ProductSpec>` | `application/catalog` | kiểm field bắt buộc theo `type`; lỗi gắn vào đúng field bằng `addPropertyNode(...)` |
| 5 | `ProductFactory.create(ProductSpec, id, itemLookup)` | `application/catalog` | chuyển từ `ProductMapper.toDomain`; `ProductMapper` gọi lại |
| 6 | Annotation lên `ProductRequest`, `PlaceOrderRequest`, `PlaceOrderRequest.Line`, `CreateShipmentRequest`, `ReasonRequest`; `@Valid` ở controller | `interfaces/rest/**` | xem bảng spec |
| 7 | `RestExceptionHandler`: `MethodArgumentNotValidException` → 400 `ProblemDetail` có property `errors: [{field, message}]` | `interfaces/rest` | message tiếng Việt; lỗi mức class mà không gắn field thì `field` = tên object |

> Code xong 13/11 trên nhánh `extra/request-validation`, chưa merge vào `main`.

**Acceptance**
- [x] Request sai nhiều chỗ trả **một** response 400 liệt kê **tất cả** field sai
- [x] Controller không còn tự kiểm field bắt buộc; `ProductMapper` chỉ còn mapping
- [x] Domain vẫn ném IAE khi bị gọi trực tiếp với dữ liệu sai (không bỏ kiểm tra ở domain)
- [x] Swagger hiển thị ràng buộc (`minimum`, `maximum`, `required`) cho field dùng annotation có sẵn
- [x] `domain/` vẫn Java thuần; test S02–S07 và Extra vẫn xanh (test cũ kiểm message lỗi được sửa theo format mới, không xoá)

**Spec hành vi** (đề xuất 11/11, test: `ProductSpecValidatorTest`, `EnumValueValidatorTest` trong `app/src/test/java/com/training/ecommerce/application/{catalog,validation}/` — test bằng `Validation.buildDefaultValidatorFactory()`, không cần Spring; `ProductControllerTest`, `OrderControllerTest` thêm case 400)

- **V1** Field chung của `ProductRequest`: `type` `@NotNull`; `name` `@NotBlank` `@Size(max = 255)`; `stock` `@PositiveOrZero`; `category` `@EnumValue(Category.class)`; mỗi phần tử `itemIds` `@NotBlank`.
- **V2** `@ValidProductSpec` theo `type`: PHYSICAL cần `basePrice ≥ 0` và `weightKg > 0`; DIGITAL cần `basePrice ≥ 0`; SUBSCRIPTION cần `basePrice ≥ 0` và `months > 0`; BUNDLE cần `itemIds` không rỗng và `discountRate` trong [0, 1], **bỏ qua** `basePrice`. `type` null thì validator trả hợp lệ (đã có lỗi `@NotNull`, không báo trùng).
- **V3** Field không thuộc `type` (vd. PHYSICAL gửi kèm `months`) bị bỏ qua như hiện nay, không báo lỗi.
- **V4** `PlaceOrderRequest`: `lines` `@NotEmpty` và mỗi dòng `@Valid`; `Line.productId` `@NotBlank`, `Line.quantity` `@Positive`; `discountRate` `@DecimalMin("0") @DecimalMax("1")`; `taxRate` `@DecimalMin("0")` (null vẫn = 0 như hiện nay).
- **V5** Lỗi kiểu dữ liệu JSON (chữ vào field số, enum `type` sai) vẫn đi đường `HttpMessageNotReadableException` → 400 như cũ; ngoài phạm vi báo theo field.

| Method / Endpoint | Input | Kết quả mong đợi | Test |
|---|---|---|---|
| `ProductSpecValidator` | PHYSICAL thiếu `weightKg`; `weightKg` = 0 | 1 lỗi ở field `weightKg` (**V2**) | `physicalRequiresPositiveWeight` |
| `ProductSpecValidator` | SUBSCRIPTION thiếu `months` | 1 lỗi ở `months` | `subscriptionRequiresMonths` |
| `ProductSpecValidator` | BUNDLE `itemIds` rỗng, `discountRate` 1.5, `basePrice` null | 2 lỗi (`itemIds`, `discountRate`), không lỗi `basePrice` | `bundleRules` |
| `ProductSpecValidator` | `type` null | không lỗi từ validator này | `nullTypeLeftToNotNull` |
| `ProductSpecValidator` | PHYSICAL hợp lệ kèm `months` = 3 | hợp lệ (**V3**) | `ignoresFieldsOfOtherTypes` |
| `EnumValueValidator` | `"fashion"`, `"FASHION"`; `"food"`; null | hợp lệ; lỗi; hợp lệ | `acceptsAnyCase`, `rejectsUnknown`, `nullIsValid` |
| `POST /api/products` | `name` rỗng + `stock` −1 + PHYSICAL thiếu `weightKg` | 400, `errors` có đủ 3 field | `reportsAllFieldErrors` |
| `POST /api/orders` | `lines` rỗng; `quantity` 0 ở dòng 2; `discountRate` 1.2 | 400, field lần lượt `lines`, `lines[1].quantity`, `discountRate` (**V4**) | `rejectsInvalidOrder` (parameterized) |
| `POST /api/products` | `type` = `"FOOD"` | 400 "Body JSON không hợp lệ" (**V5**) | `unknownTypeIsUnreadableBody` |

Ngoài spec: validation group (create khác update), message i18n theo `Accept-Language`, validate `@PathVariable`/`@RequestParam` (`@Validated` trên controller), kiểm trùng id bằng constraint (cần DB — để service lo).

**Thứ tự làm:** #1 → validator #2–4 với test thuần (đỏ trước) → #5 refactor (test controller cũ phải xanh nguyên) → #6–7 + test controller.

**Kiểm khi review:** `@Retention(RUNTIME)` và `@Target` đúng chỗ dùng; validator không ném exception mà trả `false` + violation; lỗi mức class gắn vào field cụ thể, đã `disableDefaultConstraintViolation()`; không còn kiểm field trùng lặp ở `ProductMapper`; dependency rule giữ nguyên (`interfaces → application`, `application.catalog` không import `interfaces`).

---

## Extra – CSV: export order/product, import product (file vài trăm MB)
**Nhánh:** `extra/csv-import-export` · đề xuất 11/11, theo luồng spec đỏ · checklist: **I/O** · làm sau *Validate request REST* (dùng lại `ProductSpec`, `@ValidProductSpec`, `ProductFactory`)

Hiện không có đường nào đưa dữ liệu ra/vào ngoài REST từng bản ghi. `JdbcOrderRepository.findAll()` nạp toàn bộ đơn vào RAM và chạy thêm một câu SELECT lấy line cho **mỗi** đơn (N+1); `JdbcProductRepository.findAll()` tương tự với `bundle_items`. `save` của product là **upsert** (`ON CONFLICT DO UPDATE`), không dùng được cho import "chỉ thêm mới".

Mô hình: **export là read model** — port riêng đọc dạng stream thẳng từ DB ra hàng phẳng, không dựng `Order`/`Product` domain, không đi qua `findAll()`. **Import đọc từng dòng, ghi theo chunk**: không bao giờ giữ cả file trong bộ nhớ. CSV đọc/ghi tự viết bằng `java.io` (mục tiêu học I/O), đặt ở `shared` (Java thuần, test không cần Spring). Đã loại: Excel (`.xlsx` là zip XML, POI thường nạp cả file → OOM; giới hạn 1,048,576 dòng/sheet); thư viện CSV (Commons CSV / OpenCSV — mất phần học); import all-or-nothing một transaction (giữ lock và WAL rất lâu với vài triệu dòng, lỗi dòng cuối là mất hết).

| # | Class/Task | Package | Ghi chú |
|---|---|---|---|
| 1 | `CsvWriter` (bọc `Writer`): `writeRow(String...)`, escape theo RFC 4180 | `shared/csv` (module `domain`) | field có `,` `"` `\r` `\n` thì bọc `"…"`, `"` nhân đôi; xuống dòng `\r\n` |
| 2 | `CsvReader` (bọc `Reader`, `Closeable`): `nextRecord()` → `CsvRecord(lineNo, List<String>)` hoặc null; hoặc `Iterator`/`Stream` lười | `shared/csv` | đọc từng ký tự qua `BufferedReader`; field có ngoặc kép được chứa `,`/xuống dòng; `lineNo` = dòng vật lý **bắt đầu** bản ghi; bỏ BOM UTF-8 đầu file |
| 3 | `CsvFormatException` (unchecked, có `lineNo`) | `shared/csv` | ngoặc kép không đóng, ký tự rác sau ngoặc kép đóng |
| 4 | Port `OrderExportSource.forEachLine(Consumer<OrderLineRow>)`, `ProductExportSource.forEachProduct(Consumer<ProductRow>)` | `application/report/export` | callback thay vì trả `Stream`: adapter tự mở/đóng connection + transaction |
| 5 | `CsvExportService`: `exportOrders(Writer)`, `exportProducts(Writer)` | `application/report/export` | header cố định, số dùng `toPlainString()`, thời gian ISO-8601 UTC |
| 6 | `JdbcOrderExportSource`, `JdbcProductExportSource` + bản in-memory | `infrastructure/persistence/{jdbc,inmemory}` | **một** câu SQL mỗi loại, `JdbcClient…query(mapper).stream()` trong try-with-resources, `fetchSize` 1000, trong `TransactionTemplate` read-only (Postgres chỉ stream bằng cursor khi tắt autocommit); bundle items bằng `string_agg(item_id, '\|' ORDER BY position)` |
| 7 | `ProductCsvRow` (record, implements `ProductSpec`, `@ValidProductSpec` + annotation field như `ProductRequest`) | `application/catalog/importing` | `itemIds` tách từ cột `item_ids` theo `\|` |
| 8 | Port `ProductImportStore`: `existingIds(Collection<String>)`, `insertAll(List<Product>)` (batch INSERT, **không** upsert) | `application/catalog/importing` | JDBC + in-memory |
| 9 | `ProductImportService.importProducts(Reader, ImportOptions, ChunkListener)` → `ImportResult(totalRows, imported, failed, List<RowError> firstErrors)` | `application/catalog/importing` | chunk 1000 dòng trong `TransactionTemplate`; `ChunkListener.onChunkCommitted(...)` chạy **trong** transaction của chunk (task job dùng để ghi checkpoint) |
| 10 | REST: `GET /api/exports/orders.csv`, `GET /api/exports/products.csv` (`StreamingResponseBody`), `POST /api/imports/products` (multipart, đồng bộ, file ≤ 20MB) | `interfaces/rest/exporting`, `interfaces/rest/importing` | `text/csv; charset=UTF-8` + `Content-Disposition: attachment`; import trả JSON `ImportResult` |

**Định dạng file** (export product và import product dùng chung một định dạng: export ra rồi đổi id là import lại được)

```
orders.csv   : order_id,created_at,discount_rate,tax_rate,line_no,product_id,quantity,unit_price
products.csv : id,type,name,base_price,stock,category,weight_kg,months,discount_rate,item_ids
```
Một dòng `orders.csv` = một order line (đơn 3 dòng → 3 dòng CSV, thông tin đơn lặp lại). Ô trống = null. Thứ tự theo `seq`. Profile `local` không có `created_at` (order in-memory không lưu giờ tạo) → cột này để trống.

**Acceptance**
- [ ] Export 1 triệu order line và import file product ~300MB chạy được với `-Xmx256m` (kiểm tay bằng script sinh dữ liệu, ghi lệnh + thời gian vào PR)
- [ ] Export không N+1: đúng một câu SELECT mỗi file (kiểm bằng log SQL hoặc test đếm)
- [ ] Ngắt kết nối giữa chừng khi tải export thì connection được trả về pool (không rò)
- [ ] Import: dòng hợp lệ được lưu, dòng lỗi được báo đủ (số dòng + lý do), chunk đã commit không bị mất khi chunk sau lỗi
- [ ] Export products rồi import vào DB trống cho ra cùng dữ liệu (round-trip)
- [ ] `domain/` vẫn Java thuần (`shared.csv` chỉ dùng `java.io`); test cũ vẫn xanh

**Spec hành vi** (đề xuất 11/11, test: `CsvWriterTest`, `CsvReaderTest` trong `domain/src/test/java/com/training/ecommerce/shared/csv/`; `ProductImportServiceTest`, `CsvExportServiceTest` ở `app` với store in-memory; `Jdbc*ExportSourceTest`, `JdbcProductImportStoreTest` `@Tag("docker")`)

- **C1** Ghi rồi đọc lại bằng chính `CsvReader` luôn ra đúng giá trị ban đầu, kể cả field có `,` `"` xuống dòng, tiếng Việt, chuỗi rỗng.
- **C2** `CsvReader` lười: đọc bản ghi đầu từ một `Reader` vô hạn vẫn trả về ngay (chứng minh không đọc hết file).
- **C3** Header sai (thiếu/thừa/sai thứ tự cột) → IAE trước khi ghi bất kỳ dòng nào (REST 400). Dòng có số cột khác header → lỗi dòng, không dừng import.
- **C4** Import **chỉ thêm mới**: id đã có trong DB, hoặc trùng với dòng phía trên trong cùng file → lỗi dòng "trùng id"; không ghi đè.
- **C5** Lỗi dòng = lỗi parse (số/enum sai) + vi phạm Bean Validation (`ProductCsvRow`) + lỗi domain (IAE từ constructor). Mỗi dòng lỗi báo đủ mọi vi phạm của dòng đó, gộp một message.
- **C6** Bundle chỉ được tham chiếu item đã có trong DB **hoặc** đã nhập thành công ở dòng phía trên (kể cả cùng chunk chưa commit); item là dòng lỗi → bundle cũng lỗi.
- **C7** Chunk 1000 dòng hợp lệ, mỗi chunk một transaction; DB lỗi giữa chunk (vd. id bị request khác tạo cùng lúc) → rollback **chỉ chunk đó**, các dòng của chunk được báo lỗi, import chạy tiếp.
- **C8** `ImportResult.firstErrors` giữ tối đa 100 lỗi đầu; `failed` vẫn đếm đủ.
- **C9** Export ghi số bằng `toPlainString()` (không có `1E+3`), ô null là ô trống, thời gian `2026-11-11T02:00:00Z`.

| Method | Input | Kết quả mong đợi | Test |
|---|---|---|---|
| `CsvWriter.writeRow` | `a`, `b,c`, `say "hi"`, `x⏎y`, chuỗi rỗng | `a,"b,c","say ""hi""","x⏎y",` + `\r\n` | `escapesSpecialChars` |
| ghi → `CsvReader` | các field trên + `Áo thun` | 6 field đúng như lúc ghi (**C1**) | `roundTrip` (parameterized) |
| `CsvReader` | `Reader` sinh `a,b\r\n` vô hạn | bản ghi đầu = `[a, b]`, test xong trong 1 giây (**C2**) | `readsLazily` |
| `CsvReader` | file có BOM; xuống dòng `\n` lẫn `\r\n` | BOM bị bỏ; cả hai kiểu xuống dòng đọc đúng | `skipsBom`, `acceptsLfAndCrlf` |
| `CsvReader` | bản ghi 2 có field chứa 2 xuống dòng | bản ghi 3 có `lineNo` = 5 | `lineNoIsPhysicalStartLine` |
| `CsvReader` | `"abc` không đóng; `"a"x` | `CsvFormatException` có `lineNo` | `unclosedQuote`, `garbageAfterQuote` |
| `importProducts` | header thiếu cột `months` | IAE, store không có gì (**C3**) | `rejectsBadHeader` |
| `importProducts` | 3 dòng: P1 hợp lệ, P2 `weight_kg` = `abc`, P3 SUBSCRIPTION thiếu `months` | imported 1, failed 2, lỗi ở dòng 3 và 4 (**C5**) | `reportsRowErrorsAndContinues` |
| `importProducts` | dòng có `name` rỗng **và** `stock` −1 | 1 lỗi dòng, message nêu cả 2 field | `oneErrorPerRowWithAllViolations` |
| `importProducts` | id đã có trong store; id lặp ở dòng 2 và 5 | dòng đó / dòng 5 lỗi "trùng id", bản cũ giữ nguyên (**C4**) | `insertOnly`, `duplicateIdInFile` |
| `importProducts` | bundle B dùng P1 (dòng trên) + P9 (có trong DB); bundle C dùng P2 (dòng lỗi) | B nhập được; C lỗi (**C6**) | `bundleReferencesEarlierRowsOrDb` |
| `importProducts` | 2500 dòng hợp lệ, store ném lỗi khi insert chunk 2 | chunk 1 và 3 lưu (2000 dòng), 500 dòng chunk 2 báo lỗi (**C7**) | `failedChunkRollsBackOnlyItself` |
| `importProducts` | 250 dòng lỗi | `failed` 250, `firstErrors` 100 (**C8**) | `capsReportedErrors` |
| `exportOrders` | đơn 2 dòng giá `1000.00`, đơn 1 dòng | 1 header + 3 dòng, `unit_price` `1000.00` (**C9**) | `oneRowPerOrderLine` |
| export → import | 4 loại product, có bundle | DB mới có cùng dữ liệu | `productsRoundTrip` (`@Tag("docker")`) |

Ngoài spec: Excel; import order; import kiểu upsert / cập nhật tồn kho; nén gzip; chọn cột / lọc theo ngày khi export qua REST; file > 20MB qua REST (làm ở task job bên dưới); tự đoán encoding khác UTF-8; chạy nhiều import song song trên cùng dữ liệu.

**Thứ tự làm:** `shared.csv` #1–3 (Java thuần, đỏ trước) → export #4–6 + REST export → import #7–9 → REST import #10.

**Kiểm khi review:** không chỗ nào `readAllLines`/`readAllBytes`/`List` toàn bộ file hay toàn bộ kết quả SQL; mọi `Reader`/`Writer`/`Stream` từ JDBC đóng bằng try-with-resources; `StreamingResponseBody` chạy **sau** khi controller trả về, nên transaction phải mở bên trong lambda (`@Transactional` trên controller không có tác dụng ở đây); charset ghi rõ `UTF-8`, không dùng mặc định của máy; SQL dùng tham số; `CsvWriter` flush nhưng không đóng `Writer` của người gọi (bên tạo thì bên đóng).

---

## Extra – Job nền: hàng đợi import file lớn + export đơn hằng đêm
**Nhánh:** `extra/import-jobs` · đề xuất 11/11, theo luồng spec đỏ · làm sau *CSV* (dùng lại `ProductImportService`, `CsvExportService`)

Import đồng bộ ở task CSV chỉ hợp với file nhỏ: file vài trăm MB mất nhiều phút, vượt timeout HTTP, và nếu app chết giữa chừng thì không ai biết đã nhập tới đâu. Multipart của Spring mặc định chỉ nhận 1MB. Project chưa có `@EnableScheduling` hay `Clock`.

Mô hình: **hàng đợi job trong PostgreSQL**, không thêm broker. Upload lưu file xuống đĩa + một dòng `import_jobs` `PENDING` rồi trả 202; worker `@Scheduled(fixedDelay)` nhận job bằng `FOR UPDATE SKIP LOCKED` (nhiều instance không nhận trùng), giữ **lease** có hạn, ghi **checkpoint** (số dòng đã xử lý) và lỗi dòng **cùng transaction với chunk** — app chết rồi chạy lại thì tiếp từ checkpoint, không nhập trùng, không mất lỗi. Đã loại: `@Async` / thread pool trong bộ nhớ (mất job khi restart, không thấy tiến độ); Quartz / JobRunr (che mất phần cần học); Kafka/RabbitMQ (quá tầm).

```
PENDING ──claim──▶ RUNNING ──xong──▶ DONE
   ▲                  │  └──lỗi không phục hồi (header sai, file mất)──▶ FAILED
   └── lease hết hạn (app chết) và attempts < 3 ──┘   attempts = 3 ──▶ FAILED
```

| # | Class/Task | Package | Ghi chú |
|---|---|---|---|
| 1 | Bảng `import_jobs` (`id`, `status`, `file_path`, `total_rows`, `imported`, `failed`, `checkpoint_line`, `attempts`, `locked_until`, `message`, `created_at`, `started_at`, `finished_at`) + `import_job_errors` (`job_id`, `line_no`, `message`, PK `(job_id, line_no)`) | `schema.sql` | CHECK `status` như enum |
| 2 | `ImportJob` (record) + `ImportJobStatus` (enum) | `application/catalog/importing/job` | job là chuyện của ứng dụng, không đưa vào `domain/` |
| 3 | Port `ImportJobStore`: `create`, `claimNext(now, lease)`, `extendLease`, `saveProgress` (checkpoint + lỗi, gọi trong transaction chunk), `finish`, `fail`, `findById`, `errors(jobId, Consumer)` | `application/catalog/importing/job` | JDBC + in-memory (`synchronized`) |
| 4 | `ImportJobService.submit(InputStream, filename)` → job `PENDING`; `ImportJobWorker.runOnce()` xử lý **tối đa một** job | `application/catalog/importing/job` | `runOnce()` public để test gọi thẳng, không chờ lịch |
| 5 | `NightlyOrderExportJob.run()` + `CleanupJob.run()` | `application/report/export` | export đơn **ngày hôm trước** ra `exports/orders-YYYY-MM-DD.csv`; xoá export > 30 ngày, file import của job `DONE`/`FAILED` > 7 ngày |
| 6 | `SchedulingConfig`: `@EnableScheduling`, bean `Clock` (`Asia/Ho_Chi_Minh`), các `@Scheduled` gọi `runOnce()` / `run()` | `infrastructure/config` | bật bằng `app.scheduling.enabled` (mặc định `true`, test để `false`); export đêm chỉ ở profile `dev` (order in-memory không có `created_at`) |
| 7 | REST: `POST /api/import-jobs` (multipart, 202 + `Location`), `GET /api/import-jobs/{id}` (trạng thái + tiến độ), `GET /api/import-jobs/{id}/errors.csv` (stream) | `interfaces/rest/importing` | `spring.servlet.multipart.max-file-size=1GB`, `file-size-threshold` để multipart ghi ra đĩa; controller dùng `transferTo(Path)`, không đọc vào `byte[]` |
| 8 | Cấu hình `app.import.dir`, `app.export.dir`, `app.import.lease` (mặc định 2 phút) | `application*.properties` | thư mục tạo khi khởi động nếu chưa có |

**Acceptance**
- [ ] Upload file ~300MB trả 202 trong vài giây; `GET` thấy tiến độ tăng dần tới `DONE`
- [ ] Kill app giữa chừng rồi chạy lại: job tiếp tục từ checkpoint, tổng `imported` + `failed` = số dòng file, không có product nào bị nhập hai lần, lỗi dòng không bị ghi trùng
- [ ] Hai instance (hoặc hai thread gọi `runOnce()`) không bao giờ xử lý cùng một job
- [ ] Export đêm chạy lại trong cùng ngày ghi đè đúng file đó; không bao giờ để lại file CSV dở dang ở tên chính thức
- [ ] Test không phụ thuộc giờ thật hay lịch chạy thật (inject `Clock`, gọi `runOnce()`/`run()` trực tiếp)
- [ ] `domain/` không đổi; test cũ vẫn xanh

**Spec hành vi** (đề xuất 11/11, test: `ImportJobWorkerTest`, `NightlyOrderExportJobTest`, `CleanupJobTest` với store in-memory + `Clock.fixed`; `JdbcImportJobStoreTest` `@Tag("docker")`; `ImportJobControllerTest`)

- **J1** `claimNext` lấy job `PENDING` cũ nhất, **hoặc** job `RUNNING` có `locked_until < now`; đổi sang `RUNNING`, `attempts + 1`, `locked_until = now + lease`. JDBC làm trong **một** câu `UPDATE … WHERE id = (SELECT … FOR UPDATE SKIP LOCKED LIMIT 1) RETURNING …`.
- **J2** Sau mỗi chunk: checkpoint, đếm `imported`/`failed`, lỗi dòng và gia hạn lease cùng commit với dữ liệu chunk. Chạy lại job thì bỏ qua các dòng ≤ `checkpoint_line`.
- **J3** `attempts` đạt 3 mà vẫn chưa xong (app chết lặp lại) → `FAILED`, giữ nguyên dữ liệu đã nhập. Lỗi không phục hồi (header sai, file mất) → `FAILED` ngay, không thử lại.
- **J4** Worker mất lease (job bị instance khác nhận lại) thì `saveProgress` thất bại (điều kiện `locked_until`/`attempts` không khớp) → chunk rollback, worker dừng job đó, không ghi đè tiến độ của instance kia.
- **J5** Export đêm lấy đơn có `created_at` trong `[00:00 hôm qua, 00:00 hôm nay)` giờ `Asia/Ho_Chi_Minh`; ghi ra `orders-YYYY-MM-DD.csv.tmp` rồi `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)`.
- **J6** Cleanup chỉ xoá file trong `app.export.dir`/`app.import.dir` theo tuổi; không xoá file của job `PENDING`/`RUNNING`.

| Method | Input | Kết quả mong đợi | Test |
|---|---|---|---|
| `runOnce` | không có job | không làm gì, trả `false` | `idleWhenNoJob` |
| `runOnce` | job `PENDING`, file 2500 dòng (10 lỗi) | `DONE`, imported 2490, failed 10, checkpoint = dòng cuối | `processesJobToDone` |
| `claimNext` | job `RUNNING` có `locked_until` = now − 1s, attempts 1 | nhận được, attempts 2 (**J1**) | `reclaimsExpiredLease` |
| `claimNext` | job `RUNNING` còn lease | không nhận | `respectsActiveLease` |
| `runOnce` | job `RUNNING` hết lease, checkpoint ở dòng 1001 | chỉ xử lý từ dòng 1002; tổng cuối không trùng (**J2**) | `resumesFromCheckpoint` |
| `runOnce` | job hết lease với attempts 3 | `FAILED`, message nêu số lần thử (**J3**) | `failsAfterMaxAttempts` |
| `runOnce` | file sai header | `FAILED` ngay, attempts 1 | `badHeaderFailsWithoutRetry` |
| 2 thread `claimNext` | 1 job `PENDING` (lặp 100 lần) | đúng 1 thread nhận | `noDoubleClaim` (JDBC `@Tag("docker")` + in-memory) |
| `saveProgress` | lease đã bị instance khác lấy | ném lỗi, chunk rollback (**J4**) | `lostLeaseStopsWorker` |
| `NightlyOrderExportJob.run` | `Clock` = 11/11 02:00 +07; đơn lúc 10/11 23:59 +07 và 11/11 00:00 +07 | file `orders-2026-11-10.csv` chỉ có đơn thứ nhất (**J5**) | `exportsPreviousLocalDay` |
| `NightlyOrderExportJob.run` | chạy 2 lần; lần 2 bị lỗi giữa chừng | file chính thức là bản đầy đủ của lần 1, không có `.tmp` sót | `atomicReplace` |
| `CleanupJob.run` | export 31 ngày / 29 ngày; file import của job `RUNNING` 10 ngày | xoá đúng file export 31 ngày (**J6**) | `deletesOnlyExpiredFiles` |
| `POST /api/import-jobs` | file CSV | 202, `Location: /api/import-jobs/{id}`, body `status` `PENDING` | `submitReturnsAccepted` |

Ngoài spec: nhiều worker song song trong cùng instance; ưu tiên job; huỷ job đang chạy; retry có backoff; thông báo khi xong (email/webhook); chống chạy trùng export đêm khi có nhiều instance (ShedLock / `pg_try_advisory_lock`); lưu file lên S3; theo dõi bằng metrics.

**Thứ tự làm:** schema + store #1–3 (test JDBC `SKIP LOCKED` trước) → worker #4 (J1–J4) → export đêm + cleanup #5 → config + REST #6–8.

**Kiểm khi review:** checkpoint và lỗi dòng nằm **trong** transaction của chunk (không phải sau commit); worker không giữ transaction trong lúc đọc file giữa hai chunk; `@Scheduled` mặc định chỉ có một thread — job dài không được chặn export đêm (cấu hình `spring.task.scheduling.pool.size` hoặc ghi rõ lý do); mọi chỗ lấy giờ đều qua `Clock`; `Files.move` atomic cùng thư mục; file upload không bao giờ bị đọc hết vào RAM.

---

## Extra – Gom lỗi nghiệp vụ: `BusinessException` + mã lỗi trong ProblemDetail
**Nhánh:** `extra/request-validation` · đề xuất 14/11 · checklist: **Exception**

Trước đây `RestExceptionHandler` liệt kê từng class (`notFound`: 3 class, `conflict`: 4 class) và phải dựa vào "Spring chọn handler gần nhất" để 404/409 thắng 400 (vì `ProductNotFoundException` là IAE, `OutOfStockException` là ISE). Thêm một lỗi nghiệp vụ mới phải nhớ sửa handler; response chỉ có `detail` tiếng Việt, client không có mã ổn định để xử lý bằng máy.

Mô hình: một cha chung `BusinessException` (domain, Java thuần) mang `Category` (nhóm lỗi) và `code` (chuỗi ổn định). Handler map **theo category** ở một chỗ và thêm property `code` vào `ProblemDetail`.

| # | Class/Task | Package | Ghi chú |
|---|---|---|---|
| 1 | `BusinessException` (abstract, `Category { NOT_FOUND, CONFLICT }`, `getCategory()`, `getCode()`) | `shared/error` (module `domain`) | validate category/code → IAE |
| 2 | 3 exception của domain extends `BusinessException` | `domain/catalog`, `domain/shipping` | giữ constructor, getter, message |
| 3 | 4 exception của app extends `BusinessException` | `application/{order,shipping,catalog}` | `ProductInUseException` giữ `cause` |
| 4 | `RestExceptionHandler`: một handler `BusinessException`, switch category → status, property `code` | `interfaces/rest` | bỏ `notFound`/`conflict` |
| 5 | `S02Demo` bắt thêm `BusinessException` | `demo` | in kết quả như cũ |

**Quyết định**
- `HttpStatus` không đưa vào domain (domain không có Spring): domain chỉ biết `Category`, tầng `interfaces` map sang status.
- Chỉ có `NOT_FOUND` và `CONFLICT`; chưa có `INVALID` vì chưa có chỗ dùng — lỗi đầu vào thuần vẫn là `IllegalArgumentException` → 400.
- `OrderDetailLoadException` là lỗi kỹ thuật (không đọc được dữ liệu) nên **không** thuộc `BusinessException`, vẫn 500.
- Hệ quả: 7 exception trên **không còn** là `IllegalArgumentException` / `IllegalStateException`. Code bắt `catch (IllegalStateException | IllegalArgumentException)` để bắt chúng phải bắt thêm `BusinessException`; test assert lớp cha ISE/IAE được đổi sang assert đúng exception cụ thể (đổi spec có chủ đích).

| Exception | Category | Code | REST status |
|---|---|---|---|
| `ProductNotFoundException` | NOT_FOUND | `PRODUCT_NOT_FOUND` | 404 |
| `OrderNotFoundException` | NOT_FOUND | `ORDER_NOT_FOUND` | 404 |
| `ShipmentNotFoundException` | NOT_FOUND | `SHIPMENT_NOT_FOUND` | 404 |
| `OutOfStockException` | CONFLICT | `OUT_OF_STOCK` | 409 |
| `IllegalShipmentTransitionException` | CONFLICT | `ILLEGAL_SHIPMENT_TRANSITION` | 409 |
| `ActiveShipmentExistsException` | CONFLICT | `ACTIVE_SHIPMENT_EXISTS` | 409 |
| `ProductInUseException` | CONFLICT | `PRODUCT_IN_USE` | 409 |

Response: `ProblemDetail` như cũ (`status`, `detail`) cộng property `code`. `DataIntegrityViolationException` (409) và IAE (400) không có `code`.

**Acceptance**
- [x] Một handler `BusinessException` thay cho `notFound` + `conflict`; status giữ nguyên như trước
- [x] Response 404/409 của 7 exception có `code` đúng bảng (test controller)
- [x] `BusinessException` validate category/code (`BusinessExceptionTest`)
- [x] `domain/` vẫn Java thuần; test cũ xanh (chỉ sửa test assert lớp cha ISE/IAE)
- [x] Chạy `visualizer-sync`: `domain-api.txt` không lệch (snapshot không ghi lớp cha), visualizer không bắt/mô tả các exception này theo ISE/IAE nên không phải sửa
