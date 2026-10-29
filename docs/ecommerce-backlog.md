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
| 8 | `PricingEngine.calculateRevenue(List<Product>)` | `application/pricing` (từ 28/10) | không dùng `instanceof` trong phần tính giá |
| 9 | `equals`/`hashCode` theo `id`, `toString` | `domain/catalog` | |

> Đã merge (tag `s03-done`).

**Acceptance**
- [x] Demo: list có đủ các loại product → in tổng doanh thu
- [x] Thêm `BundleProduct` mà **không sửa** `PricingEngine` (chứng minh OCP)
- [x] Test: 2 product cùng id → `HashSet.size() == 1`
- [ ] Mô tả PR có mục "SOLID check": S/O/L/I/D, mỗi nguyên tắc 1 dòng _(chưa có PR: merge trực tiếp 21/10)_
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

**Acceptance**
- [ ] `Repository` generic dùng được cho `Product` mà không ép kiểu; không có raw type
- [ ] `InventoryManager` không còn `Product[]`; tra theo id bằng `Map`
- [ ] `Cart` gộp cùng sản phẩm bằng `Map.merge` (hoặc tương đương); `getItems()` trả về view / bản copy không sửa được
- [ ] Domain exception là unchecked, message rõ ràng; code/test S02–S03 vẫn xanh (hoặc nâng cấp có chủ đích, ghi rõ)
- [ ] Không sửa collection trong lúc for-each (dùng `removeIf` / `Iterator.remove`)
- [ ] Key của `HashMap`/`HashSet` là bất biến (id `final`)
- [ ] Bonus: API nhận nhiều product dùng wildcard, ví dụ `saveAll(Collection<? extends T>)`

**Spec hành vi** (chốt 21/10, test: `app/src/test/java/com/training/ecommerce/infrastructure/persistence/inmemory/InMemoryRepositoryTest.java`, `domain/src/test/java/com/training/ecommerce/domain/{order/CartTest, catalog/DomainExceptionTest}.java`; test S02–S03 giữ nguyên)

API (quyết định 21/10): `Identifiable<ID>` + `Repository<T extends Identifiable<ID>, ID>` (`save`, `saveAll(Collection<? extends T>)`, `findById` → `null` khi không có (S05: `Optional`), `findAll`, `deleteById`, `count`) ở `shared/repository`; `InMemoryRepository` ở app `infrastructure/persistence/inmemory`; `Product implements Identifiable<String>`. Exception **ở `domain/catalog`** (tránh vòng phụ thuộc catalog ↔ inventory): `ProductNotFoundException extends IllegalArgumentException` (`getProductId`), `OutOfStockException extends IllegalStateException` (`getProductId/getRequested/getAvailable`). `Cart` ở `domain/order`. Chưa dùng `Optional`/Stream (S05).

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
| Exception | `new OutOfStockException("P002", 21, 20)`, `new ProductNotFoundException("P999")` | unchecked, đúng lớp cha (ISE / IAE), getter đúng, message có id | `outOfStockCarriesData`, `notFoundCarriesId` |
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

**Acceptance**
- [ ] Không còn vòng `for`/`while` trong `InventoryManager`, `Order`, `PricingEngine`, `ReportService`; dùng Stream, method reference khi làm code dễ đọc hơn. Ngoại lệ (chốt 21/10): bước có side effect (trừ tồn kho trong `decreaseStock`) dùng vòng `for` cho rõ ý, không dùng `forEach`
- [ ] Mỗi quy tắc giá là một hàm; ghép bằng `andThen` / `reduce(identity, andThen)`; thêm rule mới không sửa engine (OCP)
- [ ] Báo cáo dùng `groupingBy` + `reducing` cho `BigDecimal`, `partitioningBy`, top N bằng `sorted` + `limit`; key trùng không ném exception (merge function hoặc `groupingBy`)
- [ ] Logic tính toán pure: không sửa list / `Order` đầu vào, không đọc giờ hệ thống; test không cần mock
- [ ] Test S02 + S03 vẫn xanh sau refactor (hoặc được nâng cấp có chủ đích, ghi rõ)
- [ ] Không `forEach` để add vào list bên ngoài, không đặt logic trong `peek`, không `Optional.get()` trần
- [ ] `findById` trả `Optional` ở `InventoryManager` và `Repository` (đổi D1 của S04, chốt 21/10)

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

**Acceptance**
- [ ] 100 thread cùng mua sản phẩm còn 1: đúng 1 lần thành công, các lần còn lại ném `OutOfStockException`, stock cuối = 0, không bao giờ âm
- [ ] Nhiều đơn nhiều dòng chạy đồng thời vẫn all-or-nothing: không đơn nào bị trừ một phần, tổng đã trừ = tổng số lượng của các đơn thành công
- [ ] Kiểm tra và trừ nằm trong cùng một lock (không check-then-act bên ngoài lock); lock ngắn, không gọi IO hay `sleep` khi đang giữ lock
- [ ] `transfer` hai chiều (A → B và B → A) chạy song song nhiều lần không treo (test có timeout); tổng tồn kho hai kho không đổi
- [ ] `OrderDetailService`: 3 nguồn chạy song song, tổng thời gian ≈ nguồn chậm nhất; service không tự `new Thread()` và không tự tạo pool; bên nào tạo `ExecutorService` thì bên đó đóng (try-with-resources / `shutdown`)
- [ ] Không nuốt `InterruptedException` (bật lại cờ bằng `Thread.currentThread().interrupt()` hoặc ném tiếp); không dùng `volatile` cho thao tác đọc-sửa-ghi
- [ ] Module `domain/` vẫn là Java thuần; test S02–S05 vẫn xanh

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

**Acceptance**
- [ ] Profile `local` (mặc định): BE chạy trên IDE, không cần DB, có sẵn dữ liệu mẫu
- [ ] Profile `dev`: `docker compose up --build` chạy cả DB lẫn BE; dữ liệu còn sau `docker compose down` / `up`
- [ ] Đặt đơn nhiều dòng là all-or-nothing trong DB: một dòng thiếu hàng thì không dòng nào bị trừ (rollback)
- [ ] Nhiều request cùng mua sản phẩm cuối: đúng một đơn thành công, stock không âm
- [ ] Đơn đọc lại từ DB giữ giá snapshot lúc đặt, kể cả khi giá sản phẩm đổi sau đó
- [ ] Swagger UI ở `/swagger-ui.html` có đủ API product và order
- [ ] Module `domain/` vẫn Java thuần; test S02–S06 vẫn xanh

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

**Acceptance**
- [ ] `replenish` thành công: kho giảm và hub tăng đúng `quantity`; lỗi thì cả kho lẫn hub giữ nguyên
- [ ] Không ai thấy trạng thái "kho đã trừ, hub chưa cộng"
- [ ] `replenish`, `transfer` hai chiều và `decreaseStock` chạy song song không treo (test có timeout); tổng hub + các kho + đã bán không đổi
- [ ] Thứ tự khóa toàn cục ghi trong Javadoc `InventoryManager` và `Warehouse`
- [ ] `domain/` vẫn Java thuần; test S02–S07 vẫn xanh

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
| 6 | Port `StockStore.increaseStock(Order)` + `ShipmentRepository` (`findActiveByOrderId`) | `application/{inventory, shipping}` | |
| 7 | `ShipmentService`: `create(orderId, address)`, `pickUp`, `deliver`, `failAttempt`, `refuse`, `markReturned`, `cancel` | `application/shipping` | `markReturned`/`cancel` là `@Transactional`: đổi trạng thái + hoàn kho cùng commit/rollback |
| 8 | JDBC + in-memory (`shipments`, `shipment_events`), REST `/api/shipments` | `infrastructure`, `interfaces/rest/shipping` | ISE → 409, IAE → 400, không tìm thấy → 404 |
| 9 | Visualizer: kịch bản giao thành công và giao thất bại 3 lần → hoàn kho | `visualizer` | sau khi domain xanh, chạy skill `visualizer-sync` |

**Spec hành vi** (đề xuất 30/10, test: `ShipmentTest`, `ShippingFeeCalculatorTest` trong `domain/src/test/java/com/training/ecommerce/domain/shipping/`, `InventoryManagerRestockTest` trong `domain/src/test/java/com/training/ecommerce/domain/inventory/`, `ShipmentServiceTest` ở `app`)

- **G1** Vận đơn mới: `CREATED`, `failedAttempts = 0`, `trackingCode` không rỗng, `fee` scale 2, history có đúng một event `CREATED`.
- **G2** Phí theo cả đơn, `baseFee` một lần: không cộng `calculateShipping` từng dòng.
- **G3** Đơn không có dòng hàng vật lý nào (chỉ digital/subscription) → IAE khi tính phí / tạo vận đơn; `order`, `address`, `orderId` null/rỗng → IAE.
- **G4** `failAttempt(lý do)` chỉ ở `IN_TRANSIT`: `failedAttempts + 1`, giữ `IN_TRANSIT`; lần thứ 3 (`MAX_ATTEMPTS = 3`) tự chuyển `RETURNING`. Lý do null/rỗng → IAE (kiểm trước trạng thái).
- **G5** `cancel()` chỉ ở `CREATED`; đã lấy hàng thì phải `refuse` → `markReturned`.
- **G6** Chuyển trạng thái không có trong sơ đồ → `IllegalStateException`, message nêu trạng thái hiện tại; trạng thái kết thúc (`DELIVERED`, `RETURNED`, `CANCELLED`) không chuyển đi đâu được. Ném lỗi thì trạng thái và history không đổi.
- **G7** `restock(Order)` cộng lại đúng số lượng theo product của đơn; product không có trong hub → `ProductNotFoundException`, không product nào được cộng (all-or-nothing như `decreaseStock`). Ở `ShipmentService`: `RETURNED` và `CANCELLED` hoàn kho, `DELIVERED` không.
- **G8** Mỗi đơn có tối đa một vận đơn chưa kết thúc: tạo thêm → ISE (REST 409); vận đơn trước đã `CANCELLED`/`RETURNED` thì được tạo mới.

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
| `ShipmentService.create` | đơn đã có vận đơn `IN_TRANSIT`; vận đơn cũ `CANCELLED` | ISE; tạo được (**G8**) | `rejectsSecondActiveShipment`, `allowsNewAfterCancelled` |

Ngoài spec: trả hàng **sau khi đã giao** (`DELIVERED → RETURNING`, luồng đổi trả riêng), hoàn tiền (`PaymentGateway` hiện chỉ đọc), timestamp / `Clock` cho event, nhiều kiện cho một đơn, giao từng phần, phí ship cộng vào `finalAmount` của `Order`, tích hợp API hãng vận chuyển thật (nối `ShipmentTracker` vào `ShipmentRepository` để sau).

**Thứ tự làm:** domain #1–5 (G1–G7 phần domain, Java thuần, test đỏ trước) → application + persistence + REST #6–8 (G7 phần service, G8) → visualizer #9.

**Kiểm khi review:** chuyển trạng thái nằm trong `Shipment` (service không tự set status); lỗi chuyển trạng thái không để lại thay đổi nửa vời; `restock` là all-or-nothing và an toàn đa luồng như `decreaseStock`; hoàn kho cùng transaction với đổi trạng thái; `domain/` vẫn Java thuần; đã chạy `visualizer-sync`.
