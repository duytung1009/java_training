package com.training.ecommerce.visualizer.core.view;

/**
 * Một ô con trong node.
 *
 * @param key      khoá ổn định (vd. productId) để frontend nhận ra ô cũ / mới
 * @param label    nhãn ngắn hiển thị
 * @param value    số lượng (Integer) hoặc chuỗi đã định dạng (tiền, tỉ lệ)
 * @param type     tên class/kiểu mà ô đại diện (vd. Product, OrderLine, Map.Entry), hiện thành nhãn nhỏ trên ô
 * @param computed true nếu là kết quả hàm thuần chỉ để hiển thị, không phải state của object
 */
public record ItemView(String key, String label, Object value, String type, boolean computed) {

    /** Ô thuộc state của object (computed = false). */
    public ItemView(String key, String label, Object value, String type) {
        this(key, label, value, type, false);
    }
}
