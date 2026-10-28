package com.training.ecommerce.application;

import com.training.ecommerce.domain.order.OrderLine;

import java.util.List;

/** S06: nguồn các dòng của đơn (bản thật đọc DB, có độ trễ). */
@FunctionalInterface
public interface OrderLineSource {
    List<OrderLine> findLines(String orderId);
}
