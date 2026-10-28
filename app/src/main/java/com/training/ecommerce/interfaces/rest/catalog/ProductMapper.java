package com.training.ecommerce.interfaces.rest.catalog;

import com.training.ecommerce.domain.catalog.BundleProduct;
import com.training.ecommerce.domain.catalog.Category;
import com.training.ecommerce.domain.catalog.DigitalProduct;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.ProductType;
import com.training.ecommerce.domain.catalog.SubscriptionProduct;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.function.Function;

/**
 * Chuyển đổi giữa {@link ProductRequest} / {@link ProductResponse} và domain, để controller không chứa logic mapping.
 */
final class ProductMapper {

    private ProductMapper() {
    }

    /**
     * @param id         id dùng cho product (đã chọn xong giữa path và body)
     * @param itemLookup tra item của bundle theo id (ném {@code ProductNotFoundException} khi thiếu)
     * @throws IllegalArgumentException khi thiếu / sai field theo loại, hoặc domain từ chối giá trị
     */
    static Product toDomain(ProductRequest request, String id, Function<String, Product> itemLookup) {
        if (request.type() == null) {
            throw new IllegalArgumentException("type không được rỗng");
        }
        Category category = parseCategory(request.category());
        return switch (request.type()) {
            case PHYSICAL -> new PhysicalProduct(id, request.name(), request.basePrice(), request.stock(),
                    request.weightKg(), category);
            case DIGITAL -> new DigitalProduct(id, request.name(), request.basePrice(), request.stock(), category);
            case SUBSCRIPTION -> {
                if (request.months() == null) {
                    throw new IllegalArgumentException("months không được null với SUBSCRIPTION");
                }
                yield new SubscriptionProduct(id, request.name(), request.basePrice(), request.months(),
                        request.stock(), category);
            }
            case BUNDLE -> {
                if (request.itemIds() == null || request.itemIds().isEmpty()) {
                    throw new IllegalArgumentException("Bundle phải có ít nhất 1 item");
                }
                Product[] items = request.itemIds().stream().map(itemLookup).toArray(Product[]::new);
                yield new BundleProduct(id, request.name(), request.stock(), request.discountRate(), category, items);
            }
        };
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

    private static Category parseCategory(String category) {
        if (category == null || category.isBlank()) {
            return Category.OTHER;
        }
        try {
            return Category.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("category không hợp lệ: " + category, e);
        }
    }
}
