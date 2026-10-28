package com.training.ecommerce.infrastructure.fake;

import com.training.ecommerce.application.order.OrderLineSource;
import com.training.ecommerce.domain.order.OrderLine;

import java.time.Duration;
import java.util.List;

/** S06: nguồn dòng đơn giả (thay cho DB), trả list cố định sau một khoảng trễ. Dùng cho demo. */
public class FakeOrderLineSource implements OrderLineSource {
    private final Duration delay;
    private final List<OrderLine> lines;

    public FakeOrderLineSource(Duration delay, List<OrderLine> lines) {
        this.delay = delay;
        this.lines = List.copyOf(lines);
    }

    @Override
    public List<OrderLine> findLines(String orderId) {
        SimulatedLatency.await(delay);
        return lines;
    }
}
