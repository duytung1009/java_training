package com.training.ecommerce.domain.pricing;


public interface Discountable {
    double applyDiscount(DiscountCode code);
}
