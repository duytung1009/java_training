package com.training.ecommerce.interfaces.rest.catalog;

import com.training.ecommerce.domain.catalog.BundleProduct;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.ProductType;
import com.training.ecommerce.domain.catalog.SubscriptionProduct;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Chuyển {@code Product} domain sang {@link ProductResponse}. Request → domain do {@code ProductFactory} (application) lo.
 */
final class ProductMapper {

    private ProductMapper() {
    }

    static ProductResponse toResponse(Product product) {
        ProductType type = ProductType.of(product);
        BigDecimal finalPrice = product.calculateFinalPrice();
        String category = product.getCategory().name();
        return switch (product) {
            case PhysicalProduct p -> new ProductResponse(p.getId(), type, p.getName(), p.getBasePrice(), finalPrice,
                    p.getStock(), category, p.getWeightKg(), null, null, null);
            case SubscriptionProduct p -> new ProductResponse(p.getId(), type, p.getName(), p.getBasePrice(), finalPrice,
                    p.getStock(), category, null, p.getMonths(), null, null);
            case BundleProduct p -> new ProductResponse(p.getId(), type, p.getName(), p.getBasePrice(), finalPrice,
                    p.getStock(), category, null, null, p.getDiscountRate(),
                    Arrays.stream(p.getItems()).map(Product::getId).toList());
            default -> new ProductResponse(product.getId(), type, product.getName(), product.getBasePrice(), finalPrice,
                    product.getStock(), category, null, null, null, null);
        };
    }
}
