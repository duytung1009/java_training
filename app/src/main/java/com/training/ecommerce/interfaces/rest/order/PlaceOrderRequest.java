package com.training.ecommerce.interfaces.rest.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

/**
 * Body đặt đơn.
 *
 * @param discountRate tỉ lệ giảm giá trong [0, 1]; bỏ trống = 0
 * @param taxRate      tỉ lệ thuế &gt;= 0; bỏ trống = 0
 * @param lines        ít nhất một dòng
 */
public record PlaceOrderRequest(
        @DecimalMin(value = "0", message = "discountRate phải nằm trong [0, 1]")
        @DecimalMax(value = "1", message = "discountRate phải nằm trong [0, 1]")
        @Schema(description = "Tỉ lệ giảm giá trong [0, 1], mặc định 0") BigDecimal discountRate,
        @DecimalMin(value = "0", message = "taxRate phải >= 0")
        @Schema(description = "Tỉ lệ thuế >= 0, mặc định 0") BigDecimal taxRate,
        @NotEmpty(message = "lines phải có ít nhất một dòng")
        List<@Valid Line> lines) {

    public record Line(
            @NotBlank(message = "productId không được rỗng") String productId,
            @Positive(message = "quantity phải > 0") int quantity) {
    }
}
