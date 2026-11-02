package com.training.ecommerce.visualizer.core.trace.fake;

/** Event domain giả: đã gửi tiền vào tài khoản. */
public record MoneyDeposited(String account, long amount) {
}
