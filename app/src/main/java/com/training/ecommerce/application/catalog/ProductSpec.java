package com.training.ecommerce.application.catalog;

import com.training.ecommerce.domain.catalog.ProductType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mô tả một product cần tạo, độc lập nguồn dữ liệu (request REST, dòng CSV...). Field bắt buộc phụ thuộc
 * {@link #type()}, xem {@link ValidProductSpec}; field không thuộc loại đó bị bỏ qua.
 */
public interface ProductSpec {

    ProductType type();

    String name();

    BigDecimal basePrice();

    int stock();

    /** Tên {@code Category} (không phân biệt hoa thường); null / rỗng = {@code OTHER}. */
    String category();

    /** PHYSICAL: cân nặng kg. */
    BigDecimal weightKg();

    /** SUBSCRIPTION: số tháng. */
    Integer months();

    /** BUNDLE: tỉ lệ giảm trong [0, 1]. */
    BigDecimal discountRate();

    /** BUNDLE: id các item, theo thứ tự. */
    List<String> itemIds();
}
