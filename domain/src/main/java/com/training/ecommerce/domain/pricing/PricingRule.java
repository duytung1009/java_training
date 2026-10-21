package com.training.ecommerce.domain.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

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
        // 1. Validate
        if (next == null) {
            throw new IllegalArgumentException("next không được null");
        }
        // 2. Trả về rule mới: lambda capture cả this lẫn next. Chưa tính gì ở đây (lazy),
        //    chỉ khi gọi apply(price) mới chạy this trước, lấy kết quả đưa vào next.
        return price -> next.apply(this.apply(price));
    }

    /** @return rule trả nguyên giá */
    static PricingRule identity() {
        return price -> price;
    }

    /**
     * Giảm theo tỉ lệ: price × (1 − rate), scale 2, HALF_UP.
     *
     * @throws IllegalArgumentException khi rate null hoặc ngoài (0, 1]
     */
    static PricingRule percentOff(BigDecimal rate) {
        // 1. Validate
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("rate phải trong khoảng (0, 1], nhận: " + rate);
        }
        // 2. Trả về một lambda
        return price -> price.multiply(BigDecimal.ONE.subtract(rate))
                .setScale(2, RoundingMode.HALF_UP);        // tiền: scale 2
    }

    /**
     * Trừ một khoản cố định, không bao giờ âm: max(price − amount, 0), scale 2.
     *
     * @throws IllegalArgumentException khi amount null hoặc âm
     */
    static PricingRule amountOff(BigDecimal amount) {
        // 1. Validate ngay khi tạo rule (fail fast), không đợi tới lúc apply
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("amount phải >= 0, nhận: " + amount);
        }
        // 2. Trả về một lambda: nó "nhớ" amount (biến effectively final được capture)
        return price -> price.subtract(amount)
                .max(BigDecimal.ZERO)                               // không bao giờ âm (D6)
                .setScale(2, RoundingMode.HALF_UP);        // tiền: scale 2
    }

    /**
     * Gộp danh sách rule theo thứ tự: reduce(identity, andThen). List rỗng → identity.
     *
     * @throws IllegalArgumentException khi rules null hoặc chứa null
     */
    static PricingRule chain(List<PricingRule> rules) {
        // 1. Validate
        if (rules == null || rules.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("rules không được phép null hoặc chứa null");
        }
        // 2. Gộp danh sách
        return rules.stream().reduce(identity(), PricingRule::andThen);
    }
}
