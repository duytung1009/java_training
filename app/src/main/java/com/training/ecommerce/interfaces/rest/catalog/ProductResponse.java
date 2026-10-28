package com.training.ecommerce.interfaces.rest.catalog;

import com.training.ecommerce.domain.catalog.ProductType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product trả về cho client. Field riêng của từng loại là null với loại khác.
 */
public record ProductResponse(
        String id,
        ProductType type,
        String name,
        BigDecimal basePrice,
        BigDecimal finalPrice,
        int stock,
        String category,
        BigDecimal weightKg,
        Integer months,
        BigDecimal discountRate,
        List<String> itemIds) {
}
