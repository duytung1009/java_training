package com.training.ecommerce.visualizer.core.scenario;

import com.training.ecommerce.visualizer.core.trace.TraceRecorder;

/**
 * Một kịch bản minh hoạ: gọi các method domain thật qua {@link TraceRecorder}.
 * Exception nghiệp vụ dự kiến được bắt trong kịch bản (recorder đã ghi lại), không làm hỏng request.
 *
 * @param <W> kiểu "thế giới" của adapter mà kịch bản thao tác
 */
public interface Scenario<W> {

    /** Id dùng trong URL, vd. "cart-to-order". */
    String id();

    String title();

    /** Buổi học liên quan, vd. "S06". */
    String session();

    String description();

    /** Kịch bản có chạy nhiều thread hay không (UI hiện ghi chú diff có thể xen kẽ). */
    default boolean concurrent() {
        return false;
    }

    /**
     * Chạy kịch bản trên {@code world}; mọi lần gọi domain đi qua {@code recorder}.
     */
    void run(W world, TraceRecorder recorder);
}
