package com.training.ecommerce.visualizer.core.model;

/** Không ghi được file mô hình (thư mục không tạo được, không có quyền, ổ đầy...). */
public class ModelNotWritableException extends RuntimeException {

    public ModelNotWritableException(String message, Throwable cause) {
        super(message, cause);
    }
}
