package com.training.ecommerce.visualizer.world;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Định dạng tiền VND cho hiển thị. Không có state. */
public final class MoneyFormat {

    private static final BigDecimal MILLION = new BigDecimal("1000000");
    private static final BigDecimal THOUSAND = new BigDecimal("1000");

    private MoneyFormat() {
    }

    /**
     * Dạng đầy đủ, nhóm nghìn bằng dấu cách, vd. "19 440 000".
     *
     * @throws IllegalArgumentException nếu amount null
     */
    public static String full(BigDecimal amount) {
        require(amount);
        long value = amount.setScale(0, RoundingMode.HALF_UP).longValue();
        return String.format(Locale.ROOT, "%,d", value).replace(',', ' ');
    }

    /**
     * Dạng rút gọn, vd. "19.4tr", "27.5k", "800".
     *
     * @throws IllegalArgumentException nếu amount null
     */
    public static String shortOf(BigDecimal amount) {
        require(amount);
        BigDecimal abs = amount.abs();
        if (abs.compareTo(MILLION) >= 0) {
            return oneDecimal(amount.divide(MILLION, 4, RoundingMode.HALF_UP)) + "tr";
        }
        if (abs.compareTo(THOUSAND) >= 0) {
            return oneDecimal(amount.divide(THOUSAND, 4, RoundingMode.HALF_UP)) + "k";
        }
        return amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Tỉ lệ phần trăm, vd. 0.10 → "10%".
     *
     * @throws IllegalArgumentException nếu rate null
     */
    public static String percent(BigDecimal rate) {
        require(rate);
        BigDecimal pct = rate.movePointRight(2).stripTrailingZeros();
        return (pct.scale() < 0 ? pct.setScale(0) : pct).toPlainString() + "%";
    }

    private static String oneDecimal(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    private static void require(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("amount không được null");
        }
    }
}
