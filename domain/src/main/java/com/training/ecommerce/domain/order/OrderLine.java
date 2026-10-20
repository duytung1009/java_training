package com.training.ecommerce.domain.order;

import com.training.ecommerce.domain.catalog.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Một dòng trong đơn hàng: product và số lượng.
 */
public class OrderLine {
    private final Product product;
    private final int quantity;
    private final BigDecimal unitPrice; // Snapshot giá tại thời điểm tạo đơn

    public OrderLine(Product product, int quantity) {
        if (product == null) {
            throw new IllegalArgumentException("product không được null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity phải > 0");
        }
        if (quantity > product.getStock()) {
            throw new IllegalStateException("Không đủ hàng cho " + product.getId()
                    + ": còn " + product.getStock() + ", cần " + quantity);
        }
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = product.getPrice();
    }

    /** unitPrice × quantity, scale 2, HALF_UP. */
    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
}
