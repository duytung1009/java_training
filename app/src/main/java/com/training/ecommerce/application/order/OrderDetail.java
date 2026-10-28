package com.training.ecommerce.application.order;

import com.training.ecommerce.application.payment.PaymentInfo;
import com.training.ecommerce.application.shipping.ShipmentInfo;
import com.training.ecommerce.domain.order.OrderLine;

import java.util.List;

/**
 * S06: dữ liệu trang chi tiết đơn, gộp từ 3 nguồn. lines là bản copy không sửa được.
 *
 * @throws IllegalArgumentException khi một field null
 */
public record OrderDetail(String orderId, PaymentInfo payment, ShipmentInfo shipment, List<OrderLine> lines) {
    public OrderDetail {
        if (orderId == null || payment == null || shipment == null || lines == null) {
            throw new IllegalArgumentException("orderId, payment, shipment, lines không được null");
        }
        lines = List.copyOf(lines);     // copy phòng thủ: sửa list gốc không ảnh hưởng record (immutability, S05)
    }
}
