package com.training.ecommerce.visualizer.world;

/**
 * Một ô con trong panel.
 *
 * @param key   khoá ổn định (vd. productId) để frontend nhận ra ô cũ / mới
 * @param label nhãn ngắn hiển thị
 * @param value số lượng (Integer) hoặc chuỗi đã định dạng (tiền, tỉ lệ)
 */
public record ItemView(String key, String label, Object value) {
}
