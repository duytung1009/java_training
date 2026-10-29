// SPEC S07-extra – sinh 30/10 (shipment-lifecycle). Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
// Test concurrency xanh không chứng minh code không có race; vì vậy kịch bản đa luồng được lặp nhiều lần (@RepeatedTest).
package com.training.ecommerce.domain.inventory;

import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.ProductNotFoundException;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("InventoryManager.restock: hoàn kho theo đơn (S07-extra)")
class InventoryManagerRestockTest {

    private InventoryManager hub;
    private Product tshirt;
    private Product jeans;

    @BeforeEach
    void setUp() {
        hub = new InventoryManager();
        tshirt = new PhysicalProduct("P001", "Áo thun", new BigDecimal("150000"), 3, new BigDecimal("0.5"));
        jeans = new PhysicalProduct("P002", "Quần jean", new BigDecimal("450000"), 5, new BigDecimal("1"));
        hub.addProduct(tshirt);
        hub.addProduct(jeans);
    }

    private Order orderOf(OrderLine... lines) {
        Order order = new Order(BigDecimal.ZERO, BigDecimal.ZERO);
        for (OrderLine line : lines) {
            order.addLine(line);
        }
        return order;
    }

    @Test
    @DisplayName("Đơn áo 1 + quần 1 + áo 1, hub áo 3 quần 5 → áo 5, quần 6 (cộng dồn dòng cùng product)")
    void restocksOrderQuantities() {
        hub.restock(orderOf(new OrderLine(tshirt, 1), new OrderLine(jeans, 1), new OrderLine(tshirt, 1)));

        assertEquals(5, tshirt.getStock());
        assertEquals(6, jeans.getStock());
    }

    @Test
    @DisplayName("Một dòng có product không có trong hub → ProductNotFoundException, stock không đổi")
    void restockAllOrNothing() {
        Product ghost = new PhysicalProduct("P999", "Không có trong hub", new BigDecimal("1000"), 5, BigDecimal.ONE);
        Order order = orderOf(new OrderLine(tshirt, 2), new OrderLine(ghost, 1), new OrderLine(jeans, 1));

        assertThrows(ProductNotFoundException.class, () -> hub.restock(order));

        assertEquals(3, tshirt.getStock());
        assertEquals(5, jeans.getStock());
    }

    @Test
    @DisplayName("order null → IAE")
    void rejectsNullOrder() {
        assertThrows(IllegalArgumentException.class, () -> hub.restock(null));
    }

    @Test
    @DisplayName("Đơn rỗng → không làm gì, stock không đổi")
    void emptyOrderChangesNothing() {
        hub.restock(orderOf());

        assertEquals(3, tshirt.getStock());
        assertEquals(5, jeans.getStock());
    }

    @RepeatedTest(20)
    @DisplayName("100 thread restock đơn áo 1 + quần 2 cùng lúc → áo +100, quần +200, không mất cập nhật")
    void concurrentRestockLosesNoUpdate() throws Exception {
        Order order = orderOf(new OrderLine(tshirt, 1), new OrderLine(jeans, 2));

        List<Throwable> results = Concurrently.run(100, i -> () -> hub.restock(order));

        results.forEach(r -> assertNull(r, "restock không được ném exception"));
        assertEquals(103, tshirt.getStock());
        assertEquals(205, jeans.getStock());
    }
}
