# ADR-0002: Gom lỗi nghiệp vụ về `BusinessException` (category + code)

- **Trạng thái:** Chấp nhận, đã triển khai (commit `c83edff`, merge `f403c7b`)
- **Ngày:** 2026-11-14
- **Phạm vi:** `domain/` (`shared/error`, các exception trong `domain.**`), `app/` (`application.**`, `interfaces/rest/RestExceptionHandler`)
- **Chi tiết task và spec:** `docs/ecommerce-backlog.md`, mục "Extra – Gom lỗi nghiệp vụ"

## Bối cảnh

Trước quyết định này có 7 exception nghiệp vụ, mỗi cái tự chọn lớp cha:

| Exception | Lớp cha cũ | HTTP |
|---|---|---|
| `ProductNotFoundException` (domain) | `IllegalArgumentException` | 404 |
| `OutOfStockException`, `IllegalShipmentTransitionException` (domain) | `IllegalStateException` | 409 |
| `ActiveShipmentExistsException` (app) | `IllegalStateException` | 409 |
| `OrderNotFoundException`, `ShipmentNotFoundException`, `ProductInUseException` (app) | `RuntimeException` | 404 / 409 |

Vấn đề:

1. `RestExceptionHandler` liệt kê từng class trong hai handler `notFound` và `conflict`. Thêm một lỗi nghiệp vụ mới mà quên sửa handler thì lỗi rơi xuống handler IAE (400) hoặc thành 500.
2. 404/409 thắng 400 chỉ nhờ quy tắc "Spring chọn handler gần exception nhất", vì `ProductNotFoundException` cũng là IAE và `OutOfStockException` cũng là ISE. Đúng nhưng dễ vỡ và khó thấy.
3. Response chỉ có `detail` tiếng Việt. Client không có mã ổn định để xử lý bằng máy.

## Quyết định

1. Thêm `com.training.ecommerce.shared.error.BusinessException` (abstract, extends `RuntimeException`) trong module `domain`, Java thuần:
   - `enum Category { NOT_FOUND, CONFLICT }`
   - `getCategory()`, `getCode()` (chuỗi ổn định, vd. `OUT_OF_STOCK`)
   - constructor `protected (category, code, message)` và `(category, code, message, cause)`; category null hoặc code null/rỗng → `IllegalArgumentException`
2. 7 exception trên kế thừa `BusinessException`, giữ nguyên constructor, getter và message:

   | Exception | Category | Code | HTTP |
   |---|---|---|---|
   | `ProductNotFoundException` | NOT_FOUND | `PRODUCT_NOT_FOUND` | 404 |
   | `OrderNotFoundException` | NOT_FOUND | `ORDER_NOT_FOUND` | 404 |
   | `ShipmentNotFoundException` | NOT_FOUND | `SHIPMENT_NOT_FOUND` | 404 |
   | `OutOfStockException` | CONFLICT | `OUT_OF_STOCK` | 409 |
   | `IllegalShipmentTransitionException` | CONFLICT | `ILLEGAL_SHIPMENT_TRANSITION` | 409 |
   | `ActiveShipmentExistsException` | CONFLICT | `ACTIVE_SHIPMENT_EXISTS` | 409 |
   | `ProductInUseException` | CONFLICT | `PRODUCT_IN_USE` | 409 |

3. `RestExceptionHandler` chỉ còn một handler `@ExceptionHandler(BusinessException.class)`: `switch` đầy đủ trên `Category` → `HttpStatus`, trả `ProblemDetail` với `detail` = message và thêm property `code`.
4. Ngoài phạm vi:
   - `OrderDetailLoadException` là lỗi kỹ thuật (không tải được dữ liệu), không kế thừa `BusinessException`, vẫn 500.
   - Lỗi đầu vào thuần (null, số âm, id body khác path…) vẫn là `IllegalArgumentException` → 400.
   - `DataIntegrityViolationException` vẫn 409, không có `code`.

## Lý do

- **Có nơi dùng kiểu cha một cách polymorphic.** Handler xử lý mọi lỗi nghiệp vụ qua `BusinessException` và đọc `category`/`code`. Đây là điều kiện để lớp cha có giá trị (ngược với trường hợp ở [ADR-0001](0001-khong-dung-base-controller-base-service.md)).
- **Thêm lỗi mới không phải sửa handler.** Chỉ cần chọn category và code.
- **Domain không biết HTTP.** Domain chỉ khai báo *loại* lỗi; việc map sang status nằm ở tầng `interfaces`. Giữ dependency rule `interfaces → application → domain`, và `maven-enforcer` vẫn chặn Spring khỏi `domain`.
- **Đặt ở `shared` của module `domain`** vì cả exception của domain lẫn của app đều cần kế thừa.
- **Mã lỗi ổn định cho client**, tách khỏi message tiếng Việt có thể đổi câu chữ.

## Phương án đã cân nhắc

| Phương án | Kết luận |
|---|---|
| Giữ nguyên, liệt kê class trong handler | Loại: phải nhớ sửa handler, dựa vào quy tắc "handler gần nhất", không có `code` |
| Hai lớp cha `NotFoundException`, `ConflictException` | Đơn giản, hợp Java, nhưng không có `code`; muốn có `code` vẫn phải thêm vào cả hai |
| Đưa `HttpStatus` (hoặc số status) vào exception | Loại: domain phụ thuộc khái niệm HTTP |
| Marker interface để giữ lớp cha IAE/ISE | Loại: `@ExceptionHandler` chỉ nhận lớp `Throwable`; phải bắt `RuntimeException` rồi `instanceof`, rối hơn |
| Thêm `Category.INVALID` | Hoãn: chưa có exception nào dùng; thêm khi cần |

## Hệ quả

- **7 exception không còn là `IllegalArgumentException` / `IllegalStateException`.** Code bắt ISE/IAE để xử lý hết hàng hay không tìm thấy phải bắt `BusinessException` (đã sửa `S02Demo`). Test assert lớp cha ISE/IAE được đổi sang assert đúng exception cụ thể; các dòng spec cũ trong backlog (S02, S04, G6) có ghi chú trỏ về mục mới.
- Response 404/409 của lỗi nghiệp vụ có thêm `code`; status không đổi.
- Thêm lỗi nghiệp vụ mới: extends `BusinessException`, chọn category, đặt code dạng `UPPER_SNAKE_CASE`, thêm test controller kiểm `$.code`.
- Visualizer không phải sửa: `domain-api.txt` không ghi lớp cha, và visualizer không phân biệt các exception này theo ISE/IAE.

## Khi nào xem lại

- Khi cần thêm nhóm lỗi mới (vd. `INVALID` cho lỗi nghiệp vụ 422, `FORBIDDEN` khi có phân quyền): thêm giá trị vào `Category` và nhánh `switch`, không cần ADR mới.
- Khi cần trả thêm dữ liệu có cấu trúc cho client (vd. `productId`, `available` của `OutOfStockException`) hoặc đa ngôn ngữ cho message: viết ADR mới.
