package com.training.ecommerce.domain.catalog;

import com.training.ecommerce.domain.pricing.Discountable;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Sản phẩm số: áp dụng được mã giảm giá, không giao hàng (không Shippable).
 */
public class DigitalProduct extends Product implements Discountable {

    public DigitalProduct(String id, String name, BigDecimal basePrice, int stock) {
        super(id, name, basePrice, stock);
    }

    /** S05: kèm category. */
    public DigitalProduct(String id, String name, BigDecimal basePrice, int stock, Category category) {
        this(id, name, basePrice, stock);
        // TODO S05: constructor này chưa truyền được category lên Product (đang gọi this(...) bản cũ).
        //  Gợi ý: cho constructor cũ gọi this(..., Category.OTHER), còn constructor này gọi super(..., category).
    }

    @Override
    public BigDecimal calculateFinalPrice() {

        return getBasePrice().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
