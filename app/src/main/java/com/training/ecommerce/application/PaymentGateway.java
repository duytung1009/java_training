package com.training.ecommerce.application;

/** S06: nguồn thông tin thanh toán của đơn (bản thật gọi cổng thanh toán, có độ trễ). */
@FunctionalInterface
public interface PaymentGateway {
    PaymentInfo findPayment(String orderId);
}
