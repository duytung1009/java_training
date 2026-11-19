# Architecture Decision Records

Mỗi quyết định kiến trúc một file `NNNN-<tên-ngắn>.md`, đánh số tăng dần, không sửa nội dung quyết định đã chấp nhận. Đổi ý thì viết ADR mới và đặt ADR cũ thành "Bị thay thế bởi ADR-xxxx".

Mỗi ADR gồm: Trạng thái, Ngày, Bối cảnh, Quyết định, Lý do, Phương án đã cân nhắc, Hệ quả, Khi nào xem lại.

| ADR | Tiêu đề | Trạng thái |
|---|---|---|
| [0001](0001-khong-dung-base-controller-base-service.md) | Không tạo `BaseController` / `BaseService` dùng chung | Chấp nhận |
| [0002](0002-business-exception-category-code.md) | Gom lỗi nghiệp vụ về `BusinessException` (category + code) | Chấp nhận |
