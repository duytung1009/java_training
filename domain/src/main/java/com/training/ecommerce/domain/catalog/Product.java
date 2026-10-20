package com.training.ecommerce.domain.catalog;

import java.math.BigDecimal;

public class Product {
    private final String id;
    private final String name;
    private final int stock;
    private final BigDecimal price;

    public Product(String id) {
        this(id, "");
    }

    public Product(String id, String name) {
        this(id, name, 0);
    }

    public Product(String id, String name, int stock) {
        this(id, name, stock, BigDecimal.ZERO);
    }

    public Product(String id, String name, int stock, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.stock = stock;
        this.price = price;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getStock() { return stock; }
    public BigDecimal getPrice() { return price; }
}
