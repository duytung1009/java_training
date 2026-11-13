package com.training.ecommerce.application.catalog;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

/** Validator của {@link ValidProductSpec}: báo mọi vi phạm của object, mỗi lỗi gắn vào field cụ thể. Không ném exception. */
public class ProductSpecValidator implements ConstraintValidator<ValidProductSpec, ProductSpec> {

    @Override
    public boolean isValid(ProductSpec spec, ConstraintValidatorContext context) {
        if (spec == null || spec.type() == null) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        boolean valid = true;
        switch (spec.type()) {
            case PHYSICAL -> {
                valid &= checkPrice(spec, context);
                if (spec.weightKg() == null || spec.weightKg().signum() <= 0) {
                    valid = fail(context, "weightKg", "weightKg phải lớn hơn 0 với PHYSICAL");
                }
            }
            case DIGITAL -> valid &= checkPrice(spec, context);
            case SUBSCRIPTION -> {
                valid &= checkPrice(spec, context);
                if (spec.months() == null || spec.months() <= 0) {
                    valid = fail(context, "months", "months phải lớn hơn 0 với SUBSCRIPTION");
                }
            }
            case BUNDLE -> {
                if (spec.itemIds() == null || spec.itemIds().isEmpty()) {
                    valid = fail(context, "itemIds", "Bundle phải có ít nhất 1 item");
                }
                BigDecimal rate = spec.discountRate();
                if (rate == null || rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
                    valid = fail(context, "discountRate", "discountRate phải nằm trong [0, 1] với BUNDLE");
                }
            }
        }
        return valid;
    }

    private static boolean checkPrice(ProductSpec spec, ConstraintValidatorContext context) {
        if (spec.basePrice() == null || spec.basePrice().signum() < 0) {
            return fail(context, "basePrice", "basePrice phải >= 0");
        }
        return true;
    }

    private static boolean fail(ConstraintValidatorContext context, String field, String message) {
        context.buildConstraintViolationWithTemplate(message).addPropertyNode(field).addConstraintViolation();
        return false;
    }
}
