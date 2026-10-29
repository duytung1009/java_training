package com.training.ecommerce.visualizer.world;

/**
 * Một ô con trong panel.
 *
 * @param key   khoá ổn định (vd. productId) để frontend nhận ra ô cũ / mới
 * @param label nhãn ngắn hiển thị
 * @param value số lượng (Integer) hoặc chuỗi đã định dạng (tiền, tỉ lệ)
 * @param type  tên class/kiểu mà ô đại diện (vd. Product, Order, Map.Entry), hiện thành nhãn nhỏ trên ô
 */
public record ItemView(String key, String label, Object value, String type) {
}
