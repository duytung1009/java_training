package com.training.ecommerce.application.validation;

import com.training.ecommerce.domain.catalog.Category;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Test thuần Bean Validation (không Spring) cho {@link EnumValue}. */
class EnumValueValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private record Holder(@EnumValue(enumClass = Category.class) String category) {
    }

    private record StrictHolder(@EnumValue(enumClass = Category.class, ignoreCase = false) String category) {
    }

    @Test
    void acceptsAnyCase() {
        assertThat(validator.validate(new Holder("fashion"))).isEmpty();
        assertThat(validator.validate(new Holder("FASHION"))).isEmpty();
    }

    @Test
    void rejectsUnknown() {
        Set<ConstraintViolation<Holder>> violations = validator.validate(new Holder("food"));

        assertThat(violations).hasSize(1);
        ConstraintViolation<Holder> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("category");
        assertThat(violation.getMessage()).contains("FASHION", "OTHER");
    }

    private record CustomMessageHolder(@EnumValue(enumClass = Category.class, message = "category sai") String category) {
    }

    @Test
    void customMessageIsKept() {
        Set<ConstraintViolation<CustomMessageHolder>> violations = validator.validate(new CustomMessageHolder("food"));

        assertThat(violations).extracting(ConstraintViolation::getMessage).containsExactly("category sai");
    }

    @Test
    void nullIsValid() {
        assertThat(validator.validate(new Holder(null))).isEmpty();
        assertThat(validator.validate(new Holder("  "))).isEmpty();
    }

    @Test
    void caseSensitiveWhenIgnoreCaseFalse() {
        assertThat(validator.validate(new StrictHolder("fashion"))).hasSize(1);
        assertThat(validator.validate(new StrictHolder("FASHION"))).isEmpty();
    }
}
