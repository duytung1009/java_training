# S07 extra — Visualizer tổng quát: core/adapter, flow từ domain event, xem theo tầng

Nhánh: `extra/visualizer-generic` → PR vào `main`. Commit tiền tố `[S07-extra]`, luồng TDD: spec test đỏ trước rồi implementation.

Plan gốc của visualizer: `docs/extras/s07-domain-visualizer.md` (sẽ cập nhật D2, D7, §3, §4.2 theo plan này).

## 1. Mục tiêu

1. Tách code visualizer thành package `visualizer.core` (không biết domain nào) và `visualizer.ecommerce` (adapter cho domain hiện tại), trong cùng module Maven. Đây là bước đệm để sau này tách `core` thành module riêng.
2. Thêm **cơ chế** biến domain event thành flow trên bản đồ. Chỉ làm ở core để dùng cho domain sau này; **không sửa `domain/` ecommerce** (D2 giữ nguyên), adapter ecommerce chưa có event source.
3. Xem bản đồ theo tầng **context map → aggregate → instance**, kèm bộ lọc. Các tầng do user dựng trên giao diện từ gợi ý reflection, lưu thành file JSON.

Không làm: tách Maven module, tự khám phá cấu trúc bằng reflection/annotation, Java agent, thư viện auto-layout.

## 2. Quyết định

| # | Quyết định | Lý do |
|---|---|---|
| G1 | Hai package trong module `visualizer`: `com.training.ecommerce.visualizer.core.**` và `...visualizer.ecommerce.**`. `VisualizerApplication` ở package gốc `visualizer` để component scan thấy cả hai | Bước đệm rẻ; tách module sau chỉ là di chuyển thư mục |
| G2 | Ranh giới được kiểm tra bằng test: `core..` không import `com.training.ecommerce.domain..` và `visualizer.ecommerce..`. Dùng ArchUnit 1.5.1 (test scope, chỉ trong `visualizer/pom.xml`; đọc được class Java 25) | Compiler chưa chặn được khi còn chung module, test thay vai enforcer |
| G3 | SPI cắm qua Spring bean: adapter khai báo một bean `VisualizerAdapter<W>`; core nhận qua constructor | Giống quy tắc port/adapter của `app`; không cần `ServiceLoader` |
| G4 | `Scenario<W>` thành generic theo kiểu world của adapter; `TraceRecorder` nhận `Supplier<WorldSnapshot>` thay vì `World` + `WorldSnapshotter` | Bỏ phụ thuộc của trace vào world ecommerce |
| G5 | Domain event: core có SPI `DomainEventSource<W>` (rút event sau mỗi lời gọi, hợp mẫu aggregate tự ghi event `pullDomainEvents()`) và `DomainEventMapper` (event → node nguồn/đích + nhãn). Thêm `TraceRecorder.emit(Object)` cho domain dùng publisher/listener | Hỗ trợ cả hai mẫu phổ biến mà không ép domain theo một kiểu |
| G6 | Event gắn vào `TraceEvent` đã sinh ra nó (`events`), flow của event phát **sau** flow lời gọi, kiểu `event` (FE vẽ khác) | Giữ thứ tự nhân quả: lời gọi → event → phản ứng |
| G7 | Mô hình domain cho drill-down (`DomainModel`: context, aggregate/value, quan hệ) do **user dựng trên giao diện** từ gợi ý của core: reflection (package, định danh, sở hữu, kiểu field/tham số) cộng quan hệ quan sát được từ flow của các kịch bản. Node instance map vào aggregate theo `NodeView.cls`. Adapter không khai báo mô hình | Dự án mới xem được ngay bằng gợi ý; adapter chỉ còn world, snapshot, kịch bản |
| G11 | Mô hình lưu thành **file JSON trong repo** (`visualizer/src/main/resources/domain-model.json`), BE ghi qua `PUT /api/viz/model`; bản nháp chưa lưu để trong `localStorage`. Chưa có file thì dùng thẳng gợi ý | Review được, đi theo git, mọi máy giống nhau |
| G8 | Sửa D7: FE core (`static/js/**`, giữ nguyên tên file) không chứa id/toạ độ ecommerce. Dữ liệu riêng của adapter ở `static/adapter/` theo một contract cố định: `adapter/index.js` export `adapter = { layout: {ROOTS, PLACED, WIRES, PREFIXES}, infoOf(node), about, legend, tileColor(rootId, key) }`, biến màu domain ở `adapter/theme.css`; core chỉ import file contract đó | Sau này adapter là jar khác vẫn phục vụ được cùng đường dẫn |
| G9 | Tầng context và aggregate xếp lưới tự động đơn giản (theo thứ tự trong `domain-model.json`); tầng instance dùng layout tay của adapter, adapter không có layout thì rơi về lưới tự động | Không thêm thư viện; ecommerce giữ nguyên bản đồ đẹp hiện có |
| G10 | Logic FE thuần (resolve flow theo tầng, lọc, xếp lưới) tách thành hàm không đụng DOM, test bằng `node --test` (Node 22 có sẵn, không cần `package.json`) | Hiện FE không có test nào |

## 3. Cấu trúc đích

```
visualizer/src/main/java/com/training/ecommerce/visualizer/
  VisualizerApplication
  core/
    spi/      VisualizerAdapter<W>, DomainEventSource<W>, DomainEventMapper, EventFlow
    model/    DomainModel (+ ContextDef, ClassDef, RelationDef), DomainModelSuggester,
              ObservedRelations, DomainModelStore, DomainModelValidator
    trace/    TraceRecorder, TraceEvent, Flow (+ kind, label), ObjectChange, Outcome, ExceptionInfo,
              DomainEventView
    view/     WorldSnapshot, WorldView, NodeView, ItemView
    scenario/ Scenario<W>, ScenarioService, RunResult, ParallelRunner, ScenarioNotFoundException
    web/      VisualizerController, DomainModelController (/api/viz/model…), RestExceptionHandler,
              ScenarioSummary
    sync/     DomainApiScanner (package + class mốc truyền vào), DomainSyncService, SyncStatus
  ecommerce/
    EcommerceVisualizerAdapter (bean VisualizerAdapter<World>)
    world/    World, WorldFactory, WorldSnapshotter, MoneyFormat
    scenario/ *Scenario, OrderSteps, ShipmentSteps, ScenarioRegistry, ScenarioConfig

visualizer/src/main/resources/static/
  index.html, styles.css            (generic; legend lấy từ adapter)
  js/...                            (lõi FE, giữ tên file cũ) main, api, player, controls, dom, map/{scene, iso, layout},
                                    panels/{log, diff, sync, info-shell, about-shell, model-editor},
                                    view/{levels, filters}, model/{draft, drift}
  adapter/index.js                  contract: { layout: {ROOTS, PLACED, WIRES}, info, about, theme, colors }
  adapter/...                       dữ liệu ecommerce chuyển từ layout.js, info.js, about.js, scene.js, styles.css
visualizer/src/test/js/*.test.mjs   test hàm thuần FE
```

## 4. Các phần việc

Làm tuần tự P1 → P2 → P3 (P2 và P3 cùng sửa `scene.js`, `player.js`, `log.js`, `TraceEvent`). Mỗi phần một lượt implementer, review diff xong mới sang phần sau.

### P1 — Tách core / ecommerce (refactor, không đổi hành vi)

1. **Spec test (đỏ):** `core/ArchitectureTest` (ArchUnit) với hai luật ở G2.
2. Di chuyển class theo §3. Phần lẫn lộn tách như sau:
   - `World`: phần registry chung (`RegisteredNode`, `registerObject`, `putComputed`) — giữ ở adapter trong P1, chỉ tách ra core nếu adapter khác cần (chưa cần).
   - `WorldSnapshotter`: ở lại adapter; core chỉ nhận `WorldSnapshot` qua `VisualizerAdapter.snapshot(W)`.
   - `TraceRecorder` → nhận `Supplier<WorldSnapshot>` (G4).
   - `ScenarioService` → dùng `VisualizerAdapter<W>`: `createWorld()`, `snapshot(W)`, `scenarios()`.
   - `DomainApiScanner`: tham số hoá package domain và class mốc (adapter cung cấp `Product.class`, `"com.training.ecommerce.domain"`); `DomainSyncService` lấy tên resource từ adapter.
   - Test di chuyển theo class; `TraceRecorderTest` đổi sang domain giả trong test core; `DomainApiScannerTest` tách phần chung (domain giả) và phần ecommerce.
3. FE: chuyển dữ liệu ecommerce sang `static/adapter/` theo G8 (`ROOTS`/`PLACED`/`WIRES`, màu sản phẩm và `tileColor` trong `scene.js`, mô tả trong `info.js`, `about.js`, legend trong `index.html`, biến màu `--wh/--hub/...` trong `styles.css`). Core JS chỉ import `adapter/index.js`.
4. Cập nhật đường dẫn/FQN: skill `.claude/skills/visualizer-sync/SKILL.md`, header `domain-api.txt`, lệnh chạy scanner, `CLAUDE.md` (dòng visualizer), §3 của plan gốc.

**Xong khi:** ArchitectureTest xanh; toàn bộ test visualizer cũ xanh; bản đồ, kịch bản, fixture `?fixture=1` chạy y hệt trước (so sánh ảnh chụp trước/sau); `domain-api.txt` không đổi nội dung chữ ký.

### P2 — Cơ chế flow từ domain event (chỉ core)

1. **Spec test (đỏ)** trong `core/trace` với một domain giả ở `src/test` (vd. `FakeAccount` tự ghi event `MoneyDeposited`, `FakeLedger` lắng nghe):
   - Lời gọi sinh 2 event → `TraceEvent.events` có 2 phần tử đúng thứ tự, flow kiểu `event` nằm sau flow lời gọi.
   - Không có `DomainEventSource` → `events` rỗng, JSON không đổi với kịch bản ecommerce (test hồi quy).
   - Event không có mapper → vẫn ghi vào log (`type`, payload), không sinh flow.
   - Lời gọi ném exception → event đã ghi trước khi ném vẫn được rút và hiển thị (ghi rõ trong Javadoc).
   - `emit()` gọi từ listener bên trong một lời gọi → gắn vào lời gọi đang chạy của **thread đó** (ThreadLocal), không lẫn sang thread khác trong kịch bản đa luồng.
   - Phản ứng của listener được ghi thành `TraceEvent` riêng có `causedBy` = id event.
2. Implementation: các type ở §3 (`DomainEventSource`, `DomainEventMapper`, `EventFlow`, `DomainEventView{id, type, context, payload, from, to}`), `Flow` thêm `kind` (`call`|`event`) và `label`, `TraceEvent` thêm `events` và `causedBy`. Adapter ecommerce trả source rỗng.
3. FE: log hiện event thành dòng con dưới lời gọi; flow `event` vẽ nét chấm, màu riêng, nhãn tên event; diff panel không đổi. Thêm fixture `static/fixtures/sample-events.json` (sinh từ test domain giả) để xem trên trình duyệt qua `?fixture=events`.

**Đã làm (P2):** API ghi event của `TraceRecorder`: `emit(event)` (ThreadLocal theo thread, ném `IllegalStateException` ngoài lời gọi), `lastEvents()` (event gốc của lời gọi cấp ngoài cùng vừa xong trên thread này) và `react(event, className, method, args, flows, panels, action)` (ghi `TraceEvent` riêng có `causedBy` = id event; `react` không ghi đè `lastEvents`). Adapter muốn có event thì override `eventSource()` / `eventMapper()`; ecommerce dùng nguồn rỗng. Fixture `sample-events.json` do `ecommerce/trace/SampleEventsFixture` (test của **ecommerce**) sinh, không phải test lõi, vì bản đồ vẽ theo bố cục adapter ecommerce nên domain giả thuần không có chỗ để vẽ; test `SampleEventsFixtureTest` chặn file lệch khi sinh lại. `sample-run.json` là dữ liệu tay, chỉ thêm trường mặc định (`kind: CALL`, `label: null`, `events: []`, `causedBy: null`).

**Xong khi:** test P2 xanh; JSON kịch bản ecommerce không có trường mới khác rỗng/mặc định; fixture events phát đúng thứ tự lời gọi → event → phản ứng trên trình duyệt.

### P3 — Mô hình domain dựng trên giao diện, xem theo tầng, bộ lọc

Chia ba lượt tuần tự: P3a → P3b → P3c.

#### P3a — BE: gợi ý từ reflection, lưu file JSON (core)

1. **Spec test (đỏ)**, chạy với domain giả trong `src/test` và với domain ecommerce:
   - `DomainModelSuggester`:
     - context = package con dưới gốc domain (`inventory`, `order`, `shipping`…; package `shared` bị bỏ qua);
     - class có định danh (implement `shared.Identifiable`, hoặc có `getId()`) + state thay đổi được + không bị class cùng context sở hữu qua field → `aggregate` (`Order` là aggregate, `OrderLine` không vì `Order` chứa `List<OrderLine>`);
     - enum / record / class bất biến → `value`;
     - field hoặc tham số có kiểu là class domain khác → quan hệ nguồn `static`.
   - Mỗi gợi ý mang `source` (`package`, `identity`, `field`, `param`, `observed:<scenarioId>`) để FE hiện nhãn.
   - `ObservedRelations`: chạy mọi kịch bản, gộp flow theo cặp `cls` của node hai đầu → quan hệ nguồn `observed`. Bắt được tham chiếu bằng id mà reflection sót. Node collection của visualizer (`orders`, `shipments`) quy về `cls` của phần tử con.
   - `DomainModelValidator`: class không tồn tại trong domain → lỗi; một class thuộc nhiều context → lỗi; quan hệ trỏ tới class không có → lỗi; thiếu `version` → lỗi.
   - `DomainModelStore`:
     - đọc file nếu có, không có file thì trả gợi ý (`GET` báo `origin: suggestion`);
     - ghi nguyên tử (file tạm + move);
     - `PUT` mô hình không hợp lệ → 400 kèm danh sách lỗi;
     - không ghi được file → 409 kèm thông báo.
   - Đã bỏ khi dọn code: FE tự tính context/aggregate từ mô hình/bản nháp (`draft.js`), nên server không điền vào node nữa; vì vậy cũng không còn vòng phụ thuộc store ↔ ScenarioService.
2. **Endpoint**: `GET /api/viz/model/suggestion`, `GET /api/viz/model`, `PUT /api/viz/model`.
3. **Đường dẫn file**: property `visualizer.model.path`, mặc định `src/main/resources/domain-model.json`. Đường dẫn tính từ thư mục module, vì `spring-boot:run` chạy trong `visualizer/`. Store đọc file theo đường dẫn này trước rồi mới tới classpath, để bản vừa lưu có hiệu lực ngay mà không cần build lại.
4. **Định dạng file** (có `version` để đổi schema sau này):

   ```json
   { "version": 1,
     "contexts": [{ "id": "inventory", "title": "Kho", "color": "--hub" }],
     "classes":  [{ "cls": "Warehouse", "context": "inventory", "role": "aggregate", "hidden": false }],
     "relations": [{ "from": "Order", "to": "Shipment", "source": "observed" }] }
   ```

**Đã làm (P3a):** code ở `visualizer/.../core/model/` (`DomainModel`, `DomainModelSuggester`, `ObservedRelations`, `DomainModelValidator`, `DomainModelStore`) và `core/web/DomainModelController`.
- Vai trò: `aggregate | entity | value | service | other` (chuỗi thường trong JSON). Thứ tự xét: `Throwable` thành `other` + `hidden`; enum/record thành `value`, interface hoặc lớp trừu tượng không có field thành `other` (`source: type`); có định danh (`getId()`/`id()` public kể cả kế thừa, hoặc implement interface tên đơn giản `Identifiable`) thì: bị class cùng context sở hữu qua field (kể cả `List<X>`, mảng) là `entity`, ngược lại có state đổi được (field không final, hoặc final kiểu `Collection`/`Map`) là `aggregate`, còn bất biến là `entity`; không định danh: không có field instance nào (chỉ hành vi) là `service` (`source: type`), có state đổi được là `service`, bất biến là `value`. Sở hữu không tính khi class chứa là lớp con của class bị chứa (composite, vd. `BundleProduct` chứa `List<Product>`). Quan hệ: field/tham số ctor-method public tới class domain khác (field thắng param), cộng cặp quan sát từ flow (`observed:<id kịch bản>`), cặp đã có thì giữ nguồn tĩnh.
- Ecommerce ra **6** context (catalog, customer, inventory, order, pricing, shipping), không phải 5: `domain.customer` có `Address`.
- Đã bỏ `ModelAnnotator` khi dọn code (xem trên), nên `VisualizerController` không còn phụ thuộc store. Gợi ý chạy đủ 12 kịch bản nên tính lười một lần rồi cache.
- File mô hình: property `visualizer.model.path` (mặc định `src/main/resources/domain-model.json`, tính từ thư mục làm việc của tiến trình; `spring-boot:run` chạy trong `visualizer/`). Thứ tự nạp: file, classpath `/domain-model.json`, gợi ý. P3a **chưa** tạo `domain-model.json`; việc đó làm ở P3b qua giao diện.
- `source` ngoài danh sách trong plan: `type` (luật theo kiểu: Throwable/enum/record/interface). Các `source` khác: `identity`, `field`, `param`, `observed:<id>`, `manual`.
- Mốc so sánh, gợi ý cho domain ecommerce (heuristic cho phép sai; user sửa ở P3b):

| Class | Context | Role | Source |
|---|---|---|---|
| Product, BundleProduct, DigitalProduct, PhysicalProduct, SubscriptionProduct | catalog | aggregate | identity |
| Category, ProductType | catalog | value | type |
| OutOfStockException, ProductNotFoundException (ẩn) | catalog | other | type |
| Address | customer | value | field |
| InventoryManager | inventory | service | field |
| Warehouse | inventory | aggregate | identity |
| WarehouseTransferService | inventory | service | type |
| Order | order | aggregate | identity |
| OrderLine | order | value | field |
| Cart | order | service | field |
| DiscountCode | pricing | value | field |
| Discountable, PricingRule | pricing | other | type |
| Shipment | shipping | aggregate | identity |
| ShipmentEvent, ShipmentStatus, ShippingZone | shipping | value | type |
| ShippingFeeCalculator | shipping | service | type |
| Shippable | shipping | other | type |
| IllegalShipmentTransitionException (ẩn) | shipping | other | type |

  Quan hệ quan sát được thêm (chưa thấy qua field/param): `Cart→Order`, `DiscountCode→Order`, `InventoryManager→Cart`, `InventoryManager→ShippingZone`, `Order→InventoryManager`, `Order→Shipment`, `Order→ShippingZone`, `Shipment→InventoryManager`, `Warehouse→InventoryManager`. Hạn chế đã biết: `OrderLine` thành `value` vì không có `getId()`; `Cart` thành `service` vì không có định danh.

**Xong khi:** test P3a xanh. Gợi ý cho domain ecommerce ra 6 context (catalog, customer, inventory, order, pricing, shipping) với aggregate hợp lý; ghi kết quả gợi ý vào plan làm mốc so sánh. `PUT` rồi `GET` trả đúng nội dung đã lưu.

#### P3b — FE: trình sửa layer

- Panel `model-editor`:
  - cột trái là class đã quét, gom theo context gợi ý, kèm nhãn nguồn;
  - kéo class vào context; thêm, đổi tên, gộp, xoá context;
  - đổi vai aggregate/value; ẩn class;
  - xem và bỏ quan hệ.
- Áp ngay: mọi thay đổi vẽ lại bản đồ từ bản nháp (`localStorage`, bọc try/catch). Nút **Lưu** gọi `PUT`, lỗi validate hiện ngay cạnh class lỗi. Nút **Bỏ nháp** và **Lấy lại gợi ý**.
- Báo lệch: dựa trên banner sync và `domain-api.txt`, liệt kê class mới chưa xếp và class trong file không còn tồn tại; có nút đưa class mới vào context gợi ý.
- Hàm thuần `model/draft.js` (áp thao tác lên mô hình, gộp/tách context) và `model/drift.js` (so mô hình với danh sách class) có test `node --test`.

**Đã làm (P3b):** code ở `static/js/model/` (`draft.js`, `drift.js`, hàm thuần), `static/js/panels/model-editor.js` (drawer bên phải, nút **Mô hình** ở header, trước nút `?`), `api.js` (`getModel`, `getSuggestion`, `saveModel`), `scene.setDim`.
- Gán class vào context bằng **dropdown** trên từng dòng class, không kéo thả (theo mục Rủi ro). Dòng class có context, vai trò, checkbox ẩn và nhãn nguồn; lỗi validate (`errors[].cls`) hiện ngay dưới class lỗi, lỗi không có `cls` và lỗi 409 hiện ở đầu drawer. Quan hệ bị bỏ vẫn hiện gạch ngang kèm nút khôi phục cho tới khi lưu (chỉ giữ trong bộ nhớ, reload thì mất).
- Báo lệch tính từ `GET /model/suggestion` (danh sách class hiện tại của domain), không đọc `domain-api.txt`. `adoptAdded` đưa class mới vào cùng context gợi ý (thêm context còn thiếu) và các quan hệ gợi ý nối class đó với class đã có; **Bỏ** class không còn trong domain thì xoá luôn quan hệ của nó.
- Bản nháp: `localStorage` key `viz.model.draft` = `{ baseOrigin, model }`, ghi sau mỗi thay đổi, xoá khi **Lưu** thành công hoặc **Bỏ nháp**. Mở trang: có nháp thì dùng nháp, không thì `GET /model`. Badge `nháp` hiện khi nháp khác bản đã lưu. Chế độ fixture: nút **Mô hình** bị tắt (cần BE).
- "Áp ngay" ở P3b gồm hai thứ: bảng chú thích khi click khối có thêm dòng `context · role` (và `aggregate X`) tính từ nháp hiện hành; khối của class `hidden` trong nháp bị làm mờ. `scene.setDim(predicate)` gắn class CSS `dim` (`filter: opacity(.25)`, không dùng `opacity` vì animation pop-in giữ opacity) lên group của khối mà `predicate(node)` đúng, áp lại cuối mỗi `sync`/`reset` vì khối con được tạo lại. Tầng và bộ lọc để P3c.
- Test JS: `node --test 'visualizer/src/test/js/*.test.mjs'` (Node 22 không nhận đường dẫn thư mục, phải dùng glob có nháy).
- Khi review trên trình duyệt: class có context không khai báo (vd. file sửa tay) hiện ở nhóm "Chưa xếp" để lỗi validate của nó không bị mất; lỗi của class không có dòng trong panel đẩy lên đầu. Màn hẹp (≤ 480px) tên class xuống dòng riêng.
- `domain-model.json` của repo dựng từ gợi ý qua giao diện (**Lưu**, chưa sửa tay). Vì file này nằm trên classpath, store có thêm property `visualizer.model.resource` (mặc định `/domain-model.json`, rỗng thì bỏ classpath); test controller đặt rỗng để kiểm trường hợp chưa lưu.

**Xong khi:** trên trình duyệt dựng được mô hình cho ecommerce từ gợi ý, lưu thành `domain-model.json`, reload vẫn giữ. File được commit làm mô hình mặc định của repo.

#### P3c — Xem theo tầng + bộ lọc (đọc từ mô hình đã lưu)

1. **FE**
   - Ba tầng: **context map** (mỗi context một khối, cạnh = quan hệ gộp, số lời gọi/event trên cạnh, khối nháy khi có lời gọi nội bộ); **aggregate** (trong một context: khối theo loại aggregate, số instance, đánh dấu bị chạm; aggregate ở context khác hiện như cổng ở mép); **instance** (bản đồ chi tiết hiện tại, chỉ hiện context/aggregate đang chọn).
   - Điều hướng: click khối để vào sâu, breadcrumb để lùi, Esc lùi một tầng; trạng thái lưu trong URL hash để reload giữ nguyên.
   - Flow theo tầng: tổng quát hoá `resolveFlow` thành "tổ tiên gần nhất có mặt ở tầng hiện tại" (instance → aggregate → context); flow hai đầu cùng một khối thì nháy khối thay vì vẽ mũi tên.
   - Bộ lọc (thanh trên bản đồ): context (nhiều), loại aggregate, kết quả (OK/THROWN), loại flow (call/event), thread, "chỉ object có thay đổi". Lọc áp cho cả bản đồ (làm mờ phần không khớp), log và phát flow (flow bị lọc phát nhanh, không vẽ). Lưu lựa chọn trong `localStorage` (bọc try/catch).
   - Hàm thuần `levels.js` (dựng khối từng tầng, resolve flow) và `filters.js` có test `node --test`: flow instance→instance khác context hiện ở tầng context; cùng context thì nháy; bộ lọc thread chỉ giữ event của thread đó; tầng aggregate đếm đúng số instance.

**Đã làm (P3c):** (sửa khi review trên trình duyệt: SVG tầng 2D ẩn/hiện bằng attribute `hidden` vì `SVGElement` không có property `hidden`; nhãn cạnh chỉ hiện khi có lời gọi/event.) code ở `static/js/view/` (`levels.js`, `filters.js` là hàm thuần có test `levels.test.mjs`, `filters.test.mjs`; `level-view.js` vẽ; `router.js` điều hướng), `static/js/panels/filter-bar.js`, và thay đổi nhỏ ở `player.js` (nhận router thay scene, truyền event vào `flow`, `refresh()`), `panels/log.js` (tham số `matches`). Không đổi Java.
- **Hai bản đồ:** tầng instance vẫn là bản đồ isometric của adapter; tầng context và aggregate là một `<svg>` 2D phẳng thứ hai trong cùng panel (lưới tự động theo thứ tự mô hình, G9; cổng xếp cột bên phải). Router luôn chuyển `reset`/`sync` cho scene nên quay lại instance là tức thì; svg isometric khi không dùng được đẩy ra ngoài màn hình (không `display:none`, vì scene đo độ dài chữ lúc sync). `flow`/`flash` chỉ đến view đang hiện; không có mô hình (fixture) thì chỉ có tầng instance.
- **Focus là làm mờ, không gỡ khối:** "chỉ hiện context/aggregate đang chọn" ở tầng instance làm mờ khối ngoài vùng chọn (`scene.setDim`) để giữ nguyên bố cục tay của adapter.
- **Ánh xạ khối** (`blockOf`): tầng context `ctx:<context>`; tầng aggregate trong context C, node thuộc C → `agg:<aggregate của class, không có thì chính class>` nên service và class lẻ có khối riêng gắn nhãn vai trò, node area của C không có khối, node thuộc context D khác → `port:D`; tầng instance là chính node id. Id con lạ (chưa có trong world) đoán qua `PREFIXES` của adapter. Flow hai đầu cùng khối thì nháy khối, một đầu không có khối thì bỏ.
- **Hash:** `#level=aggregate&ctx=order&agg=Order` (`ctx`/`agg` bỏ khi null); hash lạ về instance. Khi mô hình chưa nạp hoặc không có (fixture) hash được giữ nhưng hiệu dụng là instance; tầng aggregate thiếu ctx lấy context đầu tiên. Click khối context → aggregate của context; click khối aggregate → instance focus `{ctx, agg}`; click cổng → aggregate của context đó; Esc lùi một tầng (bỏ qua khi đang gõ vào input/select hoặc drawer/dialog mở); nút **Instance** trên thanh chuyển tầng bỏ focus.
- **Bộ lọc** (`viz.filters` trong `localStorage`): context và aggregate (chọn nhiều, ghép bằng AND, aggregate chỉ liệt kê của context đã chọn) khớp nếu một đầu mút flow nằm trong vùng chọn, đầu mút không tra ra thì coi là khớp; kết quả và thread xét trên event; loại flow xét `flow.kind ?? 'CALL'`; "chỉ object có thay đổi" so với **đầu lượt chạy** (`changedNodeIds(worldBefore, world hiện tại)`, không chỉ bước cuối) và chỉ áp cho khối (area là khung chứa nên không bị mờ vì điều kiện này); viền "bị chạm" ở tầng context/aggregate thì theo bước vừa phát. Thanh lọc aggregate chỉ liệt kê class vai trò `aggregate` (service/value lẻ vẫn có khối riêng ở tầng aggregate). Flow/event không khớp thì không vẽ và không chờ (phát nhanh); khối, khối tầng context/aggregate (không có node khớp) và dòng log không khớp bị làm mờ, không ẩn (chỉ số bước log giữ nguyên). Không có mô hình thì bỏ qua context/aggregate; kết quả, loại flow, thread, "chỉ thay đổi" vẫn chạy.
- **`changedNodeIds`:** so `items` (key/value) của node trong world trước và world sau theo node id, node mới cũng tính là đổi. Không dùng `ObjectChange.ref` vì ref là tiêu đề do adapter đặt, không bằng node id. Router lấy "world trước" là world của lần sync/reset ngay trước, nên sau **Back** tập "đổi" chỉ là so hai world kề nhau, không phải đúng bước vừa lùi. Khối được đánh dấu "chạm" (viền đậm) khi có node thuộc khối nằm trong tập này.
- **Bộ đếm trên cạnh:** `n lời gọi · m event` tính lại từ các flow đã phát từ lần Reset (không trừ khi Back), tầng aggregate chỉ vẽ cạnh từ flow đã phát.

**Xong khi:** test Java + JS xanh; trên trình duyệt đi được context → aggregate → instance và lùi lại với kịch bản `overview`; flow hiện đúng ở cả ba tầng; từng bộ lọc làm mờ/ẩn đúng; reload giữ tầng và bộ lọc.

## 5. Kiểm chứng

```bash
./mvnw -pl visualizer -am test
node --test 'visualizer/src/test/js/*.test.mjs'
./mvnw test -DexcludedGroups=docker      # đảm bảo app/domain không bị ảnh hưởng
```

Kiểm tra trên trình duyệt bằng preview `visualizer` (`.claude/launch.json`, port 8081), cả chế độ BE thật và `?fixture=1` / `?fixture=events`; xem console không còn cảnh báo `checkLayout()`.

## 6. Rủi ro

- **P1 chạm gần như mọi file** → làm một lượt liền, không song song với việc khác trên visualizer; kiểm tra skill `visualizer-sync` chạy được sau khi đổi FQN.
- **Event trong kịch bản đa luồng**: event phát trong lúc domain giữ lock; `emit()` chỉ ghi vào buffer theo thread, không chụp snapshot hay gọi gì khác trong đó.
- **Phạm vi FE P3 lớn nhất**, nặng nhất là trình sửa kéo thả (P3b). Nếu quá tải, P3b bản đầu dùng dropdown chọn context cho từng class thay vì kéo thả; P3c tách tiếp thành tầng + điều hướng và bộ lọc.
- **Gợi ý reflection sai với dự án chia package khác bounded context**: đây là lý do có trình sửa và nhãn nguồn. Không cố làm heuristic hoàn hảo.
- **Ghi file lúc chạy**: chỉ dùng khi chạy từ source (`spring-boot:run`). Chạy từ jar thì không ghi được, khi đó `PUT` trả 409 có thông báo rõ.
- Fixture `sample-run.json` cần sinh lại sau khi JSON thêm trường (`kind`, `events`, `context`, `aggregate`).
