package com.training.ecommerce.domain.pricing;

import java.math.BigDecimal;
import java.util.List;

/**
 * Một quy tắc giá: nhận giá, trả về giá mới. Ghép nhiều quy tắc bằng {@link #andThen} hoặc {@link #chain}.
 */
@FunctionalInterface
public interface PricingRule {

    BigDecimal apply(BigDecimal price);

    /**
     * @return rule chạy {@code this} trước rồi tới {@code next}
     * @throws IllegalArgumentException khi next null
     */
    default PricingRule andThen(PricingRule next) {
        throw new UnsupportedOperationException("TODO S05");
    }

    /** @return rule trả nguyên giá */
    static PricingRule identity() {
        throw new UnsupportedOperationException("TODO S05");
    }

    /**
     * Giảm theo tỉ lệ: price × (1 − rate), scale 2, HALF_UP.
     *
     * @throws IllegalArgumentException khi rate null hoặc ngoài (0, 1]
     */
    static PricingRule percentOff(BigDecimal rate) {
        throw new UnsupportedOperationException("TODO S05");
    }

    /**
     * Trừ một khoản cố định, không bao giờ âm: max(price − amount, 0), scale 2.
     *
     * @throws IllegalArgumentException khi amount null hoặc âm
     */
    static PricingRule amountOff(BigDecimal amount) {
        throw new UnsupportedOperationException("TODO S05");
    }

    /**
     * Gộp danh sách rule theo thứ tự: reduce(identity, andThen). List rỗng → identity.
     *
     * @throws IllegalArgumentException khi rules null hoặc chứa null
     */
    static PricingRule chain(List<PricingRule> rules) {
        throw new UnsupportedOperationException("TODO S05");
    }
}
