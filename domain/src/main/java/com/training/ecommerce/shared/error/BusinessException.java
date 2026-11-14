package com.training.ecommerce.shared.error;

/**
 * Cha chung của lỗi nghiệp vụ. Mỗi lỗi mang một {@link Category} (nhóm lỗi, tầng giao tiếp tự map sang mã
 * phản hồi) và một {@code code} ổn định, đọc được bằng máy (ví dụ {@code OUT_OF_STOCK}).
 *
 * <p>Domain là Java thuần nên không biết HTTP status; việc map {@code Category} → status nằm ở tầng interfaces.
 */
public abstract class BusinessException extends RuntimeException {

    /** Nhóm lỗi nghiệp vụ. */
    public enum Category {
        /** Không tìm thấy tài nguyên. */
        NOT_FOUND,
        /** Xung đột với trạng thái hiện tại (hết hàng, chuyển trạng thái sai, đang được tham chiếu...). */
        CONFLICT
    }

    private final Category category;
    private final String code;

    /**
     * @throws IllegalArgumentException nếu category null, hoặc code null / rỗng
     */
    protected BusinessException(Category category, String code, String message) {
        this(category, code, message, null);
    }

    /**
     * @throws IllegalArgumentException nếu category null, hoặc code null / rỗng
     */
    protected BusinessException(Category category, String code, String message, Throwable cause) {
        super(message, cause);
        if (category == null) {
            throw new IllegalArgumentException("category không được null");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code không được null hoặc rỗng");
        }
        this.category = category;
        this.code = code;
    }

    public Category getCategory() {
        return category;
    }

    public String getCode() {
        return code;
    }
}
