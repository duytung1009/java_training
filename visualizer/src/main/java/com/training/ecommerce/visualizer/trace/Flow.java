package com.training.ecommerce.visualizer.trace;

/**
 * Hướng chảy dữ liệu giữa hai panel (dùng panel id). Bố cục bản đồ thuộc frontend.
 *
 * @param from panel id nguồn
 * @param to   panel id đích
 */
public record Flow(String from, String to) {

    /**
     * @throws IllegalArgumentException nếu from hoặc to null/rỗng
     */
    public Flow {
        if (from == null || from.isBlank() || to == null || to.isBlank()) {
            throw new IllegalArgumentException("from và to của flow không được rỗng");
        }
    }
}
