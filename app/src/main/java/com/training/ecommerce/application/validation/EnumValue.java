package com.training.ecommerce.application.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Chuỗi phải là tên một hằng của enum {@link #enumClass()}. {@code null} hoặc rỗng được coi là hợp lệ
 * (để {@code @NotNull} / {@code @NotBlank} lo việc bắt buộc).
 */
@Documented
@Constraint(validatedBy = EnumValueValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumValue {

    /** Enum cho phép; message mặc định liệt kê các hằng của nó. */
    Class<? extends Enum<?>> enumClass();

    /** So khớp không phân biệt hoa thường (mặc định có). */
    boolean ignoreCase() default true;

    /** Message mặc định; khi giữ mặc định thì validator thay bằng message liệt kê các hằng cho phép. */
    String DEFAULT_MESSAGE = "Giá trị không hợp lệ, không nằm trong danh sách cho phép";

    /** Ghi đè message thì validator dùng nguyên message đó, không liệt kê hằng. */
    String message() default DEFAULT_MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
