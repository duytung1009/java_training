package com.training.ecommerce.domain.order;

import com.training.ecommerce.domain.catalog.Product;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Giỏ hàng: product → số lượng, giữ thứ tự thêm. Không trừ kho.
 */
public class Cart {

    /**
     * Thêm (cộng dồn nếu đã có).
     *
     * @throws IllegalArgumentException khi product null hoặc quantity &lt;= 0
     * @throws com.training.ecommerce.domain.catalog.OutOfStockException khi tổng số lượng trong giỏ vượt tồn kho (giỏ giữ nguyên)
     */
    public void add(Product product, int quantity) {
        throw new UnsupportedOperationException("TODO S04");
    }

    /**
     * Đặt lại số lượng; 0 thì bỏ khỏi giỏ.
     *
     * @throws IllegalArgumentException khi product null hoặc quantity &lt; 0
     * @throws com.training.ecommerce.domain.catalog.ProductNotFoundException khi product chưa có trong giỏ
     * @throws com.training.ecommerce.domain.catalog.OutOfStockException khi quantity vượt tồn kho (giỏ giữ nguyên)
     */
    public void changeQuantity(Product product, int quantity) {
        throw new UnsupportedOperationException("TODO S04");
    }

    /** Bỏ product khỏi giỏ; chưa có thì không làm gì. */
    public void remove(Product product) {
        throw new UnsupportedOperationException("TODO S04");
    }

    /** @return số lượng trong giỏ, 0 nếu chưa có */
    public int getQuantity(Product product) {
        throw new UnsupportedOperationException("TODO S04");
    }

    /** @return product → số lượng theo thứ tự thêm; map không sửa được */
    public Map<Product, Integer> getItems() {
        throw new UnsupportedOperationException("TODO S04");
    }

    public boolean isEmpty() {
        throw new UnsupportedOperationException("TODO S04");
    }

    /**
     * Tạo Order: mỗi product một OrderLine theo thứ tự thêm.
     *
     * @throws IllegalStateException khi giỏ rỗng
     */
    public Order toOrder(BigDecimal discountRate, BigDecimal taxRate) {
        throw new UnsupportedOperationException("TODO S04");
    }
}
