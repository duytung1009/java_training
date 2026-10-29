package com.training.ecommerce.interfaces.rest.shipping;

import java.math.BigDecimal;
import java.util.List;

/**
 * Vận đơn trả về cho client; fee scale 2, history theo thứ tự thời gian.
 */
public record ShipmentResponse(
        String id,
        String orderId,
        String trackingCode,
        String status,
        int failedAttempts,
        BigDecimal fee,
        String street,
        String city,
        List<EventResponse> history) {

    /** note có thể null. */
    public record EventResponse(String status, String note) {
    }
}
