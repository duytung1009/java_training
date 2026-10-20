// SPEC S05 – sinh bởi java-start-backlog 21/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
package com.training.ecommerce.application;

import com.training.ecommerce.domain.catalog.Category;
import com.training.ecommerce.domain.catalog.DigitalProduct;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ReportService (S05)")
class ReportServiceTest {

    private final ReportService report = new ReportService();

    private Product tshirt;
    private Product jeans;
    private Product office;
    private Product mouse;
    private Order o1;
    private Order o2;
    private Order o3;
    private List<Order> orders;

    @BeforeEach
    void setUp() {
        tshirt = new PhysicalProduct("P001", "Áo thun nam", new BigDecimal("150000"), 50, new BigDecimal("0.3"), Category.FASHION);
        jeans = new PhysicalProduct("P002", "Quần jean", new BigDecimal("450000"), 20, new BigDecimal("0.8"), Category.FASHION);
        office = new DigitalProduct("D001", "Office 2024", new BigDecimal("1000000"), 200, Category.SOFTWARE);
        mouse = new PhysicalProduct("E001", "Chuột không dây", new BigDecimal("250000"), 30, new BigDecimal("0.1"), Category.ELECTRONICS);

        // subtotal 1.300.000, giảm 10% → 1.170.000, thuế 8% → 1.263.600
        o1 = new Order(new BigDecimal("0.10"), new BigDecimal("0.08"));
        o1.addLine(new OrderLine(tshirt, 2));
        o1.addLine(new OrderLine(office, 1));
        // 900.000
        o2 = new Order(BigDecimal.ZERO, BigDecimal.ZERO);
        o2.addLine(new OrderLine(jeans, 1));
        o2.addLine(new OrderLine(tshirt, 3));
        // 1.250.000
        o3 = new Order(BigDecimal.ZERO, BigDecimal.ZERO);
        o3.addLine(new OrderLine(mouse, 5));

        orders = List.of(o1, o2, o3);
    }

    @Nested
    @DisplayName("revenueByCategory")
    class RevenueByCategory {

        @Test
        @DisplayName("Σ lineTotal theo category (trước giảm giá/thuế của đơn)")
        void sumsLineTotalsPerCategory() {
            Map<Category, BigDecimal> revenue = report.revenueByCategory(orders);

            // FASHION: 300.000 (o1) + 450.000 + 450.000 (o2)
            assertEquals(new BigDecimal("1200000.00"), revenue.get(Category.FASHION));
            assertEquals(new BigDecimal("1000000.00"), revenue.get(Category.SOFTWARE));
            assertEquals(new BigDecimal("1250000.00"), revenue.get(Category.ELECTRONICS));
        }

        @Test
        @DisplayName("Category không có doanh thu thì không có key")
        void onlyCategoriesWithSales() {
            Map<Category, BigDecimal> revenue = report.revenueByCategory(orders);

            assertEquals(3, revenue.size());
            assertFalse(revenue.containsKey(Category.SUBSCRIPTION));
            assertFalse(revenue.containsKey(Category.OTHER));
        }

        @Test
        @DisplayName("Product tạo bằng constructor cũ → nhóm OTHER")
        void defaultCategoryIsOther() {
            Product legacy = new DigitalProduct("D900", "Ebook", new BigDecimal("99000"), 10);
            Order order = new Order(BigDecimal.ZERO, BigDecimal.ZERO);
            order.addLine(new OrderLine(legacy, 2));

            assertEquals(Map.of(Category.OTHER, new BigDecimal("198000.00")), report.revenueByCategory(List.of(order)));
        }

        @Test
        @DisplayName("Không có order → map rỗng; null → IllegalArgumentException")
        void emptyAndNull() {
            assertTrue(report.revenueByCategory(List.of()).isEmpty());
            assertThrows(IllegalArgumentException.class, () -> report.revenueByCategory(null));
        }
    }

    @Nested
    @DisplayName("topSellingProducts")
    class TopSelling {

        // Tổng số lượng: tshirt 5, mouse 5, office 1, jeans 1

        @Test
        @DisplayName("Sắp theo tổng số lượng giảm dần; bằng nhau thì id tăng dần")
        void sortsByQuantityThenId() {
            assertEquals(List.of(mouse, tshirt), report.topSellingProducts(orders, 2));
            assertEquals(List.of(mouse, tshirt, office), report.topSellingProducts(orders, 3));
        }

        @Test
        @DisplayName("n lớn hơn số product → trả hết")
        void nLargerThanProductCount() {
            assertEquals(List.of(mouse, tshirt, office, jeans), report.topSellingProducts(orders, 10));
        }

        @Test
        @DisplayName("Cùng id ở instance khác vẫn cộng chung (equals theo id)")
        void groupsByProductId() {
            Product sameJeans = new PhysicalProduct(new String("P002"), "Quần jean (bản khác)", new BigDecimal("450000"), 20, new BigDecimal("0.8"), Category.FASHION);
            Order o4 = new Order(BigDecimal.ZERO, BigDecimal.ZERO);
            o4.addLine(new OrderLine(sameJeans, 9));

            // jeans: 1 + 9 = 10 → đứng đầu
            List<Product> top = report.topSellingProducts(List.of(o1, o2, o3, o4), 1);

            assertEquals(1, top.size());
            assertEquals("P002", top.get(0).getId());
        }

        @Test
        @DisplayName("Không có order → list rỗng")
        void emptyOrders() {
            assertTrue(report.topSellingProducts(List.of(), 3).isEmpty());
        }

        @ParameterizedTest(name = "n = {0}")
        @ValueSource(ints = {0, -1})
        @DisplayName("n <= 0 → IllegalArgumentException")
        void rejectsNonPositiveN(int n) {
            assertThrows(IllegalArgumentException.class, () -> report.topSellingProducts(orders, n));
        }

        @Test
        @DisplayName("orders null → IllegalArgumentException")
        void rejectsNullOrders() {
            assertThrows(IllegalArgumentException.class, () -> report.topSellingProducts(null, 3));
        }
    }

    @Nested
    @DisplayName("partitionByFinalAmount")
    class Partition {

        @Test
        @DisplayName("true: finalAmount ≥ threshold, false: còn lại; giữ thứ tự đơn")
        void partitionsByThreshold() {
            Map<Boolean, List<Order>> parts = report.partitionByFinalAmount(orders, new BigDecimal("1000000"));

            assertEquals(List.of(o1, o3), parts.get(true));
            assertEquals(List.of(o2), parts.get(false));
        }

        @Test
        @DisplayName("Bằng đúng threshold → nhóm true (so bằng compareTo: 900000 vs 900000.00)")
        void equalGoesToTrue() {
            Map<Boolean, List<Order>> parts = report.partitionByFinalAmount(List.of(o2), new BigDecimal("900000"));

            assertEquals(List.of(o2), parts.get(true));
            assertTrue(parts.get(false).isEmpty());
        }

        @Test
        @DisplayName("Không có order → vẫn đủ 2 key, list rỗng")
        void emptyOrdersHaveBothKeys() {
            Map<Boolean, List<Order>> parts = report.partitionByFinalAmount(List.of(), new BigDecimal("1000000"));

            assertEquals(2, parts.size());
            assertTrue(parts.get(true).isEmpty());
            assertTrue(parts.get(false).isEmpty());
        }

        @Test
        @DisplayName("orders / threshold null → IllegalArgumentException")
        void rejectsNulls() {
            assertThrows(IllegalArgumentException.class, () -> report.partitionByFinalAmount(null, BigDecimal.ONE));
            assertThrows(IllegalArgumentException.class, () -> report.partitionByFinalAmount(orders, null));
        }
    }

    @Test
    @DisplayName("Không sửa input: list order và các order giữ nguyên")
    void doesNotMutateInput() {
        List<Order> input = new ArrayList<>(orders);

        report.revenueByCategory(input);
        report.topSellingProducts(input, 2);
        report.partitionByFinalAmount(input, new BigDecimal("1000000"));

        assertEquals(List.of(o1, o2, o3), input);
        assertEquals(2, o1.getLines().length);
        assertEquals(new BigDecimal("1263600.00"), o1.finalAmount());
    }
}
