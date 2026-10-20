package com.training.ecommerce.domain.order;

import com.training.ecommerce.domain.catalog.Product;
import java.math.BigDecimal;
import java.util.Arrays;

public class Order {
    private final String id;
    private final double discountRate;
    private final double taxRate;
    private OrderLine[] items = new OrderLine[]{};

    public Order(String id, double discountRate, double taxRate) {
        this.id = id;
        this.discountRate = discountRate;
        this.taxRate = taxRate;
    }

    public void addItem(Product product, int quantity) throws Exception {
        if (product == null || quantity < 0) {
            throw new Exception("Invalid value");
        }
        int itemsLength = this.items.length;
        this.items = Arrays.copyOf(this.items, itemsLength + 1);
        this.items[itemsLength] = new OrderLine(product, quantity);
    }

    public BigDecimal getRawTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderLine line : items) {
            total = total.add(line.getSubtotal());
        }
        return total;
    }

    public BigDecimal getDiscountAmount() {
        return getRawTotal().multiply(BigDecimal.valueOf(discountRate));
    }

    public BigDecimal getTaxableTotal() {
        return getRawTotal().subtract(getDiscountAmount());
    }

    public BigDecimal getTaxAmount() {
        return getTaxableTotal().multiply(BigDecimal.valueOf(taxRate));
    }

    public BigDecimal getFinalTotal() {
        return getTaxableTotal().add(getTaxAmount());
    }

    public String getId() { return id; }
    public OrderLine[] getItems() { return items; }
    public double getDiscountRate() { return discountRate; }
    public double getTaxRate() { return taxRate; }
}
