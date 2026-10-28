package com.training.ecommerce.domain.order;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** S07: id của Order (constructor có id và constructor tự sinh id). */
class OrderIdTest {

    @Test
    void explicitIdIsKept() {
        Order order = new Order("O-1", new BigDecimal("0.1"), new BigDecimal("0.08"));

        assertEquals("O-1", order.getId());
        assertEquals(new BigDecimal("0.1"), order.getDiscountRate());
        assertEquals(new BigDecimal("0.08"), order.getTaxRate());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void blankIdIsRejected(String id) {
        assertThrows(IllegalArgumentException.class, () -> new Order(id, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void invalidRatesStillRejectedWithId() {
        assertThrows(IllegalArgumentException.class, () -> new Order("O-1", new BigDecimal("1.1"), BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new Order("O-1", BigDecimal.ZERO, new BigDecimal("-1")));
    }

    @Test
    void oldConstructorGeneratesDistinctNonBlankIds() {
        Order a = new Order(BigDecimal.ZERO, BigDecimal.ZERO);
        Order b = new Order(BigDecimal.ZERO, BigDecimal.ZERO);

        assertNotEquals("", a.getId().trim());
        assertNotEquals(a.getId(), b.getId());
    }

    @Test
    void quantitiesByProductIdSumsDuplicatesKeepingOrderAndIsUnmodifiable() {
        com.training.ecommerce.domain.catalog.Product a =
                new com.training.ecommerce.domain.catalog.DigitalProduct("A", "A", BigDecimal.TEN, 10);
        com.training.ecommerce.domain.catalog.Product b =
                new com.training.ecommerce.domain.catalog.DigitalProduct("B", "B", BigDecimal.TEN, 10);
        Order order = new Order(BigDecimal.ZERO, BigDecimal.ZERO);
        order.addLine(new OrderLine(b, 2));
        order.addLine(new OrderLine(a, 1));
        order.addLine(new OrderLine(b, 3));

        java.util.Map<String, Integer> quantities = order.quantitiesByProductId();

        assertEquals(java.util.List.of("B", "A"), java.util.List.copyOf(quantities.keySet()));
        assertEquals(5, quantities.get("B"));
        assertEquals(1, quantities.get("A"));
        assertThrows(UnsupportedOperationException.class, () -> quantities.put("C", 1));
        assertEquals(java.util.Map.of(), new Order(BigDecimal.ZERO, BigDecimal.ZERO).quantitiesByProductId());
    }
}
