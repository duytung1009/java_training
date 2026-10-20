package com.training.ecommerce.domain.catalog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

/**
 * Combo nhiều sản phẩm (có thể chứa bundle khác), bán với tỉ lệ giảm riêng.
 * Thêm class này mà không phải sửa PricingEngine (OCP).
 */
public class BundleProduct extends Product {
    private final Product[] items;
    private final BigDecimal discountRate;

    /**
     * basePrice của bundle = tổng giá bán của các item (trước khi giảm giá combo).
     */
    public BundleProduct(String id, String name, int stock, BigDecimal discountRate, Product... items) {
        super(id, name, totalFinalPrice(items), stock);
        if (discountRate == null || discountRate.signum() < 0 || discountRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("discountRate phải trong [0, 1]");
        }
        this.items = Arrays.copyOf(items, items.length);
        this.discountRate = discountRate;
    }

    /** S05: kèm category (đứng trước varargs items). */
    public BundleProduct(String id, String name, int stock, BigDecimal discountRate, Category category, Product... items) {
        this(id, name, stock, discountRate, items);
        // TODO S05: constructor này chưa truyền được category lên Product (đang gọi this(...) bản cũ).
        //  Gợi ý: cho constructor cũ gọi this(..., Category.OTHER), còn constructor này gọi super(..., category).
    }

    @Override
    public BigDecimal calculateFinalPrice() {
        return getBasePrice().multiply(BigDecimal.ONE.subtract(discountRate)).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public Product[] getItems() {
        return Arrays.copyOf(items, items.length);
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    private static BigDecimal totalFinalPrice(Product[] items) {
        if (items == null || items.length == 0) {
            throw new IllegalArgumentException("Bundle phải có ít nhất 1 item");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Product item : items) {
            if (item == null) {
                throw new IllegalArgumentException("Item không được null");
            }
            total = total.add(item.calculateFinalPrice());
        }
        return total;
    }
}
