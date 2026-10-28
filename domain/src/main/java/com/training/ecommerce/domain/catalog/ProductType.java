package com.training.ecommerce.domain.catalog;

/**
 * Loại product, dùng khi lưu trữ / trả về cho client để biết dựng lại đúng subtype.
 * Tách thành enum riêng thay vì thêm method vào {@link Product}, để subtype tự định nghĩa (ví dụ trong test) không bị ép phải cài thêm.
 */
public enum ProductType {
    PHYSICAL,
    DIGITAL,
    SUBSCRIPTION,
    BUNDLE;

    /**
     * @throws IllegalArgumentException khi product null hoặc là subtype chưa được hỗ trợ
     */
    public static ProductType of(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("product không được null");
        }
        return switch (product) {
            case PhysicalProduct p -> PHYSICAL;
            case DigitalProduct p -> DIGITAL;
            case SubscriptionProduct p -> SUBSCRIPTION;
            case BundleProduct p -> BUNDLE;
            default -> throw new IllegalArgumentException("Loại product chưa hỗ trợ: " + product.getClass().getSimpleName());
        };
    }
}
