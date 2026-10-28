# Java Training – E-commerce

Repo học Java core của Tùng. Mỗi buổi lý thuyết đều được áp dụng ngay vào **một project ecommerce duy nhất**, và project này lớn dần theo từng buổi.

## Nhịp học mỗi tuần

| Ngày | Việc | Output trong repo |
|---|---|---|
| Trước T7 | Tự học trước (self-study) | Ghi chú vào `docs/sessions/session-XX.md` |
| **T7** | Review lý thuyết + làm bài tập trên lớp | Tick keyword, code thử trong `playground/sessionXX/` |
| T7 → T4 | Áp dụng vào ecommerce | Nhánh `sXX/<topic>` → PR vào `main` |
| **T4** | Review code | Ghi feedback vào session file + `docs/progress.md`, sửa rồi merge, tag `sXX-done` |

**Mô hình làm bài (Phương án A):** mỗi người tự implement cùng một task trên repo/nhánh riêng. Buổi T4 cả lớp so sánh cách làm của nhau. Repo này là bài làm của riêng Tùng.

## Cấu trúc repo

```
java_training/
├── README.md
├── pom.xml                         # Java 25, JUnit 5
├── docs/
│   ├── curriculum.md               # roadmap các buổi (cập nhật sau mỗi T7)
│   ├── progress.md                 # BẢNG TRACKING CHÍNH
│   ├── ecommerce-backlog.md        # task ecommerce theo từng buổi + acceptance criteria
│   ├── sessions/session-XX.md      # ôn tập từng buổi: keyword, quiz, bài tập, feedback T4
│   └── review/
│       ├── keywords.md             # tổng hợp keyword + mức tự tin (0–3)
│       ├── how-to-review.md        # cách ôn (lịch lặp lại, tự kiểm tra)
│       ├── mistakes.md             # nhật ký lỗi & giả định sai (java-quiz, java-explore)
│       └── gaps.md                 # gap ngoài/sâu hơn lớp học (java-explore tạo khi chạy lần đầu)
├── playground/sessionXX/           # code thử kiến thức, chạy bằng `java File.java`
├── pom.xml                         # parent (Spring Boot BOM), modules: domain, app
├── domain/                         # module Java thuần: KHÔNG có Spring/Lombok trên classpath
│   └── src/{main,test}/java/com/training/ecommerce/
│       ├── shared/                 # tiện ích dùng chung (TextUtils…)
│       └── domain/                 # model thuần Java, không phụ thuộc gì khác
└── app/                            # module Spring Boot, phụ thuộc domain
    └── src/{main,test}/java/com/training/ecommerce/
        ├── EcommerceApplication.java
        ├── application/            # service / use case, chia package theo domain/use case:
        │                           #   pricing/, report/, order/, payment/, shipping/
        ├── infrastructure/         # lưu trữ in-memory / JDBC, đọc/ghi file
        ├── interfaces/             # cli/ (ReceiptPrinter…), REST controller
        └── demo/                   # SXXDemo: entry point chạy thử theo từng buổi
```

Dependency rule: `interfaces → application → domain ← infrastructure`. Module `domain` (gồm package `domain.**` và `shared.**`) **không được import module khác** (`app`); trong module, `domain.**` được dùng `shared.**`. Rule này được **compiler enforce**: module `domain` không có Spring/Lombok/`app` trên classpath, nên import vào là compile lỗi; thêm dependency đó vào `domain/pom.xml` thì `maven-enforcer-plugin` chặn.

## Quy ước

- Nhánh: `s02/order-line-calculator`, `s03/product-pricing`…
- Commit: `[S03] add PhysicalProduct shipping`
- Demo console mỗi buổi: `app/.../demo/SXXDemo.java` (package `com.training.ecommerce.demo`, ví dụ `S03Demo`)
- Mỗi PR dùng template trong `.github/PULL_REQUEST_TEMPLATE.md`
- Review T4 xong và merge thì gắn tag: `git tag s03-done`

## Chạy

```bash
./mvnw test                               # build + test toàn bộ (test @Tag("docker") cần Docker cho Testcontainers)
./mvnw test -DexcludedGroups=docker       # bỏ qua test cần Docker
./mvnw -pl domain test                    # chỉ module domain
./mvnw -pl app -am spring-boot:run        # profile local (mặc định): in-memory, không cần DB
java playground/session01/IntegerCacheDemo.java   # chạy file demo đơn lẻ
```

Swagger UI: http://localhost:8080/swagger-ui.html · OpenAPI JSON: http://localhost:8080/v3/api-docs

### Profile

| Profile | Backend | DB | Chạy |
|---|---|---|---|
| `local` (mặc định) | IDE / Maven | `InMemoryRepository` + dữ liệu mẫu, mất khi tắt app | `./mvnw -pl app -am spring-boot:run` |
| `dev` | Docker | PostgreSQL 17 trong Docker, schema + seed từ `schema.sql` / `data.sql` | `docker compose up --build` |
| `dev` (BE trên IDE) | IDE / Maven | PostgreSQL trong Docker | `docker compose up -d db` rồi `./mvnw -pl app -am spring-boot:run -Dspring-boot.run.profiles=dev` |

Dữ liệu PostgreSQL nằm ở volume `pgdata`: `docker compose down` giữ dữ liệu, `docker compose down -v` xóa sạch. User/password DB đọc từ biến môi trường (`POSTGRES_USER`, `POSTGRES_PASSWORD`, mặc định `ecommerce`).

Testcontainers không đọc Docker context: với OrbStack / Colima cần trỏ socket, ví dụ thêm `docker.host=unix:///Users/<user>/.orbstack/run/docker.sock` vào `~/.testcontainers.properties`.
