package com.training.ecommerce.interfaces.rest.shipping;

import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.domain.shipping.Shipment;

import java.util.List;

/**
 * Chuyển đổi giữa DTO của vận đơn và domain.
 */
final class ShipmentMapper {

    private ShipmentMapper() {
    }

    /** Không tự kiểm tra: street / city rỗng được {@code Address} từ chối bằng IAE. */
    static Address toAddress(CreateShipmentRequest request) {
        return new Address(request.street(), request.city());
    }

    static ShipmentResponse toResponse(Shipment shipment) {
        List<ShipmentResponse.EventResponse> history = shipment.history().stream()
                .map(event -> new ShipmentResponse.EventResponse(event.status().name(), event.note()))
                .toList();
        return new ShipmentResponse(shipment.getId(), shipment.getOrderId(), shipment.getTrackingCode(),
                shipment.getStatus().name(), shipment.getFailedAttempts(), shipment.getFee(),
                shipment.getAddress().getStreet(), shipment.getAddress().getCity(), history);
    }
}
