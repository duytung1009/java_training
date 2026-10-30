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
java -cp visualizer/target/classes:domain/target/classes com.training.ecommerce.visualizer.ecommerce.sync.EcommerceDomainApi "$TMPDIR/domain-api.now.txt"
diff visualizer/src/main/resources/domain-api.txt "$TMPDIR/domain-api.now.txt"
```

Đọc thêm `git diff` / `git log` trên `domain/` để hiểu *hành vi* đổi thế nào, không chỉ chữ ký. Nếu compile visualizer lỗi vì method bị đổi tên/xoá, đó cũng là danh sách chỗ phải sửa.

## 2. Map thay đổi sang chỗ cần sửa

| Domain đổi | Sửa ở visualizer |
|---|---|
| Method bị đổi tên/xoá, đổi tham số | Các `ecommerce/scenario/*Scenario.java` gọi nó; nhãn `recorder.call/run("Class", "method", ...)` phải khớp tên thật |
| Trạng thái mới cần thấy (field, collection, stock…) | `ecommerce/world/WorldSnapshotter` (node + item + object diff; `ItemView.type` = class mà ô đại diện, hiện thành nhãn nhỏ trên ô — ghi đúng kiểu trong code, vd. `Map.Entry` nếu domain chỉ giữ id), có thể cả `ecommerce/world/World`, `WorldFactory` (seed) |
| Hành vi mới đáng demo (use case, exception, concurrency) | Thêm `ecommerce/scenario/<Tên>Scenario.java`, đăng ký trong `EcommerceVisualizerAdapter.scenarios()`, thêm test bất biến trong `ecommerce/scenario/ScenarioTest` |
| Domain object mới cần một khối trên bản đồ | Chọn dạng khối: `object` (một instance thật), `collection` (danh sách chứa object), `area` (package/khái niệm, không có class). Ô là kết quả hàm thuần thì `computed=true`, không ghi như state. `area` = đúng một package domain (mỗi package một vùng gốc trong `ROOTS` của `static/adapter/layout.js`). Object cố định (luôn có) đặt toạ độ trong `PLACED`, nằm trọn trong vùng của nó; object sinh lúc chạy chỉ cần `parent` trong `WorldSnapshotter`, tự xếp lưới theo `child` spec của vùng/collection. `WIRES` nối giữa object/collection (được cắt qua khung vùng, không được cắt qua khối); màu ở `static/adapter/theme.css` + `legend` trong `static/adapter/index.js` |
| Mô tả, API chính, buổi học đổi | `static/adapter/info.js` (bảng chú thích khi click khối, tra theo `cls`) — mô tả phải đúng với code domain, đối chiếu lại |
| Flow mới giữa hai khối | `TraceEvent.flows` (danh sách `{from,to}`) trong kịch bản + một wire trong `static/adapter/layout.js` (FE tự đảo chiều) |

Chỉ thay đổi nội bộ (private, refactor không đổi hành vi) thì thường chỉ cần cập nhật snapshot.

Mọi thứ riêng của ecommerce nằm ở adapter: Java trong `visualizer.ecommerce.**`, FE trong `static/adapter/**`. Không đưa class domain hay id ecommerce vào `visualizer.core.**` hoặc `static/js/**`; `core/ArchitectureTest` sẽ fail nếu core import domain.

## 3. Quy tắc bố cục

- Wire nối giữa object/collection đặt trong `PLACED` (hoặc vùng chỉ có khối con, vd. shipping); flow tới khối con tự đi về tổ tiên gần nhất có wire, gồm đoạn ngang/dọc trên lưới, không cắt xuyên khối. `checkLayout()` trong `layout.js` in cảnh báo `Visualizer layout:` ở console khi vi phạm — phải sạch.
- Khối có dải tiêu đề (`HEADER` cho gốc, `CHILD_HEADER` cho khối con); ô bắt đầu dưới dải này. Chữ tự co/cắt cho vừa bề ngang khối (`fit` trong `scene.js`) — vẫn nên giữ tiêu đề ngắn.
- Container (vùng hoặc collection có `child`) tự giãn theo số khối con, chỉ một chiều từ trên xuống, tối đa `MAX_ROWS` hàng rồi mới hiện "+N"; vùng bị giãn đẩy các vùng bên dưới (chồng theo x) xuống. `computeLayout()` trong `layout.js` tính bố cục hiệu lực; wire gắn vào khối có thể bị đẩy sẽ bị `checkLayout()` báo nếu gãy — tránh nối wire vào vùng nằm dưới container động.
- ViewBox và lưới nền tự tính theo bố cục hiệu lực.
- Thêm chặng mới vào màn "?" (`static/adapter/about.js`) khi visualizer có thay đổi đáng kể.

## 4. Kiểm tra

1. `./mvnw -pl visualizer -am test` xanh; `git diff main -- domain/ app/` rỗng (trừ khi chính thay đổi domain là việc của nhánh).
2. Chạy app (`./mvnw -pl visualizer -am spring-boot:run`, hoặc preview `visualizer` trong `.claude/launch.json`), mở http://localhost:8081:
   - chạy **mọi** kịch bản: số dòng log = số event, không lỗi console, không cảnh báo `chưa có đường nối` / `Visualizer layout:`;
   - click các khối bị ảnh hưởng, kiểm bảng chú thích.
3. Khi mọi thứ đã khớp, cập nhật snapshot rồi **khởi động lại app** (kết quả so sánh được tính một lần lúc chạy và cache) và tải lại trang — banner phải biến mất:

```bash
java -cp visualizer/target/classes:domain/target/classes com.training.ecommerce.visualizer.ecommerce.sync.EcommerceDomainApi visualizer/src/main/resources/domain-api.txt
```

Không cập nhật snapshot chỉ để tắt banner khi visualizer chưa thật sự phản ánh thay đổi. Nếu cố ý chưa vẽ một phần domain, ghi lý do vào plan doc.

## 5. Ghi lại

Cập nhật bảng kịch bản / phase trong `docs/extras/s07-domain-visualizer.md` nếu thêm kịch bản hoặc khối. Commit tiền tố `[S07-extra]` (hoặc tiền tố buổi đang làm), chỉ khi người dùng yêu cầu commit.
