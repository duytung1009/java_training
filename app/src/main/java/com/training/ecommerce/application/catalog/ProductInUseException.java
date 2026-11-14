package com.training.ecommerce.application.catalog;

import com.training.ecommerce.shared.error.BusinessException;

/**
 * Không xóa được product vì đang được tham chiếu (là item của bundle khác hoặc nằm trong đơn hàng); CONFLICT, mã {@code PRODUCT_IN_USE}.
 */
public class ProductInUseException extends BusinessException {
    private final String productId;

    public ProductInUseException(String productId, Throwable cause) {
        super(Category.CONFLICT, "PRODUCT_IN_USE", "Product " + productId + " đang được bundle hoặc đơn hàng tham chiếu nên không xóa được", cause);
        this.productId = productId;
    }

    public String getProductId() {
        return productId;
    }
}
