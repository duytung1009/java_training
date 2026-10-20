# Legacy practice (lưu trữ)

Bài practice cũ, copy nguyên từ `~/IdeaProjects/Training/src/main/java/org/example/` ngày 20/10.

- **Không tính** vào `docs/progress.md` hay `docs/ecommerce-backlog.md`.
- Giữ nguyên code gốc (package `org.example...`), không chỉnh sửa. Maven không compile thư mục này.
- Một số file cần JDK 25 (`static void main()` không tham số, `IO.println`); `session4/practice1` dùng Lombok nên không chạy được bằng `java File.java`.
- Case Study S02 (`session2/`) và S03 (`session3/ecommerce/`) **không** nằm ở đây; chúng được làm lại trong `src/` theo backlog.

| File | Nội dung |
|---|---|
| `TungVD7_Session_1.java`, `DrawBoxWithArray.java`, `WordCounter.java`, `TextTransfomer.java` | S01 practice (menu, vẽ box, đếm từ) |
| `TungVD7_Session_2_XO.java` | S02 practice: game XO |
| `session3/Session3Main.java`, `session3/shape/` | S03 practice: Shape hierarchy |
| `session4/practice1/`, `session4/practice2/` | S04 practice: Product/Laptop/Phone, TaskManager + HistoryLog |
