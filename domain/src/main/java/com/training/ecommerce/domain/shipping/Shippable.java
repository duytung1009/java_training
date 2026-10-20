package com.training.ecommerce.domain.shipping;

import com.training.ecommerce.domain.customer.Address;

import java.math.BigDecimal;

/**
 * Sản phẩm cần giao hàng vật lý.
 */
public interface Shippable {

    /**
     * @return phí ship: 20.000 nếu city là "Hà Nội", ngược lại 35.000; cộng 5.000 × weightKg; scale 2, HALF_UP
     * @throws IllegalArgumentException khi address null
     */
    BigDecimal calculateShipping(Address address);
}
