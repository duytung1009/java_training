package com.training.ecommerce.infrastructure.config;

import com.training.ecommerce.application.inventory.StockStore;
import com.training.ecommerce.application.shipping.ShipmentRepository;
import com.training.ecommerce.domain.catalog.BundleProduct;
import com.training.ecommerce.domain.catalog.Category;
import com.training.ecommerce.domain.catalog.DigitalProduct;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.SubscriptionProduct;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.infrastructure.persistence.inmemory.InMemoryRepository;
import com.training.ecommerce.infrastructure.persistence.inmemory.InMemoryShipmentRepository;
import com.training.ecommerce.infrastructure.persistence.inmemory.InMemoryStockStore;
import com.training.ecommerce.shared.repository.Repository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.util.List;

/**
 * Profile {@code local}: chạy không cần database, dữ liệu mất khi tắt ứng dụng.
 */
@Configuration
@Profile("local")
public class InMemoryPersistenceConfig {

    @Bean
    Repository<Product, String> productRepository() {
        return new InMemoryRepository<>();
    }

    @Bean
    Repository<Order, String> orderRepository() {
        return new InMemoryRepository<>();
    }

    @Bean
    ShipmentRepository shipmentRepository() {
        return new InMemoryShipmentRepository();
    }

    @Bean
    StockStore stockStore(Repository<Product, String> products) {
        return new InMemoryStockStore(products);
    }

    /** Nạp cùng dữ liệu mẫu với {@code data.sql} để hai profile cho trải nghiệm giống nhau. */
    @Bean
    CommandLineRunner localDataSeeder(Repository<Product, String> products) {
        return args -> {
            Product tshirt = new PhysicalProduct("P-TSHIRT", "Áo thun nam", new BigDecimal("150000"), 50,
                    new BigDecimal("0.5"), Category.FASHION);
            Product ebook = new DigitalProduct("P-EBOOK", "Ebook Java", new BigDecimal("99000"), 100, Category.SOFTWARE);
            Product news = new SubscriptionProduct("P-NEWS", "Gói tin tức", new BigDecimal("30000"), 12, 20,
                    Category.SUBSCRIPTION);
            Product combo = new BundleProduct("P-COMBO", "Combo áo + ebook", 10, new BigDecimal("0.1"), tshirt, ebook);
            products.saveAll(List.of(tshirt, ebook, news, combo));
        };
    }
}
