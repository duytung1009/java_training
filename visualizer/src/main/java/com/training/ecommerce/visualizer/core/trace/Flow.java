package com.training.ecommerce.visualizer.core.trace;

/**
 * Hướng chảy dữ liệu giữa hai panel (dùng panel id). Bố cục bản đồ thuộc frontend.
 *
 * @param from  panel id nguồn
 * @param to    panel id đích
 * @param kind  CALL (lời gọi) hoặc EVENT (domain event); null coi như CALL
 * @param label nhãn hiển thị: tên event, hoặc dữ liệu chảy theo flow lời gọi (vd. {@code orderId}); null nếu không cần
 */
public record Flow(String from, String to, FlowKind kind, String label) {

    /**
     * @throws IllegalArgumentException nếu from hoặc to null/rỗng
     */
    public Flow {
        if (from == null || from.isBlank() || to == null || to.isBlank()) {
            throw new IllegalArgumentException("from và to của flow không được rỗng");
        }
        if (kind == null) {
            kind = FlowKind.CALL;
        }
    }

    /** Flow lời gọi: {@code kind = CALL}, không nhãn. */
    public Flow(String from, String to) {
        this(from, to, FlowKind.CALL, null);
    }
}
