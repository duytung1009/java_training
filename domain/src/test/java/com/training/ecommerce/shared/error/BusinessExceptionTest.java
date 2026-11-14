package com.training.ecommerce.shared.error;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    private static class Sample extends BusinessException {
        Sample(Category category, String code, String message) {
            super(category, code, message);
        }

        Sample(Category category, String code, String message, Throwable cause) {
            super(category, code, message, cause);
        }
    }

    @Test
    void giuCategoryCodeVaMessage() {
        Sample e = new Sample(BusinessException.Category.CONFLICT, "SAMPLE", "thông điệp");
        assertEquals(BusinessException.Category.CONFLICT, e.getCategory());
        assertEquals("SAMPLE", e.getCode());
        assertEquals("thông điệp", e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    void giuCause() {
        Throwable cause = new RuntimeException("gốc");
        Sample e = new Sample(BusinessException.Category.NOT_FOUND, "SAMPLE", "m", cause);
        assertSame(cause, e.getCause());
    }

    @Test
    void categoryNullBiTuChoi() {
        assertThrows(IllegalArgumentException.class, () -> new Sample(null, "SAMPLE", "m"));
    }

    @Test
    void codeNullHoacRongBiTuChoi() {
        assertThrows(IllegalArgumentException.class,
                () -> new Sample(BusinessException.Category.NOT_FOUND, null, "m"));
        assertThrows(IllegalArgumentException.class,
                () -> new Sample(BusinessException.Category.NOT_FOUND, "  ", "m"));
    }
}
