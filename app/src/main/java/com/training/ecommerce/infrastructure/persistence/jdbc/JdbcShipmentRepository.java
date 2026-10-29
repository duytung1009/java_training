package com.training.ecommerce.infrastructure.persistence.jdbc;

import com.training.ecommerce.application.shipping.ShipmentRepository;
import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.domain.shipping.Shipment;
import com.training.ecommerce.domain.shipping.ShipmentEvent;
import com.training.ecommerce.domain.shipping.ShipmentStatus;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * {@link ShipmentRepository} trên PostgreSQL. Không gắn stereotype Spring (khai báo bean trong config).
 *
 * <p>{@code save} là upsert: ghi đè vận đơn cùng id (giữ {@code seq} và {@code created_at}), xóa các event cũ rồi
 * ghi lại theo thứ tự ({@code position}). Đọc lên bằng constructor khôi phục của {@link Shipment}. Nhiều câu lệnh
 * trong một {@code save} cần chạy trong transaction của caller để all-or-nothing. Partial unique index
 * {@code ux_shipments_active_order} là lớp chặn cuối cho "mỗi đơn một vận đơn còn hiệu lực".
 */
public class JdbcShipmentRepository implements ShipmentRepository {
    private static final String SELECT_SHIPMENTS = """
            SELECT id, order_id, street, city, fee, tracking_code, status, failed_attempts FROM shipments""";

    private record HeaderRow(String id, String orderId, String street, String city, BigDecimal fee,
                             String trackingCode, ShipmentStatus status, int failedAttempts) {
    }

    private final JdbcClient jdbc;

    public JdbcShipmentRepository(JdbcClient jdbc) {
        if (jdbc == null) {
            throw new IllegalArgumentException("jdbc không được null");
        }
        this.jdbc = jdbc;
    }

    /**
     * @throws IllegalArgumentException khi shipment null
     */
    @Override
    public Shipment save(Shipment shipment) {
        if (shipment == null) {
            throw new IllegalArgumentException("entity không được null");
        }
        jdbc.sql("""
                        INSERT INTO shipments (id, order_id, street, city, fee, tracking_code, status, failed_attempts)
                        VALUES (:id, :orderId, :street, :city, :fee, :trackingCode, :status, :failedAttempts)
                        ON CONFLICT (id) DO UPDATE SET order_id = EXCLUDED.order_id, street = EXCLUDED.street,
                            city = EXCLUDED.city, fee = EXCLUDED.fee, tracking_code = EXCLUDED.tracking_code,
                            status = EXCLUDED.status, failed_attempts = EXCLUDED.failed_attempts
                        """)
                .param("id", shipment.getId())
                .param("orderId", shipment.getOrderId())
                .param("street", shipment.getAddress().getStreet())
                .param("city", shipment.getAddress().getCity())
                .param("fee", shipment.getFee())
                .param("trackingCode", shipment.getTrackingCode())
                .param("status", shipment.getStatus().name())
                .param("failedAttempts", shipment.getFailedAttempts())
                .update();
        jdbc.sql("DELETE FROM shipment_events WHERE shipment_id = :id").param("id", shipment.getId()).update();
        List<ShipmentEvent> history = shipment.history();
        for (int i = 0; i < history.size(); i++) {
            jdbc.sql("""
                            INSERT INTO shipment_events (shipment_id, position, status, note)
                            VALUES (:shipmentId, :position, :status, :note)
                            """)
                    .param("shipmentId", shipment.getId())
                    .param("position", i)
                    .param("status", history.get(i).status().name())
                    .param("note", history.get(i).note())
                    .update();
        }
        return shipment;
    }

    @Override
    public Optional<Shipment> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return jdbc.sql(SELECT_SHIPMENTS + " WHERE id = :id")
                .param("id", id)
                .query(JdbcShipmentRepository::readHeader)
                .optional()
                .map(this::withHistory);
    }

    /** Thứ tự theo {@code seq}; list không sửa được. */
    @Override
    public List<Shipment> findAll() {
        return jdbc.sql(SELECT_SHIPMENTS + " ORDER BY seq")
                .query(JdbcShipmentRepository::readHeader)
                .list()
                .stream()
                .map(this::withHistory)
                .toList();
    }

    @Override
    public List<Shipment> findByOrderId(String orderId) {
        if (orderId == null) {
            return List.of();
        }
        return jdbc.sql(SELECT_SHIPMENTS + " WHERE order_id = :orderId ORDER BY seq")
                .param("orderId", orderId)
                .query(JdbcShipmentRepository::readHeader)
                .list()
                .stream()
                .map(this::withHistory)
                .toList();
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null) {
            return false;
        }
        return jdbc.sql("DELETE FROM shipments WHERE id = :id").param("id", id).update() > 0;   // event xóa theo CASCADE
    }

    @Override
    public int count() {
        return jdbc.sql("SELECT count(*) FROM shipments").query(Integer.class).single();
    }

    /** Chỉ đọc header; event nạp sau khi ResultSet đã đóng để không chạy query lồng. */
    private static HeaderRow readHeader(ResultSet rs, int rowNum) throws SQLException {
        return new HeaderRow(rs.getString("id"), rs.getString("order_id"), rs.getString("street"),
                rs.getString("city"), rs.getBigDecimal("fee"), rs.getString("tracking_code"),
                ShipmentStatus.valueOf(rs.getString("status")), rs.getInt("failed_attempts"));
    }

    private Shipment withHistory(HeaderRow row) {
        List<ShipmentEvent> history = jdbc.sql("""
                        SELECT status, note FROM shipment_events WHERE shipment_id = :id ORDER BY position
                        """)
                .param("id", row.id())
                .query((e, n) -> new ShipmentEvent(ShipmentStatus.valueOf(e.getString("status")), e.getString("note")))
                .list();
        return new Shipment(row.id(), row.orderId(), new Address(row.street(), row.city()), row.fee(),
                row.trackingCode(), row.status(), row.failedAttempts(), history);
    }
}
