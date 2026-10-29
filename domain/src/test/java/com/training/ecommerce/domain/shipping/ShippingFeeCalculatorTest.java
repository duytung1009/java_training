// SPEC S07-extra – sinh 30/10 (shipment-lifecycle). Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
package com.training.ecommerce.domain.shipping;

import com.training.ecommerce.domain.catalog.BundleProduct;
import com.training.ecommerce.domain.catalog.DigitalProduct;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.SubscriptionProduct;
import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ShippingFeeCalculator: phí ship theo cả đơn (S07-extra)")
class ShippingFeeCalculatorTest {

    private static final Address HANOI = new Address("1 Phố Huế", "Hà Nội");
    private static final Address DANANG = new Address("2 Bạch Đằng", "Đà Nẵng");

    private PhysicalProduct tshirt;
    private PhysicalProduct jeans;
    private DigitalProduct ebook;

    @BeforeEach
    void setUp() {
        tshirt = new PhysicalProduct("P001", "Áo thun", new BigDecimal("150000"), 50, new BigDecimal("0.5"));
        jeans = new PhysicalProduct("P002", "Quần jean", new BigDecimal("450000"), 20, new BigDecimal("1"));
        ebook = new DigitalProduct("D001", "Ebook Java", new BigDecimal("100000"), 100);
    }

    private Order orderOf(OrderLine... lines) {
        Order order = new Order(BigDecimal.ZERO, BigDecimal.ZERO);
        for (OrderLine line : lines) {
            order.addLine(line);
        }
        return order;
    }

    private Order shirtsAndJeans() {
        return orderOf(new OrderLine(tshirt, 2), new OrderLine(jeans, 1));
    }

    @Test
    @DisplayName("Áo 0.5 kg × 2 + quần 1 kg, Hà Nội → 30000.00 (baseFee 20000 một lần + 5000 × 2 kg)")
    void sumsWeightOnceBaseFee() {
        assertEquals(new BigDecimal("30000.00"), ShippingFeeCalculator.feeFor(shirtsAndJeans(), HANOI));
    }

    @Test
    @DisplayName("Cùng đơn giao Đà Nẵng → 45000.00 (35000 + 10000)")
    void otherCityFee() {
        assertEquals(new BigDecimal("45000.00"), ShippingFeeCalculator.feeFor(shirtsAndJeans(), DANANG));
    }

    @Test
    @DisplayName("Dòng digital đi cùng hàng vật lý không tính cân")
    void ignoresDigitalLinesNextToPhysical() {
        Order order = orderOf(new OrderLine(tshirt, 2), new OrderLine(jeans, 1), new OrderLine(ebook, 3));

        assertEquals(new BigDecimal("30000.00"), ShippingFeeCalculator.feeFor(order, HANOI));
    }

    @Test
    @DisplayName("Bundle áo 0.5 kg + ebook × 1, Hà Nội → 22500.00 (item digital không tính cân)")
    void countsPhysicalItemsInBundle() {
        BundleProduct bundle = new BundleProduct("B001", "Combo", 10, new BigDecimal("0.1"), tshirt, ebook);

        assertEquals(new BigDecimal("22500.00"), ShippingFeeCalculator.feeFor(orderOf(new OrderLine(bundle, 1)), HANOI));
    }

    @Test
    @DisplayName("Bundle × 2 nhân theo quantity của dòng; bundle lồng bundle tính đệ quy")
    void countsNestedBundleTimesQuantity() {
        BundleProduct inner = new BundleProduct("B001", "Combo nhỏ", 10, new BigDecimal("0.1"), tshirt, ebook);
        BundleProduct outer = new BundleProduct("B002", "Combo lớn", 10, new BigDecimal("0.1"), inner, jeans);

        // (0.5 + 1) kg × 2 = 3 kg → 20000 + 15000
        assertEquals(new BigDecimal("35000.00"),
                ShippingFeeCalculator.feeFor(orderOf(new OrderLine(outer, 2)), HANOI));
    }

    @Test
    @DisplayName("Đơn chỉ có hàng digital / subscription (hoặc bundle không có item vật lý) → IAE")
    void rejectsOrderWithoutPhysicalLines() {
        SubscriptionProduct subscription =
                new SubscriptionProduct("S001", "Gói tháng", new BigDecimal("50000"), 3, 100);
        BundleProduct digitalBundle = new BundleProduct("B003", "Combo số", 10, BigDecimal.ZERO, ebook);

        assertThrows(IllegalArgumentException.class,
                () -> ShippingFeeCalculator.feeFor(orderOf(new OrderLine(ebook, 1)), HANOI));
        assertThrows(IllegalArgumentException.class,
                () -> ShippingFeeCalculator.feeFor(orderOf(new OrderLine(subscription, 1)), HANOI));
        assertThrows(IllegalArgumentException.class,
                () -> ShippingFeeCalculator.feeFor(orderOf(new OrderLine(digitalBundle, 1)), HANOI));
        assertThrows(IllegalArgumentException.class, () -> ShippingFeeCalculator.feeFor(orderOf(), HANOI));
    }

    @Test
    @DisplayName("order hoặc address null → IAE")
    void rejectsNullArgs() {
        assertThrows(IllegalArgumentException.class, () -> ShippingFeeCalculator.feeFor(null, HANOI));
        assertThrows(IllegalArgumentException.class, () -> ShippingFeeCalculator.feeFor(shirtsAndJeans(), null));
    }
}
