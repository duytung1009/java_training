package com.training.ecommerce.infrastructure.fake;

import com.training.ecommerce.application.payment.PaymentGateway;
import com.training.ecommerce.application.payment.PaymentInfo;


import java.math.BigDecimal;
import java.time.Duration;

/** S06: cổng thanh toán giả, trả "PAID" sau một khoảng trễ. Dùng cho demo. */
public class FakePaymentGateway implements PaymentGateway {
    private final Duration delay;
    private final BigDecimal amount;

    public FakePaymentGateway(Duration delay, BigDecimal amount) {
        this.delay = delay;
        this.amount = amount;
    }

    @Override
    public PaymentInfo findPayment(String orderId) {
        SimulatedLatency.await(delay);
        return new PaymentInfo("PAID", amount);
    }
}
