# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Repo học Java core của Tùng: mỗi buổi (`sXX`) áp dụng lý thuyết vào **một project ecommerce duy nhất** lớn dần theo buổi. Tài liệu và comment viết bằng tiếng Việt — giữ nguyên ngôn ngữ đó.

## Lệnh

```bash
./mvnw test                                   # build + test toàn bộ (Java 25); test @Tag("docker") cần Docker
./mvnw test -DexcludedGroups=docker           # bỏ test Testcontainers/PostgreSQL
./mvnw -pl domain test                        # chỉ module domain
./mvnw -pl domain test -Dtest=InventoryManagerTest          # một test class
./mvnw -pl app -am test -Dtest=OrderDetailServiceTest -Dsurefire.failIfNoSpecifiedTests=false
./mvnw -pl app -am spring-boot:run            # profile local (mặc định, in-memory); Swagger: http://localhost:8080/swagger-ui.html
./mvnw -pl visualizer -am spring-boot:run     # Domain Visualizer: http://localhost:8081
docker compose up --build                     # profile dev: PostgreSQL + backend trong Docker
docker compose up -d db && ./mvnw -pl app -am spring-boot:run -Dspring-boot.run.profiles=dev   # BE trên IDE, DB trong Docker
./mvnw -q -pl app -am compile && java -cp app/target/classes:domain/target/classes com.training.ecommerce.demo.S06Demo
java playground/session01/IntegerCacheDemo.java   # file playground chạy đơn lẻ, không qua Maven
```

## Kiến trúc

- Maven multi-module; parent `pom.xml` kế thừa Spring Boot parent **chỉ để quản lý version** — không khai báo `<dependencies>` ở parent vì sẽ bị kế thừa xuống `domain`.
- `domain/` (artifact `ecommerce-domain`): Java thuần, gồm `com.training.ecommerce.domain.**` (model theo domain: catalog, inventory, order, pricing, shipping, customer) và `shared.**` (`Repository`/`Identifiable`, `TextUtils`). Không có Spring/Lombok trên classpath; `maven-enforcer-plugin` chặn nếu ai thêm vào `domain/pom.xml`.
- `app/` (Spring Boot + Lombok + springdoc), phụ thuộc `domain`:
  - `application.<catalog|inventory|order|payment|pricing|report|shipping>`: service/use case và các port (vd. `StockStore`, `PaymentGateway`, `OrderLineSource`). `OrderService.placeOrder` là `@Transactional`: trừ kho + lưu đơn cùng commit/rollback. Tương tự `ShipmentService.markReturned`/`cancel`: đổi trạng thái vận đơn + hoàn kho (`StockStore.increaseStock`) cùng commit/rollback.
  - `infrastructure`: `persistence/jdbc` (`JdbcClient` + PostgreSQL, `schema.sql`/`data.sql` ở `app/src/main/resources`), `persistence/inmemory`, `fake/` (implement port với `SimulatedLatency`). Class persistence không có annotation Spring; wiring theo profile ở `infrastructure/config` (`JdbcPersistenceConfig` = `dev`, `InMemoryPersistenceConfig` = `local`, kèm seed).
  - `interfaces/rest/<catalog|order|shipping>`: controller + DTO record + mapper (không trả domain object ra ngoài); `RestExceptionHandler` map exception → `ProblemDetail` (not found 404, `OutOfStockException`/ràng buộc DB 409, IAE 400). `interfaces/cli`: `ReceiptPrinter`.
  - `demo/SXXDemo`: entry point `main` chạy thử từng buổi.
- `visualizer/` (S07 extra, Spring Boot, port 8081): bản đồ 2.5D + call log + diff trạng thái, chỉ phụ thuộc `domain`, không sửa `domain/`/`app/`. **Sửa `domain/` (public API hoặc hành vi) thì chạy skill `visualizer-sync`**; app hiện banner khi `domain-api.txt` lệch với domain. Code chia lõi `visualizer.core.**` + `static/js/**` (không biết domain, `core/ArchitectureTest` chặn import domain) và adapter `visualizer.ecommerce.**` + `static/adapter/**` (mọi thứ riêng của ecommerce). Plan: `docs/extras/s07-domain-visualizer.md`, `docs/extras/s07-visualizer-generic.md`.
- Dependency rule: `interfaces → application → domain ← infrastructure`; `domain.**` được dùng `shared.**`, không được import `app`.
- Profile: `local` (mặc định, `application-local.properties` tắt autoconfig DataSource/JDBC) và `dev` (`application-dev.properties`, ghi đè bằng `SPRING_DATASOURCE_URL`/`_USERNAME`/`_PASSWORD`). Test JDBC dùng Testcontainers `postgres:17` qua `PostgresJdbcTestSupport` (một container dùng chung) + `@ActiveProfiles("dev")`; Testcontainers không đọc Docker context nên với OrbStack cần `DOCKER_HOST` hoặc `~/.testcontainers.properties`.
- Service nhận dependency qua constructor (kể cả `ExecutorService`); service không tự tạo/đóng executor — bên tạo thì bên đóng. Constructor/method validate null/rỗng và ném `IllegalArgumentException`; contract exception ghi trong Javadoc `@throws`.

## Quy ước

- **Package theo domain:** khi thêm class mới, đặt vào package con theo domain / use case (`domain.<catalog|inventory|order|pricing|…>`, `application.<pricing|report|order|payment|shipping|…>`), không để phẳng trong `application` hay `domain`; test đi cùng package với class.
- Nhánh `sXX/<topic>` (vd. `s06/concurrency-inventory`) → PR vào `main` theo `.github/PULL_REQUEST_TEMPLATE.md`; merge xong sau review T4 thì tag `sXX-done`.
- Commit có tiền tố buổi: `[S06] add spec tests (red)`. Luồng TDD mỗi buổi: spec test đỏ trước, rồi implementation.
- Task và acceptance criteria của từng buổi nằm trong `docs/ecommerce-backlog.md`; ghi chú buổi học ở `docs/sessions/session-XX.md`, tiến độ ở `docs/progress.md`, ôn tập ở `docs/review/`.
- `playground/sessionXX/` là code thử kiến thức, không thuộc build Maven; `slides/` không được track.
