package com.training.ecommerce.visualizer.core.view;

import java.util.Map;

/**
 * Kết quả một lần chụp: {@code view} để vẽ, {@code objects} (ref → field → giá trị) để so sánh.
 *
 * @param view    dữ liệu vẽ bản đồ
 * @param objects trạng thái từng object, dùng để tính diff
 */
public record WorldSnapshot(WorldView view, Map<String, Map<String, Object>> objects) {
}
