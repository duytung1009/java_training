package com.training.ecommerce.visualizer.core.model;

import java.util.List;

/** Mô hình không qua được {@link DomainModelValidator}. */
public class InvalidModelException extends RuntimeException {

    private final List<ModelError> errors;

    /**
     * @throws IllegalArgumentException nếu errors null hoặc rỗng
     */
    public InvalidModelException(List<ModelError> errors) {
        super("Mô hình không hợp lệ: " + (errors == null ? 0 : errors.size()) + " lỗi");
        if (errors == null || errors.isEmpty()) {
            throw new IllegalArgumentException("errors không được null hoặc rỗng");
        }
        this.errors = List.copyOf(errors);
    }

    public List<ModelError> errors() {
        return errors;
    }
}
