package com.training.ecommerce.domain.shipping;

import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.shared.repository.Identifiable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Vận đơn: aggregate riêng, tham chiếu đơn qua {@code orderId}, giữ địa chỉ, phí ship và trạng thái giao.
 *
 * <p>Luồng trạng thái:
 * CREATED → IN_TRANSIT → DELIVERED; IN_TRANSIT → RETURNING → RETURNED; CREATED → CANCELLED.
 * Chuyển trạng thái không có trong sơ đồ → {@link IllegalShipmentTransitionException} (message nêu trạng thái hiện tại),
 * trạng thái, số lần giao thất bại và history giữ nguyên. Mỗi lần chuyển trạng thái / giao thất bại thêm một
 * {@link ShipmentEvent}.
 */
public class Shipment implements Identifiable<String> {
    /** Số lần giao thất bại tối đa; lần thứ MAX_ATTEMPTS tự chuyển sang RETURNING. */
    public static final int MAX_ATTEMPTS = 3;

    private static final int MONEY_SCALE = 2;

    private final String id;
    private final String orderId;
    private final Address address;
    private final BigDecimal fee;
    private final String trackingCode;
    private ShipmentStatus status;
    private int failedAttempts;
    private final List<ShipmentEvent> history;

    /**
     * Tạo vận đơn mới: id UUID ngẫu nhiên, trackingCode sinh tự động (không rỗng), status CREATED,
     * failedAttempts 0, history có đúng 1 event CREATED, fee được đưa về scale 2 (HALF_UP).
     *
     * @throws IllegalArgumentException khi orderId null/rỗng, address null, fee null hoặc âm
     */
    public Shipment(String orderId, Address address, BigDecimal fee) {
        this(UUID.randomUUID().toString(), orderId, address, fee, newTrackingCode(),
                ShipmentStatus.CREATED, 0, List.of(new ShipmentEvent(ShipmentStatus.CREATED, null)));
    }

    /**
     * Khôi phục vận đơn đã lưu (dùng cho persistence): giữ nguyên mọi field, history được copy.
     *
     * @throws IllegalArgumentException khi id / orderId / trackingCode null/rỗng, address / status / history null,
     *                                  fee null hoặc âm, failedAttempts ngoài [0, MAX_ATTEMPTS],
     *                                  history rỗng hoặc chứa null
     */
    public Shipment(String id, String orderId, Address address, BigDecimal fee, String trackingCode,
                    ShipmentStatus status, int failedAttempts, List<ShipmentEvent> history) {
        requireNotBlank(id, "id");
        requireNotBlank(orderId, "orderId");
        requireNotBlank(trackingCode, "trackingCode");
        if (address == null) {
            throw new IllegalArgumentException("address không được null");
        }
        if (fee == null || fee.signum() < 0) {
            throw new IllegalArgumentException("fee phải >= 0");
        }
        if (status == null) {
            throw new IllegalArgumentException("status không được null");
        }
        if (failedAttempts < 0 || failedAttempts > MAX_ATTEMPTS) {
            throw new IllegalArgumentException("failedAttempts phải trong [0, " + MAX_ATTEMPTS + "]");
        }
        if (history == null || history.isEmpty() || history.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("history không được null, rỗng hoặc chứa null");
        }
        this.id = id;
        this.orderId = orderId;
        this.address = address;
        this.fee = fee.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.trackingCode = trackingCode;
        this.status = status;
        this.failedAttempts = failedAttempts;
        this.history = new ArrayList<>(history);
    }

    @Override
    public String getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public Address getAddress() {
        return address;
    }

    /** Phí ship, scale 2. */
    public BigDecimal getFee() {
        return fee;
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    /**
     * @return bản copy lịch sử theo thứ tự thời gian, không sửa được
     */
    public List<ShipmentEvent> history() {
        return List.copyOf(history);
    }

    /**
     * Lấy hàng: CREATED → IN_TRANSIT.
     *
     * @throws IllegalShipmentTransitionException khi không ở CREATED
     */
    public void pickUp() {
        requireStatus(ShipmentStatus.CREATED, "pickUp");
        moveTo(ShipmentStatus.IN_TRANSIT, null);
    }

    /**
     * Giao thành công: IN_TRANSIT → DELIVERED.
     *
     * @throws IllegalShipmentTransitionException khi không ở IN_TRANSIT
     */
    public void markDelivered() {
        requireStatus(ShipmentStatus.IN_TRANSIT, "markDelivered");
        moveTo(ShipmentStatus.DELIVERED, null);
    }

    /**
     * Giao thất bại một lần: failedAttempts + 1, thêm event kèm lý do; giữ IN_TRANSIT,
     * riêng lần thứ {@link #MAX_ATTEMPTS} tự chuyển sang RETURNING. Lý do được kiểm trước trạng thái.
     *
     * @throws IllegalArgumentException khi reason null/rỗng
     * @throws IllegalShipmentTransitionException khi không ở IN_TRANSIT
     */
    public void failAttempt(String reason) {
        requireNotBlank(reason, "reason");
        requireStatus(ShipmentStatus.IN_TRANSIT, "failAttempt");
        failedAttempts++;
        // Lần thất bại cuối đổi sang RETURNING, các lần trước vẫn ghi event IN_TRANSIT kèm lý do
        moveTo(failedAttempts >= MAX_ATTEMPTS ? ShipmentStatus.RETURNING : ShipmentStatus.IN_TRANSIT, reason);
    }

    /**
     * Người nhận từ chối: IN_TRANSIT → RETURNING. Lý do được kiểm trước trạng thái.
     *
     * @throws IllegalArgumentException khi reason null/rỗng
     * @throws IllegalShipmentTransitionException khi không ở IN_TRANSIT
     */
    public void refuse(String reason) {
        requireNotBlank(reason, "reason");
        requireStatus(ShipmentStatus.IN_TRANSIT, "refuse");
        moveTo(ShipmentStatus.RETURNING, reason);
    }

    /**
     * Hàng đã về kho: RETURNING → RETURNED.
     *
     * @throws IllegalShipmentTransitionException khi không ở RETURNING
     */
    public void markReturned() {
        requireStatus(ShipmentStatus.RETURNING, "markReturned");
        moveTo(ShipmentStatus.RETURNED, null);
    }

    /**
     * Huỷ vận đơn: chỉ ở CREATED → CANCELLED (đã lấy hàng thì phải refuse → markReturned).
     *
     * @throws IllegalShipmentTransitionException khi không ở CREATED
     */
    public void cancel() {
        requireStatus(ShipmentStatus.CREATED, "cancel");
        moveTo(ShipmentStatus.CANCELLED, null);
    }

    /** Kiểm trạng thái hiện tại trước khi đổi bất cứ thứ gì, để lỗi không để lại thay đổi nửa vời. */
    private void requireStatus(ShipmentStatus expected, String action) {
        if (status != expected) {
            throw new IllegalShipmentTransitionException(action, status, expected);
        }
    }

    /** Đổi trạng thái và ghi event tương ứng; chỉ gọi sau khi đã validate. */
    private void moveTo(ShipmentStatus next, String note) {
        status = next;
        history.add(new ShipmentEvent(next, note));
    }

    private static void requireNotBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " không được rỗng");
        }
    }

    /** "SHP-" + 8 ký tự hex in hoa lấy từ UUID ngẫu nhiên. */
    private static String newTrackingCode() {
        return "SHP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
