package com.training.ecommerce.application;

/**
 * S06: không tải được chi tiết đơn (một nguồn lỗi, hoặc thread gọi bị interrupt). Exception gốc nằm ở {@link #getCause()}.
 */
public class OrderDetailLoadException extends RuntimeException {

    public OrderDetailLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
