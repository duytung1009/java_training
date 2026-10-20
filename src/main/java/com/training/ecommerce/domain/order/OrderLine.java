package com.training.ecommerce.domain.order;

import com.training.ecommerce.domain.catalog.Product;

import java.math.BigDecimal;

/**
 * Một dòng trong đơn hàng: product và số lượng.
 */
public class OrderLine {

    /**
     * @throws IllegalArgumentException khi product null hoặc quantity &lt;= 0
     * @throws IllegalStateException    khi quantity lớn hơn tồn kho hiện tại của product
     */
    public OrderLine(Product product, int quantity) {
        throw new UnsupportedOperationException("TODO S02");
    }

    public Product getProduct() {
        throw new UnsupportedOperationException("TODO S02");
    }

    public int getQuantity() {
        throw new UnsupportedOperationException("TODO S02");
    }

    /**
     * @return price × quantity, scale 2, làm tròn HALF_UP
     */
    public BigDecimal lineTotal() {
        throw new UnsupportedOperationException("TODO S02");
    }
}
