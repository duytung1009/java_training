# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Repo học Java core của Tùng: mỗi buổi (`sXX`) áp dụng lý thuyết vào **một project ecommerce duy nhất** lớn dần theo buổi. Tài liệu và comment viết bằng tiếng Việt — giữ nguyên ngôn ngữ đó.

## Lệnh

```bash
./mvnw test                                   # build + test toàn bộ (Java 25)
./mvnw -pl domain test                        # chỉ module domain
./mvnw -pl domain test -Dtest=InventoryManagerTest          # một test class
./mvnw -pl app -am test -Dtest=OrderDetailServiceTest -Dsurefire.failIfNoSpecifiedTests=false
./mvnw -pl app -am spring-boot:run            # chạy app, Swagger: http://localhost:8080/swagger-ui.html
./mvnw -q -pl app -am compile && java -cp app/target/classes:domain/target/classes com.training.ecommerce.demo.S06Demo
java playground/session01/IntegerCacheDemo.java   # file playground chạy đơn lẻ, không qua Maven
```

## Kiến trúc

- Maven multi-module; parent `pom.xml` kế thừa Spring Boot parent **chỉ để quản lý version** — không khai báo `<dependencies>` ở parent vì sẽ bị kế thừa xuống `domain`.
- `domain/` (artifact `ecommerce-domain`): Java thuần, gồm `com.training.ecommerce.domain.**` (model theo domain: catalog, inventory, order, pricing, shipping, customer) và `shared.**` (`Repository`/`Identifiable`, `TextUtils`). Không có Spring/Lombok trên classpath; `maven-enforcer-plugin` chặn nếu ai thêm vào `domain/pom.xml`.
- `app/` (Spring Boot + Lombok + springdoc), phụ thuộc `domain`:
  - `application.<order|payment|pricing|report|shipping>`: service/use case và các port (vd. `PaymentGateway`, `ShipmentTracker`, `OrderLineSource`).
  - `infrastructure`: `persistence/inmemory/InMemoryRepository`, `fake/` (implement port với `SimulatedLatency`).
  - `interfaces/cli`: `ReceiptPrinter`; REST controller cũng đặt ở `interfaces`.
  - `demo/SXXDemo`: entry point `main` chạy thử từng buổi.
- Dependency rule: `interfaces → application → domain ← infrastructure`; `domain.**` được dùng `shared.**`, không được import `app`.
- Service nhận dependency qua constructor (kể cả `ExecutorService`); service không tự tạo/đóng executor — bên tạo thì bên đóng. Constructor/method validate null/rỗng và ném `IllegalArgumentException`; contract exception ghi trong Javadoc `@throws`.

## Quy ước

- **Package theo domain:** khi thêm class mới, đặt vào package con theo domain / use case (`domain.<catalog|inventory|order|pricing|…>`, `application.<pricing|report|order|payment|shipping|…>`), không để phẳng trong `application` hay `domain`; test đi cùng package với class.
- Nhánh `sXX/<topic>` (vd. `s06/concurrency-inventory`) → PR vào `main` theo `.github/PULL_REQUEST_TEMPLATE.md`; merge xong sau review T4 thì tag `sXX-done`.
- Commit có tiền tố buổi: `[S06] add spec tests (red)`. Luồng TDD mỗi buổi: spec test đỏ trước, rồi implementation.
- Task và acceptance criteria của từng buổi nằm trong `docs/ecommerce-backlog.md`; ghi chú buổi học ở `docs/sessions/session-XX.md`, tiến độ ở `docs/progress.md`, ôn tập ở `docs/review/`.
- `playground/sessionXX/` là code thử kiến thức, không thuộc build Maven; `slides/` không được track.
