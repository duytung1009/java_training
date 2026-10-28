package com.training.ecommerce.application.shipping;

/** S06: nguồn thông tin vận chuyển của đơn (bản thật gọi API đơn vị vận chuyển, có độ trễ). */
@FunctionalInterface
public interface ShipmentTracker {
    ShipmentInfo track(String orderId);
}
