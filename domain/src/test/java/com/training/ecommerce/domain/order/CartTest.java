// SPEC S04 – sinh bởi java-start-backlog 21/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
package com.training.ecommerce.domain.order;

import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.ProductNotFoundException;
import com.training.ecommerce.domain.pricing.DiscountCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartTest {

    private Cart cart;
    private Product tshirt;
    private Product jeans;
    private Product socks;

    @BeforeEach
    void setUp() {
        cart = new Cart();
        tshirt = new PhysicalProduct("P001", "Áo thun nam", new BigDecimal("150000"), 50, new BigDecimal("0.3"));
        jeans = new PhysicalProduct("P002", "Quần jean", new BigDecimal("450000"), 20, new BigDecimal("0.8"));
        socks = new PhysicalProduct("P003", "Tất cổ ngắn", new BigDecimal("12345.67"), 100, new BigDecimal("0.1"));
    }

    @Nested
    @DisplayName("add")
    class Add {

        @Test
        @DisplayName("Giỏ mới rỗng; add xong có đúng số lượng")
        void addsProduct() {
            assertTrue(cart.isEmpty());

            cart.add(tshirt, 2);

            assertFalse(cart.isEmpty());
            assertEquals(2, cart.getQuantity(tshirt));
        }

        @Test
        @DisplayName("Thêm cùng sản phẩm lần nữa → cộng dồn, không tạo dòng mới")
        void accumulatesSameProduct() {
            cart.add(tshirt, 2);
            cart.add(tshirt, 3);

            assertEquals(5, cart.getQuantity(tshirt));
            assertEquals(1, cart.getItems().size());
        }

        @Test
        @DisplayName("Cùng id nhưng khác instance vẫn cộng dồn (equals theo id)")
        void accumulatesByProductId() {
            Product sameTshirt = new PhysicalProduct(new String("P001"), "Áo thun nam (bản khác)", new BigDecimal("150000"), 50, new BigDecimal("0.3"));
            cart.add(tshirt, 2);
            cart.add(sameTshirt, 1);

            assertEquals(3, cart.getQuantity(tshirt));
            assertEquals(1, cart.getItems().size());
        }

        @Test
        @DisplayName("getItems theo thứ tự thêm; thêm lại sản phẩm cũ không đổi vị trí")
        void keepsInsertionOrder() {
            cart.add(jeans, 1);
            cart.add(tshirt, 2);
            cart.add(socks, 3);
            cart.add(jeans, 1);

            Iterator<Map.Entry<Product, Integer>> it = cart.getItems().entrySet().iterator();
            Map.Entry<Product, Integer> first = it.next();
            assertSame(jeans, first.getKey());
            assertEquals(2, first.getValue());
            assertSame(tshirt, it.next().getKey());
            assertSame(socks, it.next().getKey());
            assertFalse(it.hasNext());
        }

        @Test
        @DisplayName("product null → IllegalArgumentException")
        void rejectsNullProduct() {
            assertThrows(IllegalArgumentException.class, () -> cart.add(null, 1));
        }

        @ParameterizedTest(name = "quantity = {0}")
        @ValueSource(ints = {0, -1})
        @DisplayName("quantity <= 0 → IllegalArgumentException, giỏ giữ nguyên")
        void rejectsNonPositiveQuantity(int quantity) {
            assertThrows(IllegalArgumentException.class, () -> cart.add(tshirt, quantity));
            assertTrue(cart.isEmpty());
        }

        @Test
        @DisplayName("Thêm vượt tồn kho ngay lần đầu → OutOfStockException, giỏ rỗng")
        void rejectsQuantityOverStock() {
            OutOfStockException e = assertThrows(OutOfStockException.class, () -> cart.add(jeans, 21));

            assertEquals("P002", e.getProductId());
            assertEquals(21, e.getRequested());
            assertEquals(20, e.getAvailable());
            assertTrue(cart.isEmpty());
        }

        @Test
        @DisplayName("Tổng số lượng trong giỏ vượt tồn kho → OutOfStockException, giỏ giữ số cũ")
        void rejectsAccumulatedQuantityOverStock() {
            cart.add(tshirt, 30);

            OutOfStockException e = assertThrows(OutOfStockException.class, () -> cart.add(tshirt, 21));

            assertEquals(51, e.getRequested());
            assertEquals(50, e.getAvailable());
            assertEquals(30, cart.getQuantity(tshirt));
        }

        @Test
        @DisplayName("Đúng bằng tồn kho là hợp lệ")
        void allowsQuantityEqualToStock() {
            cart.add(jeans, 15);
            cart.add(jeans, 5);

            assertEquals(20, cart.getQuantity(jeans));
        }
    }

    @Nested
    @DisplayName("changeQuantity / remove / getQuantity")
    class ChangeAndRemove {

        @Test
        @DisplayName("changeQuantity đặt lại số lượng (không cộng dồn)")
        void setsQuantity() {
            cart.add(tshirt, 2);

            cart.changeQuantity(tshirt, 10);

            assertEquals(10, cart.getQuantity(tshirt));
        }

        @Test
        @DisplayName("changeQuantity về 0 → bỏ khỏi giỏ")
        void zeroRemovesProduct() {
            cart.add(tshirt, 2);

            cart.changeQuantity(tshirt, 0);

            assertTrue(cart.isEmpty());
            assertEquals(0, cart.getQuantity(tshirt));
        }

        @Test
        @DisplayName("changeQuantity số âm → IllegalArgumentException, giữ nguyên")
        void rejectsNegativeQuantity() {
            cart.add(tshirt, 2);

            assertThrows(IllegalArgumentException.class, () -> cart.changeQuantity(tshirt, -1));
            assertEquals(2, cart.getQuantity(tshirt));
        }

        @Test
        @DisplayName("changeQuantity sản phẩm chưa có trong giỏ → ProductNotFoundException")
        void rejectsProductNotInCart() {
            ProductNotFoundException e = assertThrows(ProductNotFoundException.class, () -> cart.changeQuantity(jeans, 1));

            assertEquals("P002", e.getProductId());
        }

        @Test
        @DisplayName("changeQuantity vượt tồn kho → OutOfStockException, giữ số cũ")
        void rejectsChangeOverStock() {
            cart.add(jeans, 5);

            assertThrows(OutOfStockException.class, () -> cart.changeQuantity(jeans, 21));
            assertEquals(5, cart.getQuantity(jeans));
        }

        @Test
        @DisplayName("remove: có thì bỏ, chưa có thì không làm gì")
        void removesProduct() {
            cart.add(tshirt, 2);
            cart.add(jeans, 1);

            cart.remove(tshirt);
            cart.remove(socks);

            assertEquals(0, cart.getQuantity(tshirt));
            assertEquals(1, cart.getItems().size());
        }

        @Test
        @DisplayName("getQuantity sản phẩm chưa có → 0")
        void quantityOfMissingProductIsZero() {
            assertEquals(0, cart.getQuantity(socks));
        }

        @Test
        @DisplayName("getItems không sửa được từ bên ngoài")
        void itemsAreUnmodifiable() {
            cart.add(tshirt, 2);
            Map<Product, Integer> items = cart.getItems();

            assertThrows(UnsupportedOperationException.class, () -> items.put(jeans, 1));
            assertEquals(0, cart.getQuantity(jeans));
        }
    }

    @Nested
    @DisplayName("toOrder")
    class ToOrder {

        @Test
        @DisplayName("Mỗi sản phẩm một OrderLine theo thứ tự thêm; tính tiền như Order S02")
        void createsOrderFromItems() {
            cart.add(tshirt, 1);
            cart.add(jeans, 1);
            cart.add(tshirt, 1);

            Order order = cart.toOrder(new BigDecimal("0.10"), new BigDecimal("0.08"));

            OrderLine[] lines = order.getLines();
            assertEquals(2, lines.length);
            assertSame(tshirt, lines[0].getProduct());
            assertEquals(2, lines[0].getQuantity());
            assertSame(jeans, lines[1].getProduct());
            assertEquals(new BigDecimal("729000.00"), order.finalAmount());
        }

        @Test
        @DisplayName("toOrder không trừ kho")
        void doesNotDecreaseStock() {
            cart.add(tshirt, 2);

            cart.toOrder(BigDecimal.ZERO, BigDecimal.ZERO);

            assertEquals(50, tshirt.getStock());
        }

        @Test
        @DisplayName("Giỏ rỗng → IllegalStateException")
        void rejectsEmptyCart() {
            assertThrows(IllegalStateException.class, () -> cart.toOrder(BigDecimal.ZERO, BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Rate không hợp lệ → IllegalArgumentException (theo quy tắc của Order)")
        void rejectsInvalidRates() {
            cart.add(tshirt, 1);

            assertThrows(IllegalArgumentException.class, () -> cart.toOrder(new BigDecimal("1.5"), BigDecimal.ZERO));
        }
    }

    // SPEC S07-extra – thêm 30/10: chốt giỏ bằng DiscountCode (D1 tương đương toOrder(rate, tax), D2 code null → IAE).
    @Nested
    @DisplayName("toOrder(DiscountCode, taxRate)")
    class ToOrderWithDiscountCode {

        private final DiscountCode sale10 = new DiscountCode("SALE10", new BigDecimal("0.10"));

        @Test
        @DisplayName("Mã SALE10 → giống toOrder(0.10, 0.08): 2 dòng theo thứ tự thêm, final 729000.00")
        void appliesCodeRate() {
            cart.add(tshirt, 1);
            cart.add(jeans, 1);
            cart.add(tshirt, 1);

            Order order = cart.toOrder(sale10, new BigDecimal("0.08"));

            OrderLine[] lines = order.getLines();
            assertEquals(2, lines.length);
            assertSame(tshirt, lines[0].getProduct());
            assertEquals(2, lines[0].getQuantity());
            assertSame(jeans, lines[1].getProduct());
            assertEquals(0, new BigDecimal("0.10").compareTo(order.getDiscountRate()));
            assertEquals(new BigDecimal("729000.00"), order.finalAmount());
        }

        @Test
        @DisplayName("Chốt bằng mã cũng không trừ kho")
        void doesNotDecreaseStockWithCode() {
            cart.add(tshirt, 2);

            cart.toOrder(sale10, BigDecimal.ZERO);

            assertEquals(50, tshirt.getStock());
        }

        @Test
        @DisplayName("Mã giảm 100% → final 0.00")
        void fullDiscountCode() {
            cart.add(jeans, 1);

            Order order = cart.toOrder(new DiscountCode("FREE", BigDecimal.ONE), new BigDecimal("0.08"));

            assertEquals(0, BigDecimal.ZERO.compareTo(order.finalAmount()));
        }

        @Test
        @DisplayName("code null → IllegalArgumentException")
        void rejectsNullCode() {
            cart.add(tshirt, 1);

            assertThrows(IllegalArgumentException.class, () -> cart.toOrder((DiscountCode) null, BigDecimal.ZERO));
        }

        @Test
        @DisplayName("code null kiểm trước giỏ rỗng → IllegalArgumentException, không phải IllegalStateException")
        void nullCodeCheckedBeforeEmptyCart() {
            assertThrows(IllegalArgumentException.class, () -> cart.toOrder((DiscountCode) null, BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Giỏ rỗng với mã hợp lệ → IllegalStateException")
        void rejectsEmptyCartWithCode() {
            assertThrows(IllegalStateException.class, () -> cart.toOrder(sale10, BigDecimal.ZERO));
        }

        @Test
        @DisplayName("taxRate âm → IllegalArgumentException (theo quy tắc của Order)")
        void rejectsInvalidTaxWithCode() {
            cart.add(tshirt, 1);

            assertThrows(IllegalArgumentException.class, () -> cart.toOrder(sale10, new BigDecimal("-0.01")));
        }
    }
}
