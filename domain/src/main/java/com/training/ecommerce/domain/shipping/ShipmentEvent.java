package com.training.ecommerce.domain.shipping;

/**
 * Một mốc trong lịch sử vận đơn: trạng thái sau khi chuyển (hoặc sau lần giao thất bại) kèm ghi chú.
 *
 * @param status trạng thái của vận đơn tại thời điểm ghi event
 * @param note   ghi chú (vd. lý do giao thất bại), có thể null
 */
public record ShipmentEvent(ShipmentStatus status, String note) {
}
