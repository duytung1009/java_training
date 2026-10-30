package com.training.ecommerce.visualizer.core.model;

/**
 * Mô hình hiện hành kèm nguồn gốc.
 *
 * @param origin {@code file}, {@code classpath} hoặc {@code suggestion}
 * @param model  mô hình
 */
public record ModelSnapshot(String origin, DomainModel model) {
}
