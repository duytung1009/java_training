package com.training.ecommerce.interfaces.rest.catalog;

import com.training.ecommerce.application.catalog.ProductSpec;
import com.training.ecommerce.application.catalog.ValidProductSpec;
import com.training.ecommerce.application.validation.EnumValue;
import com.training.ecommerce.domain.catalog.Category;
import com.training.ecommerce.domain.catalog.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Body tạo / cập nhật product. Field bắt buộc phụ thuộc {@code type} (kiểm bởi {@link ValidProductSpec}):
 * PHYSICAL cần {@code weightKg}; SUBSCRIPTION cần {@code months} ({@code basePrice} là giá một tháng);
 * BUNDLE cần {@code discountRate} và {@code itemIds} ({@code basePrice} bị bỏ qua, suy ra từ item); DIGITAL không cần thêm.
 */
@ValidProductSpec
public record ProductRequest(
        @Schema(description = "Id product; khi PUT thì id trên path được ưu tiên (nếu có ở body phải trùng)") String id,
        @NotNull(message = "type không được rỗng")
        @Schema(description = "Loại product") ProductType type,
        @NotBlank(message = "name không được rỗng")
        @Size(max = 255, message = "name tối đa 255 ký tự")
        String name,
        BigDecimal basePrice,
        @PositiveOrZero(message = "stock phải >= 0")
        int stock,
        @EnumValue(enumClass = Category.class)
        @Schema(description = "FASHION, ELECTRONICS, SOFTWARE, SUBSCRIPTION hoặc OTHER (mặc định)") String category,
        @Schema(description = "PHYSICAL: cân nặng kg") BigDecimal weightKg,
        @Schema(description = "SUBSCRIPTION: số tháng") Integer months,
        @Schema(description = "BUNDLE: tỉ lệ giảm trong [0, 1]") BigDecimal discountRate,
        @Schema(description = "BUNDLE: id các item, theo thứ tự")
        List<@NotBlank(message = "itemIds không được chứa id rỗng") String> itemIds) implements ProductSpec {
}
