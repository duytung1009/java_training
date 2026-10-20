package com.training.ecommerce.domain.shipping;

import com.training.ecommerce.domain.customer.Address;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Vùng giao hàng và biểu phí ship: phí cơ bản theo vùng + phí theo cân nặng.
 */
public enum ShippingZone {
    INNER_CITY(new BigDecimal("20000")),
    OTHER_CITY(new BigDecimal("35000"));

    private static final String INNER_CITY_NAME = "Hà Nội";
    private static final BigDecimal FEE_PER_KG = new BigDecimal("5000");

    private final BigDecimal baseFee;

    ShippingZone(BigDecimal baseFee) {
        this.baseFee = baseFee;
    }

    /**
     * @throws IllegalArgumentException khi address null
     */
    public static ShippingZone of(Address address) {
        if (address == null) {
            throw new IllegalArgumentException("address không được null");
        }
        return INNER_CITY_NAME.equals(address.getCity()) ? INNER_CITY : OTHER_CITY;
    }

    /** baseFee + FEE_PER_KG × weightKg, scale 2, HALF_UP. */
    public BigDecimal feeFor(BigDecimal weightKg) {
        return baseFee.add(FEE_PER_KG.multiply(weightKg)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getBaseFee() {
        return baseFee;
    }
}
