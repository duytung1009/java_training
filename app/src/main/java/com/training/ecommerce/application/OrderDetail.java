package com.training.ecommerce.application;

import com.training.ecommerce.domain.order.OrderLine;

import java.util.List;

/**
 * S06: dữ liệu trang chi tiết đơn, gộp từ 3 nguồn. lines là bản copy không sửa được.
 *
 * @throws IllegalArgumentException khi một field null
 */
public record OrderDetail(String orderId, PaymentInfo payment, ShipmentInfo shipment, List<OrderLine> lines) {
    public OrderDetail {
        throw new UnsupportedOperationException("TODO S06");
    }
}
