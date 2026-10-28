// SPEC S05 – sinh bởi java-start-backlog 21/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
package com.training.ecommerce.application.pricing;

import com.training.ecommerce.domain.catalog.DigitalProduct;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.SubscriptionProduct;
import com.training.ecommerce.domain.pricing.PricingRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("PricingEngine.calculateRevenue(products, PricingRule) (S05)")
class PricingEngineRuleTest {

    private final PricingEngine engine = new PricingEngine();

    private final Product tshirt = new PhysicalProduct("P001", "Áo thun nam", new BigDecimal("150000"), 50, new BigDecimal("0.3"));
    private final Product office = new DigitalProduct("D001", "Office 2024", new BigDecimal("1000000"), 200);
    private final Product claudePro = new SubscriptionProduct("S001", "Gói Claude Pro", new BigDecimal("499000"), 12, 100);

    @Test
    @DisplayName("identity → bằng calculateRevenue(products) bản S03")
    void identityMatchesOldOverload() {
        List<Product> products = List.of(tshirt, office, claudePro);

        assertEquals(engine.calculateRevenue(products), engine.calculateRevenue(products, PricingRule.identity()));
        assertEquals(new BigDecimal("7138000.00"), engine.calculateRevenue(products, PricingRule.identity()));
    }

    @Test
    @DisplayName("Rule áp lên finalPrice từng product rồi mới cộng")
    void appliesRulePerProduct() {
        // 135.000 + 900.000
        assertEquals(new BigDecimal("1035000.00"),
                engine.calculateRevenue(List.of(tshirt, office), PricingRule.percentOff(new BigDecimal("0.10"))));
        // max(150.000 − 200.000, 0) + 800.000: trừ theo từng product, không trừ trên tổng
        assertEquals(new BigDecimal("800000.00"),
                engine.calculateRevenue(List.of(tshirt, office), PricingRule.amountOff(new BigDecimal("200000"))));
    }

    @Test
    @DisplayName("Rule là lambda / chain")
    void acceptsLambdaAndChain() {
        PricingRule flatFee = price -> price.add(new BigDecimal("10000"));

        // 160.000 + 1.010.000
        assertEquals(new BigDecimal("1170000.00"), engine.calculateRevenue(List.of(tshirt, office), flatFee));
        // (150.000 × 0.9 + 10.000) + (1.000.000 × 0.9 + 10.000)
        assertEquals(new BigDecimal("1055000.00"), engine.calculateRevenue(List.of(tshirt, office),
                PricingRule.chain(List.of(PricingRule.percentOff(new BigDecimal("0.10")), flatFee))));
    }

    @Test
    @DisplayName("List rỗng → 0.00")
    void emptyListIsZero() {
        assertEquals(new BigDecimal("0.00"), engine.calculateRevenue(List.of(), PricingRule.identity()));
    }

    @Test
    @DisplayName("products null / rule null / có phần tử null → IllegalArgumentException")
    void rejectsNulls() {
        assertThrows(IllegalArgumentException.class, () -> engine.calculateRevenue(null, PricingRule.identity()));
        assertThrows(IllegalArgumentException.class, () -> engine.calculateRevenue(List.of(tshirt), null));
        assertThrows(IllegalArgumentException.class,
                () -> engine.calculateRevenue(Arrays.asList(tshirt, null), PricingRule.identity()));
    }
}
