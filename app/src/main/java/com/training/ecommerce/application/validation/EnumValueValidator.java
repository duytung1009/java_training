package com.training.ecommerce.application.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.List;

/** Validator của {@link EnumValue}. Không bao giờ ném exception. */
public class EnumValueValidator implements ConstraintValidator<EnumValue, String> {
    private List<String> allowed;
    private boolean ignoreCase;
    private boolean listAllowedInMessage;

    @Override
    public void initialize(EnumValue annotation) {
        this.allowed = Arrays.stream(annotation.enumClass().getEnumConstants()).map(Enum::name).toList();
        this.ignoreCase = annotation.ignoreCase();
        // Chỉ thay message khi người dùng không tự đặt message
        this.listAllowedInMessage = EnumValue.DEFAULT_MESSAGE.equals(annotation.message());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        boolean ok = allowed.stream().anyMatch(name -> ignoreCase ? name.equalsIgnoreCase(value) : name.equals(value));
        if (!ok && listAllowedInMessage) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Giá trị không hợp lệ, chỉ nhận: " + String.join(", ", allowed)).addConstraintViolation();
        }
        return ok;
    }
}
