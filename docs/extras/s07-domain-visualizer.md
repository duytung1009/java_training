# S07 extra – Domain Visualizer (bản đồ 2.5D + log + state diff)

> Tài liệu bổ sung, **không** thuộc acceptance S07. Claude triển khai tự động theo plan này; Tùng chỉ review công năng ở các Gate.
> **Trạng thái:** Gate 1 đã duyệt (29/10) với design ở `docs/extras/mockups/iso-map.html`. P1–P3 xong, chờ Gate 2.

## 1. Mục tiêu

Một trang web chạy local để **nhìn thấy** module `domain/` hoạt động:

- **Trái – Bản đồ 2.5D (isometric):** mỗi domain object là một panel phẳng; product và các phần tử con là ô vuông có số; đường nối chạy theo hướng data khi có action.
- **Phải trên – Call log:** từng method được gọi theo thứ tự thật: thời gian, thread, `Class.method(args)`, kết quả hoặc exception.
- **Phải dưới – State changes:** object nào đổi, field nào đổi, `before → after`. Gọi thất bại (vd. `OutOfStockException`) thì hiện "không đổi" để thấy rõ all-or-nothing.

Học viên chọn một **kịch bản** (gắn với buổi S02–S06), bấm Run / Step, theo dõi ba panel đồng bộ.

## 2. Quyết định đã chốt

| # | Quyết định | Lý do |
|---|---|---|
| D1 | Module Maven **mới `visualizer/`** (Spring Boot, port **8081**), chỉ phụ thuộc `ecommerce-domain` | Không đụng `app/`; `domain/` vẫn Java thuần |
| D2 | **Không sửa `domain/`.** Ghi log bằng wrapper tường minh `TraceRecorder.call(...)`: chụp snapshot trước, gọi method domain, chụp snapshot sau | Domain object không phải Spring bean nên AOP không bắt được; wrapper tường minh dễ đọc, chính nó là tài liệu học |
| D3 | Snapshot do **`WorldSnapshotter` viết tay** cho từng loại object, không dùng reflection | Kiểm soát field hiện ra; không mở field private |
| D4 | **Mỗi lượt chạy dựng thế giới mới từ dữ liệu mẫu**, API trả cả lượt (`worldBefore` + danh sách event, mỗi event kèm `worldAfter`). FE tự phát lại Play / Pause / Step; Reset = về `worldBefore`. Không SSE / WebSocket, không endpoint reset | Kết quả tất định, dễ test, không có state dùng chung giữa các request |
| D5 | FE là **file tĩnh** trong `visualizer/src/main/resources/static/`: HTML + CSS + JS (ES modules) thuần, SVG isometric. Không npm / build tool | `spring-boot:run` là đủ |
| D6 | Bản đồ **vẽ đúng domain hiện có**; đường `WH → hub` là wire nét liền của `InventoryManager.replenish` | Ban đầu `Warehouse` và tồn kho `InventoryManager` là hai model rời (đường nét chấm "chưa có"); nhánh warehouse-hub đã nối chúng: hub = hàng sẵn bán, kho = hàng dự trữ |
| D7 | **Bố cục bản đồ thuộc FE.** BE chỉ trả panel id + item + `flows` (danh sách `{from, to}` theo thứ tự phát); FE tự map cặp `(from, to)` sang đường và hướng. Đường chỉ gồm đoạn ngang/dọc; `checkLayout()` trong `layout.js` cảnh báo ở console khi đường cắt qua panel hoặc panel chồng nhau | BE không biết gì về toạ độ |
| D8 | Nhánh `extra/domain-visualizer` từ `main` (đã có S07), commit tiền tố `[S07-extra]` | — |

## 3. Kiến trúc

```
visualizer/  (artifact ecommerce-visualizer, phụ thuộc ecommerce-domain)
└── com.training.ecommerce.visualizer
    ├── VisualizerApplication
    ├── trace/        TraceRecorder, TraceEvent, ObjectChange, Flow, Outcome
    ├── world/        World (2 warehouse, InventoryManager, Cart, orders, pricing, shipping), WorldFactory, WorldSnapshotter, PanelView, ItemView
    ├── scenario/     Scenario (interface), ScenarioRegistry, 9 kịch bản (mục 5)
    └── web/          VisualizerController, RestExceptionHandler, DTO record
resources/static/
    index.html, styles.css
    js/ api.js, player.js (state + playback), map/iso.js, map/layout.js, map/scene.js, panels/log.js, panels/diff.js, controls.js
    fixtures/sample-run.json
```

Luật phụ thuộc: `web → scenario → trace/world → domain`. Không import `app`.

### 3.1 Cơ chế trace

```java
// Ý tưởng, không phải code chốt
public <T> T call(String className, String method, List<Object> args, Flow flow, List<String> panels, Supplier<T> action) {  // flow nullable; có thêm callFlows/runFlows nhận List<Flow>
    var before = snapshotter.snapshot(world);           // trước khi gọi
    try {
        T result = action.get();                        // gọi method domain thật
        emit(..., OK, result, null, diff(before, snapshotter.snapshot(world)));
        return result;
    } catch (RuntimeException e) {
        emit(..., THROWN, null, e, diff(before, snapshotter.snapshot(world)));  // thường rỗng → all-or-nothing
        throw e;
    }
}
```

Kịch bản đa luồng: snapshot chụp ngoài lock của domain, nên `changes` của một event có thể gồm thay đổi của thread khác chạy xen. Chấp nhận và ghi chú trên UI (`RunResult.concurrent = true`); test kiểm invariant (tổng hàng không đổi, đúng 1 OK) thay vì từng diff.

## 4. Hợp đồng dữ liệu

### 4.1 REST

| Method | Path | Trả về |
|---|---|---|
| GET | `/api/viz/world` | `WorldView` của dữ liệu mẫu |
| GET | `/api/viz/scenarios` | `[{id, title, session, description}]` |
| POST | `/api/viz/scenarios/{id}/run` | `RunResult`; id sai → 404 `ProblemDetail` |

### 4.2 Kiểu dữ liệu

```jsonc
// WorldView: danh sách panel theo thứ tự cố định
{ "panels": [ { "id": "warehouse:WH-HN", "type": "warehouse", "title": "Warehouse WH-HN",
                "items": [ { "key": "LAPTOP", "label": "LAP", "value": 10 } ] } ] }
// value: number (FE đếm dần khi đổi) hoặc string (vd. "33.0tr", "-10%")

// RunResult
{ "scenarioId": "transfer", "title": "...", "concurrent": false,
  "worldBefore": { /* WorldView */ }, "events": [ /* TraceEvent */ ] }

// TraceEvent
{ "seq": 1, "at": "2026-11-01T10:00:00.123Z", "thread": "main",
  "className": "WarehouseTransferService", "method": "transfer",
  "args": ["WH-HN", "WH-HCM", "LAPTOP", "3"],
  "outcome": "OK",                     // OK | THROWN
  "result": null,                      // string hoặc null
  "exception": null,                   // {"type": "OutOfStockException", "message": "..."} khi THROWN
  "flows": [ { "from": "warehouse:WH-HN", "to": "warehouse:WH-HCM" } ],   // theo thứ tự phát, [] nếu không có
  "panels": ["warehouse:WH-HN", "warehouse:WH-HCM"],                 // panel cần nháy viền
  "changes": [ { "ref": "Warehouse WH-HN", "before": {"LAPTOP": 10}, "after": {"LAPTOP": 7} } ],
  "worldAfter": { /* WorldView */ } }
```

- `changes[].before = null` → object mới tạo; `after = null` → object bị xoá; key chỉ có ở một phía → field thêm / xoá.
- `changes` chỉ chứa field **có đổi**.
- FE dùng `worldAfter` để đồng bộ ô (pop-in / pop-out / đếm số); dùng `changes` cho panel diff.

### 4.3 Node và cặp flow

`WorldView` là cây node `{id, kind, cls, title, sub, parent, items[]}`; `kind`: `object` (một instance thật), `collection` (danh sách chứa object), `area` (đúng một package domain, không có class). Ô `computed=true` là kết quả hàm thuần để hiển thị, không phải state.

Mỗi package domain là một `area` gốc; object nằm trong vùng của package chứa class đó.

| Vùng (area gốc) | Con | Ô |
|---|---|---|
| `inventory` · `domain.inventory` | `warehouse:WH-HN`, `warehouse:WH-HCM` (object `Warehouse`), `hub` (object `InventoryManager`) | `Map.Entry` productId → qty; `Product` → stock |
| `order` · `domain.order` | `cart` (object `Cart`), `orders` (collection `List<Order>` do `World` của visualizer giữ) → `order:<id>` (object `Order`) | `Product` → qty; `OrderLine` sản phẩm × qty → đơn giá snapshot, dòng phụ: giảm giá, thuế, thành tiền |
| `catalog` · `domain.catalog` | `catalog:product:*` (chỉ khi kịch bản tạo) | basePrice, `= finalPrice` |
| `pricing` · `domain.pricing` | `pricing:rule:*`, `pricing:code:*` (chỉ khi kịch bản tạo) | `= apply(...)`; code, rate |
| `shipping` · `domain.shipping` | `zone:INNER_CITY`, `zone:OTHER_CITY` (enum) | baseFee + phí theo thành phố (computed) |

Container (vùng hoặc collection có `child`) tự giãn theo số khối con, chỉ từ trên xuống, tối đa 4 hàng rồi mới hiện "+N"; vùng giãn đẩy các vùng bên dưới (chồng theo x) xuống (`computeLayout()` trong `layout.js`). Nút "?" trên header mở màn tóm tắt hành trình phát triển (`js/panels/about.js`).

Wire nối object/collection (chiều ngược tự đảo): `warehouse:WH-HN ↔ warehouse:WH-HCM`, `warehouse:WH-HN → hub`, `warehouse:WH-HCM → hub` (replenish), `hub → cart`, `cart → orders`, `orders → hub`, `hub → shipping`. Flow tới khối con đi về tổ tiên gần nhất có wire. `toOrder` không dùng `domain.pricing` (tỉ lệ do người gọi truyền, đơn giá lấy từ `Product.calculateFinalPrice` trong `OrderLine`); phí ship do `PhysicalProduct.calculateShipping` tính, `Order` không liên quan `ShippingZone`.

## 5. Kịch bản

| id | Buổi | Nội dung | Điểm cần thấy trên UI |
|---|---|---|---|
| `cart-to-order` | S02/S04 | `Cart.add` ×3, `changeQuantity`, `remove`, `toOrder(discount, tax)`, `decreaseStock(order)` | Ô cart pop-in / đổi số / pop-out; toOrder tạo khối `Order` con trong `orders`; kho chỉ giảm ở bước `decreaseStock` (Cart không trừ kho) |
| `pricing-types` | S03 | `calculateFinalPrice` cho Physical / Digital / Subscription / Bundle; `PricingRule.andThen`; `DiscountCode` | Ô giá từng loại hiện ở trạm giá (đa hình) |
| `decrease-stock-ok` | S04 | `InventoryManager.decreaseStock(order)` có 2 dòng cùng một sản phẩm | Cộng dồn theo product, trừ đúng một lần |
| `decrease-stock-fail` | S04 | Đơn có một dòng thiếu hàng | `THROWN OutOfStockException`, `changes` rỗng |
| `transfer` | S06 | `transfer` thành công, sang kho chưa có loại hàng (pop-in), và thất bại | Line chạy giữa hai kho; lần thất bại không kho nào đổi |
| `transfer-deadlock-safe` | S06 | 2 thread chuyển A→B và B→A song song (`ExecutorService` kịch bản tự tạo và đóng) | Log xen kẽ hai thread; tổng hàng không đổi |
| `last-item-race` | S06 | N thread cùng `decreaseStock` sản phẩm còn 1 cái | Đúng một `OK`; stock không âm |
| `shipping-fee` | S02/S03 | `ShippingZone.of(address)` + `PhysicalProduct.calculateShipping(address)`: LAPTOP → Hà Nội, PHONE → Đà Nẵng | Flow hub → zone; ô phí pop-in ở đúng vùng |
| `replenish` | S07-extra | `InventoryManager.replenish` HN→hub LAPTOP ×3, HCM→hub PHONE ×2; thất bại: HEADPHONE (hub không bán, `ProductNotFoundException`), BOOK (kho không có, `OutOfStockException`) | Line chạy kho → hub; kho giảm đúng bằng hub tăng; ca lỗi `changes` rỗng |

## 6. Thiết kế UI (đã duyệt Gate 1)

Theo mockup `docs/extras/mockups/iso-map.html`:

- Nền tối, font JetBrains Mono, panel viền mảnh nhiều màu, legend ô vuông.
- **Panel domain:** hình thoi phẳng trên lưới isometric, viền màu theo loại, ruột trong suốt; tên + kiểu dữ liệu ở góc trên.
- **Ô con:** hình thoi phẳng có nhãn + giá trị. Số đổi → đếm dần + viền nháy xanh (tăng) / cam (giảm). Ô mới → pop-in; ô mất → pop-out.
- **Đường nối:** một màu xám, đứng yên; chỉ khi event có `flows` thì lần lượt từng flow chạy nét đứt có mũi tên theo hướng data; exception → màu đỏ + panel nháy đỏ.
- Log, diff và bản đồ dùng chung **một con trỏ bước**; bấm dòng log → chọn bước đó.
- Header: chọn kịch bản, Run, Pause, Step, tốc độ, Reset. Màn hình < 1100px: xếp dọc.

## 7. Giai đoạn triển khai

| Phase | Slice (file sở hữu) | Nội dung | Kiểm tự động |
|---|---|---|---|
| **P0** ✅ | `docs/extras/` | Chốt hợp đồng (mục 4) + `fixtures/sample-run.json` + mockup | Gate 1 duyệt |
| **P1** ✅ | `pom.xml` (thêm module), `visualizer/pom.xml`, `visualizer/src/**/java` | Scaffold, `trace/`, `world/`, 8 kịch bản, REST | `./mvnw -pl visualizer -am test` |
| **P2** ✅ | `visualizer/src/main/resources/static/**` | FE thật từ mockup, chạy được với fixture (`?fixture=1`) | Browser: không lỗi console, đủ 3 panel |
| **P3** ✅ | main session | Nối FE với API thật, chạy end-to-end 8 kịch bản | `./mvnw test` toàn repo xanh; chạy từng kịch bản trên browser |
| **Gate 2** | Tùng | Review công năng theo mục 8 | — |
| **P4** | `docs/extras/`, README module | Hướng dẫn, bảng kịch bản ↔ keyword, PR | — |
| **Gate 3** | Tùng | Duyệt PR, merge | — |

Ràng buộc chung: Java 25; `domain/` không đổi; service nhận dependency qua constructor; validate tham số và ném `IllegalArgumentException`; comment và text UI bằng tiếng Việt; không thêm thư viện JS.

## 8. Checklist review công năng (Gate 2)

- [ ] `./mvnw -pl visualizer -am spring-boot:run` → `http://localhost:8081`, không cần DB hay Docker
- [ ] Chọn từng kịch bản → Run: line chạy đúng hướng, số trong ô đổi đúng, ô mới pop-in / ô mất pop-out
- [ ] Step từng bước: log, diff và bản đồ luôn chỉ cùng một bước
- [ ] `decrease-stock-fail` và transfer thất bại: log có dòng đỏ, diff ghi "không đổi"
- [ ] `transfer-deadlock-safe`: log có 2 tên thread, không treo, tổng hàng giữ nguyên
- [ ] `last-item-race`: đúng 1 thành công, stock không âm
- [ ] Reset đưa bản đồ về dữ liệu mẫu
- [ ] `./mvnw test` toàn repo vẫn xanh; `git diff main -- domain/` rỗng

## 8b. Việc để sau

- **Ghi nhận đúng từng bước ở kịch bản song song** (`transfer-deadlock-safe`, `last-item-race`). Hiện `TraceRecorder` chụp before → action → after ngoài mọi lock, nên thread khác chen giữa làm dòng log OK mà ô không đổi; snapshot đọc từng kho riêng nên có thể "rách" (tổng PHONE = 9). Domain đúng, lỗi ở cách ghi.
  Hướng đã chọn (chưa làm): ByteBuddy `Advice` inline vào các method mutator của domain (bảng cấu hình class/method), chạy trong lock của domain → chụp riêng object vừa đổi, cấp seq toàn cục, ghi journal; `TraceRecorder` gom thay đổi theo ngữ cảnh thread-local của lần gọi; `worldAfter` dựng lại từ journal. Cần `byte-buddy` + `byte-buddy-agent` trong `visualizer/pom.xml`, `-javaagent` cho `spring-boot:run`/surefire (IntelliJ thêm VM option, không thì tự attach kèm cảnh báo JDK 25); kiểm tra agent trên JDK 25 trước tiên. Skill `visualizer-sync` thêm bước khai báo mutator cho domain mới.

## 9. Ngoài phạm vi

Trace `app/`, streaming realtime, chỉnh dữ liệu mẫu trên UI, lưu lịch sử lượt chạy, WebGL, test tự động cho JS.
