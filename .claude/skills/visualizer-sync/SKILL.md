---
name: visualizer-sync
description: Cập nhật module visualizer (bản đồ 2.5D, kịch bản, bảng chú thích) sau khi domain/ thay đổi. Dùng khi sửa domain/ (thêm/đổi/xoá class, method, field), khi visualizer hiện banner "chưa đồng bộ với domain", hoặc khi được nhờ "sync visualizer".
---

# Đồng bộ visualizer với domain

Module `visualizer/` vẽ lại `domain/` để làm tài liệu học. Nó chỉ đọc domain, **không bao giờ sửa `domain/` hay `app/`** để hợp với visualizer; chiều phụ thuộc là visualizer → domain.

Tín hiệu lệch: `visualizer/src/main/resources/domain-api.txt` là snapshot chữ ký public API của `com.training.ecommerce.domain.**` ở lần sync cuối. Khi chạy app, `GET /api/viz/sync` so snapshot với domain hiện tại và FE hiện banner cam nếu khác. Plan và quyết định thiết kế: `docs/extras/s07-domain-visualizer.md`.

## 1. Xem domain đổi gì

```bash
./mvnw -q -pl visualizer -am compile
java -cp visualizer/target/classes:domain/target/classes com.training.ecommerce.visualizer.sync.DomainApiScanner "$TMPDIR/domain-api.now.txt"
diff visualizer/src/main/resources/domain-api.txt "$TMPDIR/domain-api.now.txt"
```

Đọc thêm `git diff` / `git log` trên `domain/` để hiểu *hành vi* đổi thế nào, không chỉ chữ ký. Nếu compile visualizer lỗi vì method bị đổi tên/xoá, đó cũng là danh sách chỗ phải sửa.

## 2. Map thay đổi sang chỗ cần sửa

| Domain đổi | Sửa ở visualizer |
|---|---|
| Method bị đổi tên/xoá, đổi tham số | Các `scenario/*Scenario.java` gọi nó; nhãn `recorder.call/run("Class", "method", ...)` phải khớp tên thật |
| Trạng thái mới cần thấy (field, collection, stock…) | `world/WorldSnapshotter` (panel item + object diff; `ItemView.type` = class mà ô đại diện, hiện thành nhãn nhỏ trên ô — ghi đúng kiểu trong code, vd. `Map.Entry` nếu domain chỉ giữ id), có thể cả `world/World`, `WorldFactory` (seed) |
| Hành vi mới đáng demo (use case, exception, concurrency) | Thêm `scenario/<Tên>Scenario.java`, đăng ký trong `ScenarioRegistry`, thêm test bất biến trong `ScenarioTest` |
| Domain object mới cần một khối trên bản đồ | `static/js/map/layout.js`: `PANELS` (toạ độ ô lưới), `WIRES` nếu có flow; panel id khớp `WorldSnapshotter`; màu ở `styles.css` + legend `index.html` |
| Mô tả, API chính, buổi học đổi | `static/js/panels/info.js` (bảng chú thích khi click khối) — mô tả phải đúng với code domain, đối chiếu lại |
| Flow mới giữa hai khối | `TraceEvent.flows` (danh sách `{from,to}`) trong kịch bản + một wire trong `layout.js` (FE tự đảo chiều) |

Chỉ thay đổi nội bộ (private, refactor không đổi hành vi) thì thường chỉ cần cập nhật snapshot.

## 3. Quy tắc bố cục

- Wire chỉ gồm đoạn ngang/dọc trên lưới, không cắt xuyên panel. `checkLayout()` trong `layout.js` in cảnh báo `Visualizer layout:` ở console khi vi phạm — phải sạch.
- Panel có dải tiêu đề `HEADER` ở đầu; ô con bắt đầu từ `y + HEADER`. Tiêu đề, dòng phụ phải nằm gọn trong bề ngang panel.
- Giữ viewBox vừa bản đồ; mở rộng lưới nền trong `scene.js` nếu thêm hàng/cột.

## 4. Kiểm tra

1. `./mvnw -pl visualizer -am test` xanh; `git diff main -- domain/ app/` rỗng (trừ khi chính thay đổi domain là việc của nhánh).
2. Chạy app (`./mvnw -pl visualizer -am spring-boot:run`, hoặc preview `visualizer` trong `.claude/launch.json`), mở http://localhost:8081:
   - chạy **mọi** kịch bản: số dòng log = số event, không lỗi console, không cảnh báo `chưa có đường nối` / `Visualizer layout:`;
   - click các khối bị ảnh hưởng, kiểm bảng chú thích.
3. Khi mọi thứ đã khớp, cập nhật snapshot rồi **khởi động lại app** (kết quả so sánh được tính một lần lúc chạy và cache) và tải lại trang — banner phải biến mất:

```bash
java -cp visualizer/target/classes:domain/target/classes com.training.ecommerce.visualizer.sync.DomainApiScanner visualizer/src/main/resources/domain-api.txt
```

Không cập nhật snapshot chỉ để tắt banner khi visualizer chưa thật sự phản ánh thay đổi. Nếu cố ý chưa vẽ một phần domain, ghi lý do vào plan doc.

## 5. Ghi lại

Cập nhật bảng kịch bản / phase trong `docs/extras/s07-domain-visualizer.md` nếu thêm kịch bản hoặc khối. Commit tiền tố `[S07-extra]` (hoặc tiền tố buổi đang làm), chỉ khi người dùng yêu cầu commit.
