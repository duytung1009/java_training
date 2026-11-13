package com.training.ecommerce.application.catalog;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Constraint mức class cho {@link ProductSpec}: kiểm field bắt buộc theo {@code type}.
 * <ul>
 *   <li>PHYSICAL: {@code basePrice >= 0}, {@code weightKg > 0}</li>
 *   <li>DIGITAL: {@code basePrice >= 0}</li>
 *   <li>SUBSCRIPTION: {@code basePrice >= 0}, {@code months > 0}</li>
 *   <li>BUNDLE: {@code itemIds} không rỗng, {@code discountRate} trong [0, 1]; bỏ qua {@code basePrice}</li>
 * </ul>
 * {@code type} null được coi là hợp lệ (để {@code @NotNull} báo). Mỗi lỗi gắn vào đúng field.
 */
@Documented
@Constraint(validatedBy = ProductSpecValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidProductSpec {

    String message() default "Product không hợp lệ";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
