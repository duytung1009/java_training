package com.training.ecommerce.infrastructure.fake;

import com.training.ecommerce.application.ShipmentInfo;
import com.training.ecommerce.application.ShipmentTracker;

import java.time.Duration;

/** S06: API vận chuyển giả, trả "DELIVERING" sau một khoảng trễ. Dùng cho demo. */
public class FakeShipmentTracker implements ShipmentTracker {
    private final Duration delay;

    public FakeShipmentTracker(Duration delay) {
        this.delay = delay;
    }

    @Override
    public ShipmentInfo track(String orderId) {
        SimulatedLatency.await(delay);
        return new ShipmentInfo("GHN", "DELIVERING");
    }
}
