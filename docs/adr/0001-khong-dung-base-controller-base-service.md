# ADR-0001: Không tạo `BaseController` / `BaseService` dùng chung

- **Trạng thái:** Chấp nhận
- **Ngày:** 2026-11-14
- **Phạm vi:** `app/` — `interfaces/rest/**`, `application/**`

## Bối cảnh

Sau khi gom lỗi nghiệp vụ về `BusinessException` ([ADR-0002](0002-business-exception-category-code.md)), câu hỏi tiếp theo là có nên làm tương tự cho controller và service: tạo abstract class `BaseController`, `BaseService<T, ID>` để các class hiện có kế thừa.

Hiện trạng lúc quyết định:

| Class | Hình dạng |
|---|---|
| `ProductService` | CRUD (`findAll`, `findById`, `save`, `update`, `delete`) |
| `OrderService` | use case `placeOrder` (trừ kho + lưu đơn cùng transaction), `findById`, `findAll` |
| `ShipmentService` | `create` + chuỗi chuyển trạng thái (`pickUp`, `deliver`, `failAttempt`, `refuse`, `markReturned`, `cancel`) |
| `ReportService`, `OrderDetailService` | không có repository riêng; gọi song song qua `ExecutorService` được inject |
| `ProductController`, `OrderController`, `ShipmentController` | route, DTO, mapper, Swagger annotation riêng; chỗ lặp thật chỉ là dựng URI cho header `Location` khi trả 201 |

Phần cross-cutting của tầng REST đã được giải bằng cơ chế của Spring: `RestExceptionHandler` (`@RestControllerAdvice`) cho lỗi, `@Valid` + Bean Validation cho request.

## Quyết định

**Không tạo `BaseController` hay `BaseService`.** Mỗi controller và service là một class độc lập, chỉ phụ thuộc qua constructor.

Code lặp được gom bằng **composition** (helper tĩnh, port, cơ chế của Spring), không dùng kế thừa. Ví dụ nếu cần, dựng `Location` cho response 201 bằng một helper trong `interfaces/rest`:

```java
final class Locations {
    static URI ofCreated(Object id) {
        return ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri();
    }
}
```

## Lý do

1. **Không có nơi nào dùng controller hay service một cách polymorphic.** Lớp cha `BusinessException` có giá trị vì `RestExceptionHandler` xử lý mọi lỗi nghiệp vụ qua kiểu cha (đọc `category`, `code`). Không có code nào gọi "một `BaseService` bất kỳ", nên lớp cha ở đây chỉ còn là kế thừa để tái dùng code.
2. **Service không cùng hình dạng.** `BaseService<T, ID>` với CRUD chỉ khớp `ProductService`. Các service còn lại sẽ hoặc phải nhận những method vô nghĩa với chúng (ví dụ `delete` một vận đơn, trong khi nghiệp vụ là `cancel`), hoặc phải override để chặn — vi phạm Liskov.
3. **Kế thừa trói chặt.** Java chỉ cho kế thừa một lớp, nên lớp cha chiếm mất chỗ đó. Sửa lớp cha ảnh hưởng mọi lớp con (fragile base class). `@Transactional` và Swagger annotation trên method kế thừa từ lớp cha generic khó đọc và khó mô tả riêng từng endpoint.
4. **Lượng lặp nhỏ.** Chỉ dòng dựng `Location` lặp ở 2–3 controller; kiểm null trong constructor mỗi chỗ chỉ 1–2 dòng với message khác nhau. Không đủ để đổi lấy một tầng trừu tượng.

## Phương án đã cân nhắc

| Phương án | Kết luận |
|---|---|
| `BaseService<T, ID>` generic với CRUD | Loại: chỉ khớp một service, ép các service use case nhận method thừa |
| `BaseController` chứa helper (Location, mapping chung) | Loại: lấy kế thừa để tái dùng vài dòng; helper tĩnh làm được mà không trói class |
| Helper tĩnh / composition (`Locations.ofCreated`) | Chọn khi có nhu cầu; chưa bắt buộc làm ngay |
| Giữ nguyên, không gom gì | Chấp nhận được với lượng lặp hiện tại |

## Hệ quả

- Thêm controller hoặc service mới: viết class độc lập, nhận dependency qua constructor, theo quy ước validate và `@throws` trong `CLAUDE.md`.
- Hành vi chung của REST tiếp tục đặt ở `@RestControllerAdvice`, Bean Validation hoặc helper trong `interfaces/rest`, không đặt ở lớp cha.
- Chấp nhận lặp vài dòng giữa các controller.

## Khi nào xem lại

Khi xuất hiện nhiều resource **CRUD thuần, giống hệt nhau** (ví dụ một loạt bảng danh mục cho trang admin). Lúc đó một `CrudService<T, ID>` generic, hoặc Spring Data REST, mới thật sự bớt việc. Khi đó viết ADR mới thay thế ADR này.
