package com.training.ecommerce.domain.order;

import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** S07: constructor khôi phục OrderLine với đơn giá snapshot. */
class OrderLineRestoreTest {

    private final Product product = new PhysicalProduct("P1", "Áo", new BigDecimal("150000"), 1, new BigDecimal("0.5"));

    @Test
    void usesGivenUnitPriceInsteadOfCurrentPrice() {
        OrderLine line = new OrderLine(product, 3, new BigDecimal("100000"));

        assertEquals(new BigDecimal("100000.00"), line.getUnitPrice());
        assertEquals(new BigDecimal("300000.00"), line.lineTotal());
    }

    @Test
    void doesNotCheckStock() {
        OrderLine line = new OrderLine(product, 99, BigDecimal.ONE);

        assertEquals(99, line.getQuantity());
    }

    @Test
    void normalizesScaleHalfUp() {
        assertEquals(new BigDecimal("1.01"), new OrderLine(product, 1, new BigDecimal("1.005")).getUnitPrice());
    }

    @Test
    void zeroPriceIsAllowed() {
        assertEquals(new BigDecimal("0.00"), new OrderLine(product, 1, BigDecimal.ZERO).getUnitPrice());
    }

    @Test
    void invalidArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new OrderLine(null, 1, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> new OrderLine(product, 0, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> new OrderLine(product, -1, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> new OrderLine(product, 1, null));
        assertThrows(IllegalArgumentException.class, () -> new OrderLine(product, 1, new BigDecimal("-0.01")));
    }
}
