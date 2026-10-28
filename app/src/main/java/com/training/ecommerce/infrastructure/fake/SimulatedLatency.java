package com.training.ecommerce.infrastructure.fake;

import java.time.Duration;

/** S06: giả lập độ trễ của một lời gọi IO (DB, HTTP). Bị interrupt thì dừng ngay và báo lỗi. */
final class SimulatedLatency {

    private SimulatedLatency() {
    }

    static void await(Duration delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();          // giữ cờ cho executor / caller biết
            throw new IllegalStateException("Lời gọi bị hủy", e);
        }
    }
}
