package com.training.ecommerce.visualizer.world;

import java.util.List;

/**
 * Một panel trên bản đồ, ứng với một object domain.
 *
 * @param id    panel id (vd. "warehouse:WH-HN", "hub")
 * @param type  loại panel (warehouse, hub, cart, pricing, order, zone)
 * @param title tiêu đề hiển thị
 * @param items các ô con
 */
public record PanelView(String id, String type, String title, List<ItemView> items) {
}
