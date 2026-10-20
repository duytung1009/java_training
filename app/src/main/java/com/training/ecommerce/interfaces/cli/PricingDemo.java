package com.training.ecommerce.interfaces.cli;


import com.training.ecommerce.domain.catalog.DigitalProduct;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.SubscriptionProduct;
import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.domain.pricing.DiscountCode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PricingDemo {

    public static void main(String[] args) {
        Address destDuyTan = new Address("FPT 17 Duy Tân", 150000);
        Address destHoaLac = new Address("FPT Hoà Lạc", 500000);
        DiscountCode code25 = new DiscountCode("25%", 0.25, LocalDate.parse("2026-12-31"));
        DiscountCode code50 = new DiscountCode("50%", 0.5, LocalDate.parse("2026-12-31"));

        List<Product> products = new ArrayList<>();
        products.add(new PhysicalProduct("01", "Iphone 17", 15000000));
        products.add(new PhysicalProduct("02", "Galaxy S25", 10000000, code25, destDuyTan));
        products.add(new DigitalProduct("03", "Windows", 5000000));
        products.add(new DigitalProduct("04", "Office 2021", 1000000, code25));
        products.add(new SubscriptionProduct("05", "Claude Max", 3000000));
        products.add(new SubscriptionProduct("06", "Codex", 500000));

        for (Product p : products) {
            IO.println(p.info());
            IO.println(p.calculateFinalPriceStep());
        }

        double revenue = PricingEngine.calculateRevenue(products);
        IO.println(String.format("Revenue %12.0fđ", revenue));
    }
}
