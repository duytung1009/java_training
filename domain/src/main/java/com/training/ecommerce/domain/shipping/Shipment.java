package com.training.ecommerce.domain.shipping;

import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.shared.repository.Identifiable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Vận đơn: aggregate riêng, tham chiếu đơn qua {@code orderId}, giữ địa chỉ, phí ship và trạng thái giao.
 *
 * <p>Luồng trạng thái:
 * CREATED → IN_TRANSIT → DELIVERED; IN_TRANSIT → RETURNING → RETURNED; CREATED → CANCELLED.
 * Chuyển trạng thái không có trong sơ đồ → {@link IllegalStateException} (message nêu trạng thái hiện tại),
 * trạng thái, số lần giao thất bại và history giữ nguyên. Mỗi lần chuyển trạng thái / giao thất bại thêm một
 * {@link ShipmentEvent}.
 */
public class Shipment implements Identifiable<String> {
    /** Số lần giao thất bại tối đa; lần thứ MAX_ATTEMPTS tự chuyển sang RETURNING. */
    public static final int MAX_ATTEMPTS = 3;

    /**
     * Tạo vận đơn mới: id UUID ngẫu nhiên, trackingCode sinh tự động (không rỗng), status CREATED,
     * failedAttempts 0, history có đúng 1 event CREATED, fee được đưa về scale 2 (HALF_UP).
     *
     * @throws IllegalArgumentException khi orderId null/rỗng, address null, fee null hoặc âm
     */
    public Shipment(String orderId, Address address, BigDecimal fee) {
        throw new UnsupportedOperationException("TODO S07-extra");
    }

    @Override
    public String getId() {
        throw new UnsupportedOperationException("TODO S07-extra");
    }

    public String getOrderId() {
        throw new UnsupportedOperationException("TODO S07-extra");
    }

    public Address getAddress() {
        throw new UnsupportedOperationException("TODO S07-extra");
    }

    /** Phí ship, scale 2. */
    public BigDecimal getFee() {
        throw new UnsupportedOperationException("TODO S07-extra");
    }

    public String getTrackingCode() {
        throw new UnsupportedOperationException("TODO S07-extra");
    }

    public ShipmentStatus getStatus() {
        throw new UnsupportedOperationException("TODO S07-extra");
    }

    public int getFailedAttempts() {
        throw new UnsupportedOperationException("TODO S07-extra");
    }

    /**
     * @return bản copy lịch sử theo thứ tự thời gian, không sửa được
     */
    public List<ShipmentEvent> history() {
        throw new UnsupportedOperationException("TODO S07-extra");
    }

    /**
     * Lấy hàng: CREATED → IN_TRANSIT.
     *
     * @throws IllegalStateException khi không ở CREATED
     */
    public void pickUp() {
        throw new UnsupportedOperationException("TODO S07-extra");
    }

    /**
     * Giao thành công: IN_TRANSIT → DELIVERED.
     *
     * @throws IllegalStateException khi không ở IN_TRANSIT
     */
    public void markDelivered() {
        throw new UnsupportedOperationException("TODO S07-extra");
    }

    /**
     * Giao thất bại một lần: failedAttempts + 1, thêm event kèm lý do; giữ IN_TRANSIT,
     * riêng lần thứ {@link #MAX_ATTEMPTS} tự chuyển sang RETURNING. Lý do được kiểm trước trạng thái.
     *
     * @throws IllegalArgumentException khi reason null/rỗng
     * @throws IllegalStateException    khi không ở IN_TRANSIT
     */
    public void failAttempt(String reason) {
        throw new UnsupportedOperationException("TODO S07-extra");
    }

    /**
     * Người nhận từ chối: IN_TRANSIT → RETURNING. Lý do được kiểm trước trạng thái.
     *
     * @throws IllegalArgumentException khi reason null/rỗng
     * @throws IllegalStateException    khi không ở IN_TRANSIT
     */
    public void refuse(String reason) {
        throw new UnsupportedOperationException("TODO S07-extra");
    }

    /**
     * Hàng đã về kho: RETURNING → RETURNED.
     *
     * @throws IllegalStateException khi không ở RETURNING
     */
    public void markReturned() {
        throw new UnsupportedOperationException("TODO S07-extra");
    }

    /**
     * Huỷ vận đơn: chỉ ở CREATED → CANCELLED (đã lấy hàng thì phải refuse → markReturned).
     *
     * @throws IllegalStateException khi không ở CREATED
     */
    public void cancel() {
        throw new UnsupportedOperationException("TODO S07-extra");
    }
}
