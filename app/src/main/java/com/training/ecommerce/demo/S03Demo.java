package com.training.ecommerce.demo;

import com.training.ecommerce.application.pricing.PricingEngine;
import com.training.ecommerce.domain.catalog.BundleProduct;
import com.training.ecommerce.domain.catalog.DigitalProduct;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.SubscriptionProduct;
import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.domain.pricing.DiscountCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Demo S03: list đủ các loại product → in giá từng loại và tổng doanh thu.
 */
public class S03Demo {

    public static void main(String[] args) {
        Address duyTan = new Address("FPT 17 Duy Tân", "Hà Nội");
        Address hoaLac = new Address("FPT Hoà Lạc", "Hà Nội");
        Address q1 = new Address("12 Nguyễn Huệ, Quận 1", "Hồ Chí Minh");
        DiscountCode code25 = new DiscountCode("SALE25", new BigDecimal("0.25"));

        PhysicalProduct iphone = new PhysicalProduct("01", "Iphone 17", new BigDecimal("15000000"), 10, new BigDecimal("0.2"));
        PhysicalProduct galaxy = new PhysicalProduct("02", "Galaxy S25", new BigDecimal("10000000"), 10, new BigDecimal("0.2"));
        DigitalProduct windows = new DigitalProduct("03", "Windows", new BigDecimal("5000000"), 100);
        DigitalProduct office = new DigitalProduct("04", "Office 2021", new BigDecimal("1000000"), 100);
        Product claudeMax = new SubscriptionProduct("05", "Claude Max", new BigDecimal("3000000"), 12, 100);
        Product codex = new SubscriptionProduct("06", "Codex", new BigDecimal("500000"), 1, 100);
        Product workKit = new BundleProduct("07", "Combo Windows + Office", 50, new BigDecimal("0.10"), windows, office);

        List<Product> products = new ArrayList<>();
        products.add(iphone);
        products.add(galaxy);
        products.add(windows);
        products.add(office);
        products.add(claudeMax);
        products.add(codex);
        products.add(workKit);

        for (Product product : products) {
            System.out.println(String.format(Locale.ROOT, "%-20s %-3s %-25s %15.2f",
                    product.getClass().getSimpleName(), product.getId(), product.getName(), product.calculateFinalPrice()));
        }

        System.out.println();
        System.out.println(String.format(Locale.ROOT, "Galaxy S25 với %s: %.2f", code25.getCode(), galaxy.applyDiscount(code25)));
        System.out.println(String.format(Locale.ROOT, "Office 2021 với %s: %.2f", code25.getCode(), office.applyDiscount(code25)));
        System.out.println(String.format(Locale.ROOT, "Ship Iphone 17 tới %s: %.2f", duyTan.getStreet(), iphone.calculateShipping(duyTan)));
        System.out.println(String.format(Locale.ROOT, "Ship Iphone 17 tới %s: %.2f", hoaLac.getStreet(), iphone.calculateShipping(hoaLac)));
        System.out.println(String.format(Locale.ROOT, "Ship Iphone 17 tới %s: %.2f", q1.getStreet(), iphone.calculateShipping(q1)));

        BigDecimal revenue = new PricingEngine().calculateRevenue(products);
        System.out.println();
        System.out.println(String.format(Locale.ROOT, "Revenue %15.2f", revenue));
    }
}
