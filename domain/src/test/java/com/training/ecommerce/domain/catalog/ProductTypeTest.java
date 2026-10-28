package com.training.ecommerce.domain.catalog;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductTypeTest {

    @Test
    void mapsEverySubtype() {
        Product physical = new PhysicalProduct("P1", "A", BigDecimal.TEN, 1, BigDecimal.ONE);
        Product digital = new DigitalProduct("P2", "B", BigDecimal.TEN, 1);
        Product subscription = new SubscriptionProduct("P3", "C", BigDecimal.TEN, 1, 1);
        Product bundle = new BundleProduct("P4", "D", 1, BigDecimal.ZERO, digital);

        assertEquals(ProductType.PHYSICAL, ProductType.of(physical));
        assertEquals(ProductType.DIGITAL, ProductType.of(digital));
        assertEquals(ProductType.SUBSCRIPTION, ProductType.of(subscription));
        assertEquals(ProductType.BUNDLE, ProductType.of(bundle));
    }

    @Test
    void rejectsNullAndUnknownSubtype() {
        Product custom = new Product("X", "Custom", BigDecimal.ONE) {
            @Override
            public BigDecimal calculateFinalPrice() {
                return getBasePrice();
            }
        };

        assertThrows(IllegalArgumentException.class, () -> ProductType.of(null));
        assertThrows(IllegalArgumentException.class, () -> ProductType.of(custom));
    }
}
