package com.training.ecommerce.domain.order;

import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.catalog.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Một dòng trong đơn hàng: product và số lượng.
 */
public class OrderLine {
    private final Product product;
    private final int quantity;
    private final BigDecimal unitPrice; // Snapshot giá bán (calculateFinalPrice) tại thời điểm tạo đơn

    /**
     * @throws IllegalArgumentException khi product null hoặc quantity &lt;= 0
     * @throws OutOfStockException      khi quantity lớn hơn tồn kho hiện tại
     */
    public OrderLine(Product product, int quantity) {
        if (product == null) {
            throw new IllegalArgumentException("product không được null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity phải > 0");
        }
        if (quantity > product.getStock()) {
            throw new OutOfStockException(product.getId(), quantity, product.getStock());
        }
        this(product, quantity, product.calculateFinalPrice());     // Java 25 (JEP 513): được phép có lệnh trước this(...)
    }

    /**
     * Khôi phục dòng đã lưu: dùng đúng đơn giá snapshot, không tính lại từ {@code calculateFinalPrice()}
     * và không kiểm tra tồn kho (hàng đã được trừ lúc đặt). unitPrice được chuẩn hóa về scale 2, HALF_UP.
     *
     * @throws IllegalArgumentException khi product null, quantity &lt;= 0, hoặc unitPrice null / âm
     */
    public OrderLine(Product product, int quantity, BigDecimal unitPrice) {
        if (product == null) {
            throw new IllegalArgumentException("product không được null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity phải > 0");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IllegalArgumentException("unitPrice phải >= 0");
        }
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
    }

    /** unitPrice × quantity, scale 2, HALF_UP. */
    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
}
