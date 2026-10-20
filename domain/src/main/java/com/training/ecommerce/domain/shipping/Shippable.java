package com.training.ecommerce.domain.shipping;

import com.training.ecommerce.domain.customer.Address;

import java.math.BigDecimal;

/**
 * Sản phẩm cần giao hàng vật lý.
 */
public interface Shippable {

    /**
     * @return phí ship, scale 2, HALF_UP
     * @throws IllegalArgumentException khi address null
     */
    BigDecimal calculateShipping(Address address);
}
