package com.training.ecommerce.visualizer.core.trace.fake;

/** Event domain giả: khoản gửi lớn (mapper test không ánh xạ event này). */
public record LargeDeposit(String account, long amount) {
}
