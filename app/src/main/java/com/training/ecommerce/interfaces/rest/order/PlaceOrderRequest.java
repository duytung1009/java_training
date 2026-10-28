package com.training.ecommerce.interfaces.rest.order;

import io.swagger.v3.oas.annotations.media.Schema;

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
        @Schema(description = "Tỉ lệ giảm giá trong [0, 1], mặc định 0") BigDecimal discountRate,
        @Schema(description = "Tỉ lệ thuế >= 0, mặc định 0") BigDecimal taxRate,
        List<Line> lines) {

    public record Line(String productId, int quantity) {
    }
}
