package com.training.ecommerce.visualizer.world;

import java.util.List;

/**
 * Ảnh chụp thế giới để vẽ: danh sách panel theo thứ tự cố định.
 *
 * @param panels các panel theo thứ tự cố định
 */
public record WorldView(List<PanelView> panels) {
}
