package com.training.ecommerce.application.catalog;

import com.training.ecommerce.domain.catalog.ProductType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Test thuần Bean Validation (không Spring) cho constraint mức class {@link ValidProductSpec}. */
class ProductSpecValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @ValidProductSpec
    private record Spec(ProductType type, String name, BigDecimal basePrice, int stock, BigDecimal weightKg, Integer months,
                        BigDecimal discountRate, List<String> itemIds, String category) implements ProductSpec {
        Spec(ProductType type, BigDecimal basePrice, BigDecimal weightKg, Integer months,
             BigDecimal discountRate, List<String> itemIds, String category) {
            this(type, "X", basePrice, 1, weightKg, months, discountRate, itemIds, category);
        }
    }

    private Set<String> invalidFields(Spec spec) {
        return validator.validate(spec).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    void physicalRequiresPositiveWeight() {
        Spec missing = new Spec(ProductType.PHYSICAL, new BigDecimal("10"), null, null, null, null, null);
        Spec zero = new Spec(ProductType.PHYSICAL, new BigDecimal("10"), BigDecimal.ZERO, null, null, null, null);

        assertThat(validator.validate(missing)).hasSize(1);
        assertThat(invalidFields(missing)).containsExactly("weightKg");
        assertThat(validator.validate(zero)).hasSize(1);
        assertThat(invalidFields(zero)).containsExactly("weightKg");
    }

    @Test
    void subscriptionRequiresMonths() {
        Spec spec = new Spec(ProductType.SUBSCRIPTION, new BigDecimal("10"), null, null, null, null, null);

        assertThat(validator.validate(spec)).hasSize(1);
        assertThat(invalidFields(spec)).containsExactly("months");
    }

    @Test
    void bundleRules() {
        Spec spec = new Spec(ProductType.BUNDLE, null, null, null, new BigDecimal("1.5"), List.of(), null);

        Set<ConstraintViolation<Spec>> violations = validator.validate(spec);

        assertThat(violations).hasSize(2);
        assertThat(invalidFields(spec)).containsExactlyInAnyOrder("itemIds", "discountRate");
    }

    @Test
    void nullTypeLeftToNotNull() {
        Spec spec = new Spec(null, null, null, null, null, null, null);

        assertThat(validator.validate(spec)).isEmpty();
    }

    @Test
    void ignoresFieldsOfOtherTypes() {
        Spec spec = new Spec(ProductType.PHYSICAL, new BigDecimal("10"), BigDecimal.ONE, 3, null, null, null);

        assertThat(validator.validate(spec)).isEmpty();
    }

    @Test
    void reportsEveryViolationAtOnce() {
        Spec spec = new Spec(ProductType.PHYSICAL, new BigDecimal("-1"), null, null, null, null, null);

        assertThat(invalidFields(spec)).containsExactlyInAnyOrder("basePrice", "weightKg");
    }
}
