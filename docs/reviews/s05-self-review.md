# Self-review S05 – `s05/stream-report`

## Lần 1 – 21/10

**Build/test:** 285/285 xanh (javac + JUnit 5.13.4 ở cloud vì máy chỉ có JDK 11; chưa kiểm enforcer qua Maven, `pom.xml` không đổi từ S04) · **Spec test S05:** 53/53 xanh, không bị sửa kể từ `e09c1df` · **Acceptance:** 7/7 đạt

Phạm vi: `86b8ce3..` commit `[S05] review` (nhánh nằm trên các commit S04 chưa merge). Không commit `target/`, `.idea/`, `*.class`.

> Phần lớn code S05 do Claude viết theo yêu cầu (`amountOff`, `andThen`, constructor có category, `InventoryManager`, `PricingEngine`, `ReportService`, `S05Demo`). Tùng tự làm `Product`, `percentOff`, `identity`, `chain`, `Order.subtotal` → trước T4 cần tự đọc và giải thích được từng pipeline.

### Bắt buộc sửa trước T4
Không có.

### Nên sửa
1. [ ] **Kiểm null không đồng đều:** `PricingEngine` ném IAE khi `products` chứa `null`, còn `ReportService` để văng NPE khi list order chứa `null` (đã kiểm chứng: `revenueByCategory([o, null])` → NPE ở `Order.getLines()`). Chọn một quy ước chung, bổ sung vào bảng Spec.
2. [ ] **Rule trả `null`:** `calculateRevenue(products, p -> null)` văng NPE trong `BigDecimal.add`, xa chỗ gây lỗi (đã kiểm chứng). Ngoài spec; cân nhắc `Objects.requireNonNull` sau `apply`.
3. [x] **`ReportService.requireNonNull` dễ nhầm với `Objects.requireNonNull` (ném NPE):** đã đổi thành `checkNotNull` (commit `[S05] review`).
4. [ ] **Còn 2 vòng `for` ngoài phạm vi Acceptance:** `BundleProduct.totalFinalPrice`, `InMemoryRepository.saveAll`. Không bắt buộc.
5. [ ] **Comment thừa** `// 1. Validate`, `// 2. Gộp danh sách` trong `PricingRule`: comment nên nói *vì sao*.

### Câu hỏi mang lên T4
- [x] `forEach` có side effect (`required.forEach(Product::decreaseStock)`, `List.of(...).forEach(inventory::addProduct)`)? → **Chốt 21/10: dùng vòng `for` cho rõ ý** (commit `[S05] review`); ghi ngoại lệ vào Acceptance S05. Vẫn mang lên T4 để nghe mentor. Còn `revenueByCategory(...).forEach(... println ...)` trong `S05Demo` giữ nguyên.
- Ném exception trong `ifPresent(e -> { throw ... })` (bước kiểm all-or-nothing) so với `anyMatch` + `if`.
- `PricingRule` riêng hay `extends UnaryOperator<BigDecimal>` (`andThen(lambda)` bị ambiguous với `Function.andThen`)?
- `identity()` có nên chuẩn hóa scale 2? (`chain(List.of()).apply(150000)` → `150000`, spec đang chấp nhận.)
- `EnumMap` hay `HashMap` cho báo cáo; thứ tự key có nên nằm trong spec?
- Slide S05: `inStock.and(digital.negate())` lọc "cần giao" sai vì Subscription/Bundle cũng lọt (đã ghi ở mục 6 session-05).

### Làm tốt
- `Order.subtotal` bằng `Arrays.stream(lines, 0, lineCount)` + `reduce`: tự sửa đúng lỗi NPE do mảng còn ô trống.
- `percentOff` validate (0, 1] bằng `compareTo`, Javadoc khớp.
- `chain` = `reduce(identity(), PricingRule::andThen)`, kiểm null bằng `anyMatch(Objects::isNull)`: né được bẫy `List.of().contains(null)`.
- `Product`: một constructor chính làm toàn bộ validation, các constructor khác ủy quyền.
- Không lặp lỗi cũ trong `mistakes.md`: `toMap` luôn có merge function, không có logic trong `peek`.

### Để sau (ngoài phạm vi buổi)
- `Order` vẫn tự quản lý mảng → đổi sang `List<OrderLine>` (S04); `getLines()` trả `List` sẽ ảnh hưởng test.
- Ghi bẫy `BigDecimal.max` vào `mistakes.md`: trả về đúng object lớn hơn, giữ scale của nó (`S05Demo` từng in `130000` thay vì `130000.00`).
