package com.training.ecommerce.visualizer.core.model;

import com.training.ecommerce.visualizer.core.view.NodeView;

/** Quy node trên bản đồ về tên class domain. */
final class NodeClasses {

    private NodeClasses() {
    }

    /**
     * Tên class của node: object lấy {@code cls}; collection lấy tham số generic ({@code List<Order>} thành
     * {@code Order}); area và node không có cls trả về null.
     */
    static String classOf(NodeView node) {
        if (node == null || node.cls() == null) {
            return null;
        }
        return switch (node.kind()) {
            case NodeView.OBJECT -> node.cls();
            case NodeView.COLLECTION -> {
                int open = node.cls().indexOf('<');
                int close = node.cls().lastIndexOf('>');
                yield open >= 0 && close > open ? node.cls().substring(open + 1, close).trim() : null;
            }
            default -> null;
        };
    }
}
