package com.training.ecommerce.domain.catalog;

public class SubscriptionProduct extends Product {
    public SubscriptionProduct(String id, String name, double basePrice) {
        super(id, name, basePrice);
    }

    @Override
    public double calculateFinalPrice() {
        return this.getBasePrice();
    }

    @Override
    public String info() {
        return String.format("%-20s %-2s - %-20s %12.0fđ", "SubscriptionProduct", id, name, basePrice);
    }

    @Override
    public String calculateFinalPriceStep() {
        return String.format("%-20sđ = %12.0fđ", basePrice, calculateFinalPrice());
    }
}
