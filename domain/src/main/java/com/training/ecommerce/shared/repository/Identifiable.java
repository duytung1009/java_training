package com.training.ecommerce.shared.repository;

/**
 * Đối tượng có định danh, dùng làm khóa trong {@link Repository}.
 *
 * @param <ID> kiểu của id (ví dụ String, Long)
 */
public interface Identifiable<ID> {

    ID getId();
}
