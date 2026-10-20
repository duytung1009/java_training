package com.training.ecommerce.domain.order;

import com.training.ecommerce.domain.catalog.Product;
import java.math.BigDecimal;

public class OrderLine {
    private final Product product;
    private final int quantity;
    private final BigDecimal unitPrice; // Snapshot giá tại thời điểm tạo đơn

    public OrderLine(Product product) {
        this(product, 0);
    }

    public OrderLine(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = product.getPrice();
    }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
}
