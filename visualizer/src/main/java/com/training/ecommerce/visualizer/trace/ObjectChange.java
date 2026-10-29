package com.training.ecommerce.visualizer.trace;

import java.util.Map;

/**
 * Thay đổi của một object domain: chỉ chứa field có đổi.
 * <ul>
 *   <li>{@code before == null}: object mới tạo.</li>
 *   <li>{@code after == null}: object bị xoá.</li>
 *   <li>key chỉ có ở một phía: field được thêm / xoá.</li>
 * </ul>
 *
 * @param ref    tên dễ đọc, vd. "Warehouse WH-HN", "Order a1f3"
 * @param before giá trị trước (null nếu mới tạo)
 * @param after  giá trị sau (null nếu bị xoá)
 */
public record ObjectChange(String ref, Map<String, Object> before, Map<String, Object> after) {
}
