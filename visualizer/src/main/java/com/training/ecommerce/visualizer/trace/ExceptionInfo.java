package com.training.ecommerce.visualizer.trace;

/**
 * Thông tin exception của một event THROWN.
 *
 * @param type    tên đơn giản của class exception
 * @param message message của exception (có thể null)
 */
public record ExceptionInfo(String type, String message) {

    /**
     * @throws IllegalArgumentException nếu e null
     */
    public static ExceptionInfo of(Throwable e) {
        if (e == null) {
            throw new IllegalArgumentException("exception không được null");
        }
        return new ExceptionInfo(e.getClass().getSimpleName(), e.getMessage());
    }
}
