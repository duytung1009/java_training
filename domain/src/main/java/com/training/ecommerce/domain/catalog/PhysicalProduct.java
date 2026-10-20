package com.training.ecommerce.domain.catalog;

import com.training.ecommerce.domain.pricing.Discountable;
import com.training.ecommerce.domain.shipping.Shippable;
import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.domain.pricing.DiscountCode;

import java.time.LocalDate;

public class PhysicalProduct extends Product implements Shippable, Discountable {
    private DiscountCode code;
    private Address dest;

    public PhysicalProduct(String id, String name, double basePrice) {
        super(id, name, basePrice);
    }

    public PhysicalProduct(String id, String name, double basePrice, DiscountCode code, Address dest) {
        this(id, name, basePrice);
        this.code = code;
        this.dest = dest;
    }

    @Override
    public double calculateFinalPrice() {
        double discount = applyDiscount(code);
        double shipping = calculateShipping(dest);
        return basePrice + shipping - discount;
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
    public double calculateShipping(Address dest) {
        if (dest == null) {
            return 0;
        }
        return dest.shippingPrice;
    }

    @Override
    public String info() {
        return String.format("%-20s %-2s - %-20s %12.0fđ", "PhysicalProduct", id, name, basePrice);
    }

    @Override
    public String calculateFinalPriceStep() {
        double discount = applyDiscount(code);
        double shipping = calculateShipping(dest);
        return String.format("%-20sđ + %-20sđ - %-20sđ = %12.0fđ", basePrice, shipping, discount, calculateFinalPrice());
    }
}
