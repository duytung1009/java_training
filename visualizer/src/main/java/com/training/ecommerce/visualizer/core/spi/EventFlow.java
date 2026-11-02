package com.training.ecommerce.visualizer.core.spi;

/**
 * Flow trên bản đồ do một domain event sinh ra.
 *
 * @param from    panel/node id nguồn
 * @param to      panel/node id đích
 * @param label   nhãn hiển thị; null thì lấy tên đơn giản của class event
 * @param context ngữ cảnh bổ sung hiện trong log (vd. id đơn hàng); có thể null
 */
public record EventFlow(String from, String to, String label, String context) {

    /**
     * @throws IllegalArgumentException nếu from hoặc to null/rỗng
     */
    public EventFlow {
        if (from == null || from.isBlank() || to == null || to.isBlank()) {
            throw new IllegalArgumentException("from và to của event flow không được rỗng");
        }
    }
}
