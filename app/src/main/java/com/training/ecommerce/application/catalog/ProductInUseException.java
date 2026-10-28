package com.training.ecommerce.application.catalog;

/**
 * Không xóa được product vì đang được tham chiếu (là item của bundle khác hoặc nằm trong đơn hàng).
 */
public class ProductInUseException extends RuntimeException {
    private final String productId;

    public ProductInUseException(String productId, Throwable cause) {
        super("Product " + productId + " đang được bundle hoặc đơn hàng tham chiếu nên không xóa được", cause);
        this.productId = productId;
    }

    public String getProductId() {
        return productId;
    }
}
