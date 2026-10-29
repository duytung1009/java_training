package com.training.ecommerce.visualizer.scenario;

import com.training.ecommerce.visualizer.trace.TraceRecorder;
import com.training.ecommerce.visualizer.world.World;

/**
 * Một kịch bản minh hoạ: gọi các method domain thật qua {@link TraceRecorder}.
 * Exception nghiệp vụ dự kiến được bắt trong kịch bản (recorder đã ghi lại), không làm hỏng request.
 */
public interface Scenario {

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
    void run(World world, TraceRecorder recorder);
}
