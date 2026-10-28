package com.training.ecommerce.application;

import java.math.BigDecimal;

/**
 * S06: thông tin thanh toán.
 *
 * @throws IllegalArgumentException khi status hoặc amount null
 */
public record PaymentInfo(String status, BigDecimal amount) {
    public PaymentInfo {
        if (status == null || amount == null) {
            throw new IllegalArgumentException("status, amount không được null");
        }
    }
}
