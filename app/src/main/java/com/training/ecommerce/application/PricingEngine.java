package com.training.ecommerce.application;

import com.training.ecommerce.domain.catalog.Product;

import java.util.List;

public class PricingEngine {
    public static double calculateRevenue(List<Product> products) {
        double revenue = 0;
        for (Product p : products) {
            revenue += p.calculateFinalPrice();
        }
        return revenue;
    }
}
