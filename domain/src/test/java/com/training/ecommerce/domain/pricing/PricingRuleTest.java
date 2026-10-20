// SPEC S05 – sinh bởi java-start-backlog 21/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
package com.training.ecommerce.domain.pricing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("PricingRule (S05)")
class PricingRuleTest {

    private static final BigDecimal PRICE = new BigDecimal("150000");

    private static void assertSameValue(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual), () -> "expected " + expected + " but was " + actual);
    }

    @Nested
    @DisplayName("identity / lambda")
    class IdentityAndLambda {

        @Test
        @DisplayName("identity trả nguyên giá")
        void identityKeepsPrice() {
            assertSameValue(PRICE, PricingRule.identity().apply(PRICE));
        }

        @Test
        @DisplayName("PricingRule là functional interface: gán được lambda")
        void acceptsLambda() {
            PricingRule plusShipping = price -> price.add(new BigDecimal("20000"));

            assertSameValue(new BigDecimal("170000"), plusShipping.apply(PRICE));
        }
    }

    @Nested
    @DisplayName("percentOff")
    class PercentOff {

        @Test
        @DisplayName("Giảm 10% → 135000.00")
        void tenPercent() {
            assertEquals(new BigDecimal("135000.00"), PricingRule.percentOff(new BigDecimal("0.10")).apply(PRICE));
        }

        @Test
        @DisplayName("Làm tròn HALF_UP scale 2: 12345.67 × 0.85 = 10493.8195 → 10493.82")
        void roundsHalfUp() {
            assertEquals(new BigDecimal("10493.82"),
                    PricingRule.percentOff(new BigDecimal("0.15")).apply(new BigDecimal("12345.67")));
        }

        @Test
        @DisplayName("Giảm 100% → 0.00")
        void fullDiscount() {
            assertEquals(new BigDecimal("0.00"), PricingRule.percentOff(BigDecimal.ONE).apply(PRICE));
        }

        @ParameterizedTest(name = "rate = {0}")
        @ValueSource(strings = {"0", "-0.1", "1.01"})
        @DisplayName("rate ngoài (0, 1] → IllegalArgumentException")
        void rejectsInvalidRate(String rate) {
            assertThrows(IllegalArgumentException.class, () -> PricingRule.percentOff(new BigDecimal(rate)));
        }

        @Test
        @DisplayName("rate null → IllegalArgumentException")
        void rejectsNullRate() {
            assertThrows(IllegalArgumentException.class, () -> PricingRule.percentOff(null));
        }
    }

    @Nested
    @DisplayName("amountOff")
    class AmountOff {

        @Test
        @DisplayName("Trừ 50000 → 100000.00")
        void subtractsAmount() {
            assertEquals(new BigDecimal("100000.00"), PricingRule.amountOff(new BigDecimal("50000")).apply(PRICE));
        }

        @Test
        @DisplayName("Trừ quá giá → 0.00, không âm")
        void neverNegative() {
            assertEquals(new BigDecimal("0.00"), PricingRule.amountOff(new BigDecimal("200000")).apply(PRICE));
        }

        @Test
        @DisplayName("Trừ 0 → giữ giá, scale 2")
        void zeroAmount() {
            assertEquals(new BigDecimal("150000.00"), PricingRule.amountOff(BigDecimal.ZERO).apply(PRICE));
        }

        @Test
        @DisplayName("amount âm / null → IllegalArgumentException")
        void rejectsInvalidAmount() {
            assertThrows(IllegalArgumentException.class, () -> PricingRule.amountOff(new BigDecimal("-1")));
            assertThrows(IllegalArgumentException.class, () -> PricingRule.amountOff(null));
        }
    }

    @Nested
    @DisplayName("andThen / chain")
    class Compose {

        private final PricingRule tenPercent = PricingRule.percentOff(new BigDecimal("0.10"));
        private final PricingRule minus50k = PricingRule.amountOff(new BigDecimal("50000"));

        @Test
        @DisplayName("andThen chạy this trước, next sau: thứ tự đổi thì kết quả đổi")
        void andThenOrderMatters() {
            // 150.000 × 0.9 − 50.000
            assertEquals(new BigDecimal("85000.00"), tenPercent.andThen(minus50k).apply(PRICE));
            // (150.000 − 50.000) × 0.9
            assertEquals(new BigDecimal("90000.00"), minus50k.andThen(tenPercent).apply(PRICE));
        }

        @Test
        @DisplayName("andThen với lambda")
        void andThenWithLambda() {
            assertSameValue(new BigDecimal("155000"), tenPercent.andThen(price -> price.add(new BigDecimal("20000"))).apply(PRICE));
        }

        @Test
        @DisplayName("andThen(null) → IllegalArgumentException")
        void andThenRejectsNull() {
            assertThrows(IllegalArgumentException.class, () -> tenPercent.andThen(null));
        }

        @Test
        @DisplayName("chain áp dụng theo thứ tự trong list")
        void chainAppliesInOrder() {
            assertEquals(new BigDecimal("85000.00"), PricingRule.chain(List.of(tenPercent, minus50k)).apply(PRICE));
            assertEquals(new BigDecimal("90000.00"), PricingRule.chain(List.of(minus50k, tenPercent)).apply(PRICE));
        }

        @Test
        @DisplayName("chain 3 rule: 150.000 × 0.9 − 50.000 rồi × 0.9 = 76500.00")
        void chainThreeRules() {
            assertEquals(new BigDecimal("76500.00"),
                    PricingRule.chain(List.of(tenPercent, minus50k, tenPercent)).apply(PRICE));
        }

        @Test
        @DisplayName("chain(List.of()) → identity")
        void emptyChainIsIdentity() {
            assertSameValue(PRICE, PricingRule.chain(List.of()).apply(PRICE));
        }

        @Test
        @DisplayName("chain null / chứa null → IllegalArgumentException")
        void chainRejectsNull() {
            assertThrows(IllegalArgumentException.class, () -> PricingRule.chain(null));
            assertThrows(IllegalArgumentException.class, () -> PricingRule.chain(Arrays.asList(tenPercent, null)));
        }
    }
}
