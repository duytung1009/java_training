package com.training.ecommerce.interfaces.rest.catalog;

import com.training.ecommerce.domain.catalog.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Body tạo / cập nhật product. Field bắt buộc phụ thuộc {@code type}:
 * PHYSICAL cần {@code weightKg}; SUBSCRIPTION cần {@code months} ({@code basePrice} là giá một tháng);
 * BUNDLE cần {@code discountRate} và {@code itemIds} ({@code basePrice} bị bỏ qua, suy ra từ item); DIGITAL không cần thêm.
 */
public record ProductRequest(
        @Schema(description = "Id product; khi PUT thì id trên path được ưu tiên (nếu có ở body phải trùng)") String id,
        @Schema(description = "Loại product") ProductType type,
        String name,
        BigDecimal basePrice,
        int stock,
        @Schema(description = "FASHION, ELECTRONICS, SOFTWARE, SUBSCRIPTION hoặc OTHER (mặc định)") String category,
        @Schema(description = "PHYSICAL: cân nặng kg") BigDecimal weightKg,
        @Schema(description = "SUBSCRIPTION: số tháng") Integer months,
        @Schema(description = "BUNDLE: tỉ lệ giảm trong [0, 1]") BigDecimal discountRate,
        @Schema(description = "BUNDLE: id các item, theo thứ tự") List<String> itemIds) {
}
