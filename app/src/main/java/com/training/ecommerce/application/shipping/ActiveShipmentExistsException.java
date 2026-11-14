package com.training.ecommerce.application.shipping;

import com.training.ecommerce.shared.error.BusinessException;

/**
 * Đơn đã có vận đơn còn hiệu lực (khác CANCELLED / RETURNED) nên không tạo thêm được (CONFLICT, mã {@code ACTIVE_SHIPMENT_EXISTS}).
 */
public class ActiveShipmentExistsException extends BusinessException {
    private final String orderId;

    public ActiveShipmentExistsException(String orderId) {
        super(Category.CONFLICT, "ACTIVE_SHIPMENT_EXISTS", "Đơn " + orderId + " đã có vận đơn còn hiệu lực");
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}
