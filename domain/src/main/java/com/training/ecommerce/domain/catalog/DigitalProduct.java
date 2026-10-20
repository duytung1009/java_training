package com.training.ecommerce.domain.catalog;

import com.training.ecommerce.domain.pricing.Discountable;
import com.training.ecommerce.domain.pricing.DiscountCode;

import java.time.LocalDate;

public class DigitalProduct extends Product implements Discountable {
    private DiscountCode code;

    public DigitalProduct(String id, String name, double basePrice) {
        super(id, name, basePrice);
    }

    public DigitalProduct(String id, String name, double basePrice, DiscountCode code) {
        this(id, name, basePrice);
        this.code = code;
    }

    @Override
    public double calculateFinalPrice() {
        double discount = applyDiscount(code);
        return basePrice - discount;
    }

    @Override
    public double applyDiscount(DiscountCode code) {
        if (code == null) {
            return 0;
        }
        return code.expiredAt.isAfter(LocalDate.now())
                ? basePrice * code.discountRate
                : 0;
    }

    @Override
    public String info() {
        return String.format("%-20s %-2s - %-20s %12.0fđ", "DigitalProduct", id, name, basePrice);
    }

    @Override
    public String calculateFinalPriceStep() {
        double discount = applyDiscount(code);
        return String.format("%-20sđ - %-20sđ = %12.0fđ", basePrice, discount, calculateFinalPrice());
    }
}
