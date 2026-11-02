package com.training.ecommerce.visualizer.core.model;

/**
 * Một lỗi của mô hình.
 *
 * @param cls     class liên quan, null nếu lỗi không gắn với class nào
 * @param message mô tả lỗi
 */
public record ModelError(String cls, String message) {
}
