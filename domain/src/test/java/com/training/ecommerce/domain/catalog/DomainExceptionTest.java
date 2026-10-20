// SPEC S04 – sinh bởi java-start-backlog 21/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
package com.training.ecommerce.domain.catalog;

import com.training.ecommerce.domain.inventory.InventoryManager;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Domain exception: OutOfStockException, ProductNotFoundException")
class DomainExceptionTest {

    private Product tshirt;
    private Product jeans;

    @BeforeEach
    void setUp() {
        tshirt = new PhysicalProduct("P001", "Áo thun nam", new BigDecimal("150000"), 50, new BigDecimal("0.3"));
        jeans = new PhysicalProduct("P002", "Quần jean", new BigDecimal("450000"), 20, new BigDecimal("0.8"));
    }

    @Nested
    @DisplayName("Bản thân exception")
    class ExceptionItself {

        @Test
        @DisplayName("OutOfStockException: unchecked, là IllegalStateException, có dữ liệu và message có id")
        void outOfStockCarriesData() {
            OutOfStockException e = new OutOfStockException("P002", 21, 20);

            assertInstanceOf(IllegalStateException.class, e);
            assertInstanceOf(RuntimeException.class, e);
            assertEquals("P002", e.getProductId());
            assertEquals(21, e.getRequested());
            assertEquals(20, e.getAvailable());
            assertTrue(e.getMessage().contains("P002"), e.getMessage());
        }

        @Test
        @DisplayName("ProductNotFoundException: unchecked, là IllegalArgumentException, message có id")
        void notFoundCarriesId() {
            ProductNotFoundException e = new ProductNotFoundException("P999");

            assertInstanceOf(IllegalArgumentException.class, e);
            assertEquals("P999", e.getProductId());
            assertTrue(e.getMessage().contains("P999"), e.getMessage());
        }
    }

    @Nested
    @DisplayName("Nơi ném exception")
    class ThrownBy {

        @Test
        @DisplayName("Product.decreaseStock thiếu hàng → OutOfStockException")
        void productDecreaseStock() {
            OutOfStockException e = assertThrows(OutOfStockException.class, () -> jeans.decreaseStock(21));

            assertEquals("P002", e.getProductId());
            assertEquals(21, e.getRequested());
            assertEquals(20, e.getAvailable());
        }

        @Test
        @DisplayName("new OrderLine vượt tồn kho → OutOfStockException")
        void orderLineOverStock() {
            OutOfStockException e = assertThrows(OutOfStockException.class, () -> new OrderLine(tshirt, 51));

            assertEquals("P001", e.getProductId());
            assertEquals(51, e.getRequested());
            assertEquals(50, e.getAvailable());
        }

        @Test
        @DisplayName("InventoryManager.decreaseStock thiếu hàng → OutOfStockException với tổng số lượng cộng dồn")
        void inventoryOutOfStock() {
            InventoryManager inventory = new InventoryManager();
            inventory.addProduct(tshirt);
            Order order = new Order(BigDecimal.ZERO, BigDecimal.ZERO);
            order.addLine(new OrderLine(tshirt, 30));
            order.addLine(new OrderLine(tshirt, 30));

            OutOfStockException e = assertThrows(OutOfStockException.class, () -> inventory.decreaseStock(order));

            assertEquals("P001", e.getProductId());
            assertEquals(60, e.getRequested());
            assertEquals(50, e.getAvailable());
        }

        @Test
        @DisplayName("InventoryManager.decreaseStock product không có trong kho → ProductNotFoundException")
        void inventoryProductNotFound() {
            InventoryManager inventory = new InventoryManager();
            inventory.addProduct(tshirt);
            Order order = new Order(BigDecimal.ZERO, BigDecimal.ZERO);
            order.addLine(new OrderLine(jeans, 1));

            ProductNotFoundException e = assertThrows(ProductNotFoundException.class, () -> inventory.decreaseStock(order));

            assertEquals("P002", e.getProductId());
        }
    }
}
