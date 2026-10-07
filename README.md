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
├── src/main/java/com/training/ecommerce/
│   ├── shared/          # tiện ích dùng chung (TextUtils…)
│   ├── domain/          # model thuần Java, không phụ thuộc gì khác
│   ├── application/     # service / use case (PricingEngine, CheckoutService…)
│   ├── infrastructure/  # lưu trữ in-memory / JDBC, đọc/ghi file
│   └── interfaces/cli/  # App.java: menu console để demo
└── src/test/java/com/training/ecommerce/
```

Dependency rule: `interfaces → application → domain ← infrastructure`. Package `domain` không được import package nào khác trong project.

## Quy ước

- Nhánh: `s02/order-line-calculator`, `s03/product-pricing`…
- Commit: `[S03] add PhysicalProduct shipping`
- Mỗi PR dùng template trong `.github/PULL_REQUEST_TEMPLATE.md`
- Review T4 xong và merge thì gắn tag: `git tag s03-done`

## Chạy

```bash
mvn test                                  # build + test
java playground/session01/IntegerCacheDemo.java   # chạy file demo đơn lẻ
```
