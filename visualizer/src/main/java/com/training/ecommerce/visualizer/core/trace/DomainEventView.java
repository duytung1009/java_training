package com.training.ecommerce.visualizer.core.trace;

/**
 * Một domain event đã rút được trong một lời gọi, để hiện trong log.
 *
 * @param id      id trong lượt chạy, dạng {@code ev-N}
 * @param type    tên đơn giản của class event
 * @param context ngữ cảnh từ mapper, null nếu không có
 * @param payload {@code String.valueOf(event)}
 * @param from    panel id nguồn của flow, null nếu mapper không ánh xạ event
 * @param to      panel id đích của flow, null nếu mapper không ánh xạ event
 */
public record DomainEventView(String id, String type, String context, String payload, String from, String to) {
}
