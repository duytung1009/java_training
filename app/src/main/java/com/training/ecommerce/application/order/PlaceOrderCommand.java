package com.training.ecommerce.application.order;

import java.math.BigDecimal;
import java.util.List;

/**
 * Yêu cầu đặt đơn: tỉ lệ giảm giá, thuế và các dòng (productId, quantity).
 * Tỉ lệ chỉ được kiểm tra khi dựng {@code Order} (nguồn kiểm tra duy nhất), không lặp lại ở đây.
 *
 * @param discountRate tỉ lệ giảm giá trong [0, 1]
 * @param taxRate      tỉ lệ thuế &gt;= 0
 * @param lines        ít nhất một dòng; được copy phòng thủ thành list không sửa được
 */
public record PlaceOrderCommand(BigDecimal discountRate, BigDecimal taxRate, List<Line> lines) {

    /**
     * @throws IllegalArgumentException khi lines null / rỗng / chứa null
     */
    public PlaceOrderCommand {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("lines không được rỗng");
        }
        try {
            lines = List.copyOf(lines);     // copy phòng thủ; List.copyOf ném NPE nếu có phần tử null
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("line không được null", e);
        }
    }

    /**
     * @param productId id product, không rỗng
     * @param quantity  số lượng &gt; 0
     */
    public record Line(String productId, int quantity) {

        /**
         * @throws IllegalArgumentException khi productId null / rỗng hoặc quantity &lt;= 0
         */
        public Line {
            if (productId == null || productId.isBlank()) {
                throw new IllegalArgumentException("productId không được rỗng");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity phải > 0");
            }
        }
    }
}
