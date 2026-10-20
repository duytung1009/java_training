// SPEC S02 – sinh bởi java-start-backlog 20/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
// SPEC S03 – nâng cấp 20/10: Product thành abstract → dùng PhysicalProduct (0.5 kg); kỳ vọng S02 giữ nguyên.
package com.training.ecommerce.domain.order;

import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderTest {

    private static final BigDecimal TEN_PERCENT = new BigDecimal("0.10");
    private static final BigDecimal VAT_8 = new BigDecimal("0.08");

    private Product tshirt;
    private Product jeans;
    private Product socks;

    @BeforeEach
    void setUp() {
        tshirt = new PhysicalProduct("P001", "Áo thun nam", new BigDecimal("150000"), 50, new BigDecimal("0.5"));
        jeans = new PhysicalProduct("P002", "Quần jean", new BigDecimal("450000"), 20, new BigDecimal("0.5"));
        socks = new PhysicalProduct("P003", "Tất cổ ngắn", new BigDecimal("12345.67"), 100, new BigDecimal("0.5"));
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private static void assertAmounts(Order order, String subtotal, String discount, String tax, String total) {
        assertAll(
                () -> assertEquals(money(subtotal), order.subtotal(), "subtotal"),
                () -> assertEquals(money(discount), order.discountAmount(), "discountAmount"),
                () -> assertEquals(money(tax), order.taxAmount(), "taxAmount"),
                () -> assertEquals(money(total), order.finalAmount(), "finalAmount"));
    }

    @Nested
    @DisplayName("Tính tiền: subtotal → discount → tax → final")
    class Calculation {

        @Test
        @DisplayName("Đơn chưa có line → mọi số tiền = 0.00")
        void emptyOrderIsZero() {
            Order order = new Order(TEN_PERCENT, VAT_8);

            assertAmounts(order, "0.00", "0.00", "0.00", "0.00");
        }

        @Test
        @DisplayName("2 áo thun + 1 quần jean, giảm 10%, VAT 8% → 729000.00")
        void calculatesStandardOrder() {
            Order order = new Order(TEN_PERCENT, VAT_8);
            order.addLine(new OrderLine(tshirt, 2));
            order.addLine(new OrderLine(jeans, 1));

            assertAmounts(order, "750000.00", "75000.00", "54000.00", "729000.00");
        }

        @Test
        @DisplayName("Thuế tính trên số tiền SAU giảm giá")
        void taxIsCalculatedOnDiscountedAmount() {
            Order order = new Order(new BigDecimal("0.20"), new BigDecimal("0.10"));
            order.addLine(new OrderLine(jeans, 1));

            assertAmounts(order, "450000.00", "90000.00", "36000.00", "396000.00");
        }

        @Test
        @DisplayName("Làm tròn HALF_UP 2 chữ số ở từng bước")
        void roundsHalfUpAtEachStep() {
            Order order = new Order(new BigDecimal("0.075"), VAT_8);
            order.addLine(new OrderLine(socks, 3));

            // 37037.01 × 0.075 = 2777.77575 → 2777.78 ; (37037.01 − 2777.78) × 0.08 = 2740.7384 → 2740.74
            assertAmounts(order, "37037.01", "2777.78", "2740.74", "36999.97");
        }

        @Test
        @DisplayName("Không giảm giá (0) → final = subtotal + thuế")
        void zeroDiscount() {
            Order order = new Order(BigDecimal.ZERO, VAT_8);
            order.addLine(new OrderLine(jeans, 1));

            assertAmounts(order, "450000.00", "0.00", "36000.00", "486000.00");
        }

        @Test
        @DisplayName("Giảm 100% → final = 0.00")
        void fullDiscount() {
            Order order = new Order(BigDecimal.ONE, VAT_8);
            order.addLine(new OrderLine(tshirt, 2));
            order.addLine(new OrderLine(jeans, 1));

            assertAmounts(order, "750000.00", "750000.00", "0.00", "0.00");
        }

        @Test
        @DisplayName("Không thuế (0) → final = subtotal − giảm giá")
        void zeroTax() {
            Order order = new Order(TEN_PERCENT, BigDecimal.ZERO);
            order.addLine(new OrderLine(tshirt, 2));

            assertAmounts(order, "300000.00", "30000.00", "0.00", "270000.00");
        }

        @Test
        @DisplayName("Subtotal cập nhật sau mỗi lần addLine")
        void subtotalReflectsLinesAddedLater() {
            Order order = new Order(TEN_PERCENT, VAT_8);
            order.addLine(new OrderLine(tshirt, 2));
            assertEquals(money("300000.00"), order.subtotal());

            order.addLine(new OrderLine(jeans, 1));
            assertEquals(money("750000.00"), order.subtotal());
        }
    }

    @Nested
    @DisplayName("Kiểm tra tham số")
    class Validation {

        @ParameterizedTest(name = "discountRate = {0}")
        @ValueSource(strings = {"-0.01", "1.01", "10"})
        @DisplayName("discountRate ngoài [0, 1] → IllegalArgumentException")
        void rejectsDiscountRateOutOfRange(String rate) {
            assertThrows(IllegalArgumentException.class, () -> new Order(new BigDecimal(rate), VAT_8));
        }

        @Test
        @DisplayName("taxRate âm → IllegalArgumentException")
        void rejectsNegativeTaxRate() {
            assertThrows(IllegalArgumentException.class, () -> new Order(TEN_PERCENT, new BigDecimal("-0.08")));
        }

        @Test
        @DisplayName("discountRate null → IllegalArgumentException")
        void rejectsNullDiscountRate() {
            assertThrows(IllegalArgumentException.class, () -> new Order(null, VAT_8));
        }

        @Test
        @DisplayName("taxRate null → IllegalArgumentException")
        void rejectsNullTaxRate() {
            assertThrows(IllegalArgumentException.class, () -> new Order(TEN_PERCENT, null));
        }

        @Test
        @DisplayName("addLine(null) → IllegalArgumentException")
        void rejectsNullLine() {
            Order order = new Order(TEN_PERCENT, VAT_8);

            assertThrows(IllegalArgumentException.class, () -> order.addLine(null));
        }
    }

    @Nested
    @DisplayName("getLines")
    class GetLines {

        @Test
        @DisplayName("Đơn rỗng → mảng độ dài 0")
        void emptyOrderHasNoLines() {
            assertEquals(0, new Order(TEN_PERCENT, VAT_8).getLines().length);
        }

        @Test
        @DisplayName("Trả về đúng các line theo thứ tự add, độ dài bằng số line")
        void returnsLinesInInsertionOrder() {
            Order order = new Order(TEN_PERCENT, VAT_8);
            OrderLine first = new OrderLine(tshirt, 2);
            OrderLine second = new OrderLine(jeans, 1);
            order.addLine(first);
            order.addLine(second);

            OrderLine[] lines = order.getLines();

            assertEquals(2, lines.length);
            assertSame(first, lines[0]);
            assertSame(second, lines[1]);
        }

        @Test
        @DisplayName("Sửa mảng trả về không làm thay đổi Order (defensive copy)")
        void returnedArrayIsACopy() {
            Order order = new Order(TEN_PERCENT, VAT_8);
            OrderLine first = new OrderLine(tshirt, 2);
            order.addLine(first);

            OrderLine[] lines = order.getLines();
            lines[0] = new OrderLine(jeans, 1);

            assertSame(first, order.getLines()[0]);
            assertEquals(money("300000.00"), order.subtotal());
        }
    }
}
