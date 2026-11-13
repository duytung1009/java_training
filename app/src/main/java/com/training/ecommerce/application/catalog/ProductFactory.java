package com.training.ecommerce.application.catalog;

import com.training.ecommerce.domain.catalog.BundleProduct;
import com.training.ecommerce.domain.catalog.Category;
import com.training.ecommerce.domain.catalog.DigitalProduct;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.SubscriptionProduct;

import java.util.function.Function;

/**
 * Dựng {@link Product} domain từ {@link ProductSpec}. Là API công khai của application nên giữ vài guard phòng thủ
 * (type, months, itemIds) cho caller không đi qua Bean Validation.
 */
public final class ProductFactory {

    private ProductFactory() {
    }

    /**
     * @param spec       mô tả product
     * @param id         id dùng cho product
     * @param itemLookup tra item của bundle theo id (ném {@code ProductNotFoundException} khi thiếu)
     * @throws IllegalArgumentException khi spec null, thiếu / sai field theo loại, category không hợp lệ, hoặc domain từ chối giá trị
     */
    public static Product create(ProductSpec spec, String id, Function<String, Product> itemLookup) {
        if (spec == null) {
            throw new IllegalArgumentException("spec không được null");
        }
        if (spec.type() == null) {
            throw new IllegalArgumentException("type không được rỗng");
        }
        Category category = parseCategory(spec.category());
        return switch (spec.type()) {
            case PHYSICAL -> new PhysicalProduct(id, spec.name(), spec.basePrice(), spec.stock(),
                    spec.weightKg(), category);
            case DIGITAL -> new DigitalProduct(id, spec.name(), spec.basePrice(), spec.stock(), category);
            case SUBSCRIPTION -> {
                if (spec.months() == null) {
                    throw new IllegalArgumentException("months không được null với SUBSCRIPTION");
                }
                yield new SubscriptionProduct(id, spec.name(), spec.basePrice(), spec.months(),
                        spec.stock(), category);
            }
            case BUNDLE -> {
                if (spec.itemIds() == null || spec.itemIds().isEmpty()) {
                    throw new IllegalArgumentException("Bundle phải có ít nhất 1 item");
                }
                Product[] items = spec.itemIds().stream().map(itemLookup).toArray(Product[]::new);
                yield new BundleProduct(id, spec.name(), spec.stock(), spec.discountRate(), category, items);
            }
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
