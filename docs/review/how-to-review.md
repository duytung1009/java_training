# Cách ôn tập

## Lịch lặp lại (spaced repetition) cho mỗi buổi

| Lần | Khi nào | Làm gì | Thời gian |
|---|---|---|---|
| R0 | Tối T7 (ngay sau buổi học) | Đọc lại **Tóm tắt 1 phút**, tick keyword, ghi câu hỏi còn thắc mắc | 20′ |
| R1 | CN / T2 | Làm **Quiz** (chưa mở đáp án), chạy file demo trong `playground/`, **đoán output trước khi chạy** | 30′ |
| R2 | T3 (trước review T4) | Làm task ecommerce, tự review PR theo checklist | theo task |
| R3 | T7 tuần sau (trước giờ học) | Ôn lại các keyword có tự tin ≤ 1 của buổi trước | 10′ |
| R4 | Sau 1 tháng | Làm lại quiz của cả module | 30′ |

## 3 kỹ thuật chính
1. **Active recall**: che đáp án rồi tự trả lời. Đọc lại một cách thụ động không tính là ôn.
2. **Feynman**: giải thích keyword bằng 2–3 câu như đang dạy người mới. Chỗ nào bị vấp là chỗ chưa hiểu.
3. **Đoán output rồi mới chạy**: các file trong `playground/sessionXX/*Pitfalls.java` có đánh số thứ tự output. Viết dự đoán ra giấy, chạy, rồi so sánh.

## Checklist trước buổi review T4
- [ ] PR đã mở, mô tả rõ đã áp dụng keyword nào
- [ ] `mvn test` pass
- [ ] Đã ghi 1–2 câu hỏi muốn hỏi vào PR
- [ ] Đã cập nhật `docs/progress.md`

## Sau buổi review T4
- [ ] Chép feedback vào mục 7 của session file và vào bảng feedback trong `progress.md`
- [ ] Sửa code, merge, `git tag sXX-done`
- [ ] Lỗi nào lặp lại từ 2 lần trở lên thì đưa vào "Lỗi hay lặp lại"
