package com.training.ecommerce.infrastructure.config;

import com.training.ecommerce.application.inventory.StockStore;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.infrastructure.persistence.jdbc.JdbcOrderRepository;
import com.training.ecommerce.infrastructure.persistence.jdbc.JdbcProductRepository;
import com.training.ecommerce.infrastructure.persistence.jdbc.JdbcStockStore;
import com.training.ecommerce.shared.repository.Repository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Profile {@code dev}: lưu trữ trên PostgreSQL qua JDBC. Class persistence không có annotation Spring,
 * mọi wiring nằm ở đây.
 */
@Configuration
@Profile("dev")
public class JdbcPersistenceConfig {

    @Bean
    Repository<Product, String> productRepository(JdbcClient jdbc) {
        return new JdbcProductRepository(jdbc);
    }

    @Bean
    Repository<Order, String> orderRepository(JdbcClient jdbc, Repository<Product, String> products) {
        return new JdbcOrderRepository(jdbc, products);
    }

    @Bean
    StockStore stockStore(JdbcClient jdbc) {
        return new JdbcStockStore(jdbc);
    }
}
