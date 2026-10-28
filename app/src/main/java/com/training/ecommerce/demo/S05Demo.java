package com.training.ecommerce.demo;

import com.training.ecommerce.application.pricing.PricingEngine;
import com.training.ecommerce.application.report.ReportService;
import com.training.ecommerce.domain.catalog.Category;
import com.training.ecommerce.domain.catalog.DigitalProduct;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.SubscriptionProduct;
import com.training.ecommerce.domain.inventory.InventoryManager;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;
import com.training.ecommerce.domain.pricing.PricingRule;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Demo S05: lambda / Predicate / Optional, PricingRule ghép bằng andThen + chain, 3 báo cáo bằng Stream.
 */
public class S05Demo {

    public static void main(String[] args) {
        Product tshirt = new PhysicalProduct("P001", "Áo thun nam", new BigDecimal("150000"), 50, new BigDecimal("0.3"), Category.FASHION);
        Product jeans = new PhysicalProduct("P002", "Quần jean", new BigDecimal("450000"), 20, new BigDecimal("0.8"), Category.FASHION);
        Product mouse = new PhysicalProduct("E001", "Chuột không dây", new BigDecimal("250000"), 0, new BigDecimal("0.1"), Category.ELECTRONICS);
        Product office = new DigitalProduct("D001", "Office 2024 bản quyền", new BigDecimal("1000000"), 200, Category.SOFTWARE);
        Product claudePro = new SubscriptionProduct("S001", "Gói Claude Pro", new BigDecimal("499000"), 12, 100, Category.SUBSCRIPTION);

        InventoryManager inventory = new InventoryManager();
        for (Product product : List.of(tshirt, jeans, mouse, office, claudePro)) {
            inventory.addProduct(product);                   // side effect: vòng for cho rõ ý
        }

        // 1. Optional thay cho null
        System.out.println(">>> Tìm P001: " + inventory.findById("P001").map(Product::getName).orElse("không tồn tại"));
        System.out.println(">>> Tìm P999: " + inventory.findById("P999").map(Product::getName).orElse("không tồn tại"));

        // 2. Predicate: ghép điều kiện bằng and / negate
        Predicate<Product> inStock = p -> p.getStock() > 0;
        Predicate<Product> fashion = p -> p.getCategory() == Category.FASHION;
        System.out.println(">>> Thời trang còn hàng: " + getNames(inventory.search(fashion.and(inStock))));
        System.out.println(">>> Hết hàng: " + getNames(inventory.search(inStock.negate())));

        // 3. PricingRule: mỗi quy tắc là một hàm, ghép theo thứ tự
        PricingRule member = PricingRule.percentOff(new BigDecimal("0.05"));     // thành viên −5%
        PricingRule flashSale = PricingRule.amountOff(new BigDecimal("20000"));  // flash sale −20.000đ
        BigDecimal price = tshirt.calculateFinalPrice();
        System.out.println(">>> Giá áo " + price
                + " | member rồi flash sale: " + member.andThen(flashSale).apply(price)
                + " | flash sale rồi member: " + flashSale.andThen(member).apply(price));

        PricingRule campaign = PricingRule.chain(List.of(member, flashSale, p -> p.max(new BigDecimal("130000.00")))); // lambda: giá sàn 130.000 (ghi .00: max trả nguyên object, kể cả scale)
        System.out.println(">>> Áo qua chuỗi campaign (có giá sàn): " + campaign.apply(price));

        PricingEngine engine = new PricingEngine();
        List<Product> catalog = inventory.search(p -> true);
        System.out.println(">>> Doanh thu niêm yết: " + engine.calculateRevenue(catalog)
                + " | sau campaign: " + engine.calculateRevenue(catalog, campaign));

        // 4. Báo cáo trên 3 đơn
        Order o1 = new Order(new BigDecimal("0.10"), new BigDecimal("0.08"));
        o1.addLine(new OrderLine(tshirt, 2));
        o1.addLine(new OrderLine(office, 1));
        Order o2 = new Order(BigDecimal.ZERO, BigDecimal.ZERO);
        o2.addLine(new OrderLine(jeans, 1));
        o2.addLine(new OrderLine(tshirt, 3));
        Order o3 = new Order(BigDecimal.ZERO, new BigDecimal("0.08"));
        o3.addLine(new OrderLine(claudePro, 1));
        List<Order> orders = List.of(o1, o2, o3);

        ReportService report = new ReportService();
        System.out.println(">>> Doanh thu theo category:");
        report.revenueByCategory(orders).forEach((category, revenue) -> System.out.println("    " + category + ": " + revenue));

        System.out.println(">>> Top 2 bán chạy: " + getNames(report.topSellingProducts(orders, 2)));

        Map<Boolean, List<Order>> bySize = report.partitionByFinalAmount(orders, new BigDecimal("1000000"));
        System.out.println(">>> Đơn ≥ 1.000.000: " + bySize.get(true).stream().map(Order::finalAmount).toList()
                + " | đơn nhỏ: " + bySize.get(false).stream().map(Order::finalAmount).toList());
    }

    private static List<String> getNames(List<Product> products) {
        return products.stream().map(Product::getName).toList();
    }
}
