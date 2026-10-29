package com.training.ecommerce.application.shipping;

import com.training.ecommerce.application.inventory.StockStore;
import com.training.ecommerce.application.order.OrderNotFoundException;
import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.shipping.Shipment;
import com.training.ecommerce.domain.shipping.ShippingFeeCalculator;
import com.training.ecommerce.shared.repository.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Use case vận đơn: tạo vận đơn cho đơn, đẩy vận đơn qua vòng đời và hoàn kho khi hàng không đến tay khách.
 *
 * <p>Mọi chuyển trạng thái nằm trong {@link Shipment}; service chỉ nạp, gọi domain, (hoàn kho) rồi lưu.
 * {@link #markReturned} và {@link #cancel} chạy trong một transaction: đổi trạng thái + hoàn kho cùng commit hoặc
 * cùng rollback; domain ném lỗi (chuyển trạng thái sai) thì chưa hoàn kho. {@link #deliver} không hoàn kho.
 * Profile {@code local} không có TransactionManager nên annotation không có tác dụng.
 */
@Service
public class ShipmentService {
    private final Repository<Order, String> orders;
    private final ShipmentRepository shipments;
    private final StockStore stock;

    /**
     * @throws IllegalArgumentException khi một tham số null
     */
    public ShipmentService(Repository<Order, String> orders, ShipmentRepository shipments, StockStore stock) {
        if (orders == null || shipments == null || stock == null) {
            throw new IllegalArgumentException("orders, shipments, stock không được null");
        }
        this.orders = orders;
        this.shipments = shipments;
        this.stock = stock;
    }

    /**
     * Tạo vận đơn cho đơn: phí tính theo cả đơn. Mỗi đơn chỉ có một vận đơn "còn hiệu lực"
     * (khác CANCELLED / RETURNED); DELIVERED cũng chặn tạo mới.
     *
     * @throws IllegalArgumentException khi orderId / address null, hoặc đơn không có hàng vật lý
     * @throws OrderNotFoundException   khi không có đơn với id này
     * @throws ActiveShipmentExistsException khi đơn đã có vận đơn còn hiệu lực
     */
    @Transactional
    public Shipment create(String orderId, Address address) {
        if (orderId == null || orderId.isBlank() || address == null) {
            throw new IllegalArgumentException("orderId, address không được rỗng");
        }
        Order order = loadOrder(orderId);
        boolean hasActive = shipments.findByOrderId(orderId).stream()
                .anyMatch(shipment -> shipment.getStatus().blocksNewShipment());
        if (hasActive) {
            throw new ActiveShipmentExistsException(orderId);
        }
        BigDecimal fee = ShippingFeeCalculator.feeFor(order, address);
        return shipments.save(new Shipment(orderId, address, fee));
    }

    /**
     * @throws ShipmentNotFoundException khi không có vận đơn với id này (kể cả id null)
     */
    @Transactional(readOnly = true)
    public Shipment findById(String id) {
        return shipments.findById(id).orElseThrow(() -> new ShipmentNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Shipment> findByOrderId(String orderId) {
        return shipments.findByOrderId(orderId);
    }

    /**
     * @throws ShipmentNotFoundException khi không có vận đơn
     * @throws IllegalShipmentTransitionException  khi vận đơn không ở CREATED
     */
    @Transactional
    public Shipment pickUp(String id) {
        Shipment shipment = findById(id);
        shipment.pickUp();
        return shipments.save(shipment);
    }

    /**
     * Giao thành công, không hoàn kho.
     *
     * @throws ShipmentNotFoundException khi không có vận đơn
     * @throws IllegalShipmentTransitionException  khi vận đơn không ở IN_TRANSIT
     */
    @Transactional
    public Shipment deliver(String id) {
        Shipment shipment = findById(id);
        shipment.markDelivered();
        return shipments.save(shipment);
    }

    /**
     * @throws ShipmentNotFoundException khi không có vận đơn
     * @throws IllegalArgumentException  khi reason null/rỗng
     * @throws IllegalShipmentTransitionException  khi vận đơn không ở IN_TRANSIT
     */
    @Transactional
    public Shipment failAttempt(String id, String reason) {
        Shipment shipment = findById(id);
        shipment.failAttempt(reason);
        return shipments.save(shipment);
    }

    /**
     * @throws ShipmentNotFoundException khi không có vận đơn
     * @throws IllegalArgumentException  khi reason null/rỗng
     * @throws IllegalShipmentTransitionException  khi vận đơn không ở IN_TRANSIT
     */
    @Transactional
    public Shipment refuse(String id, String reason) {
        Shipment shipment = findById(id);
        shipment.refuse(reason);
        return shipments.save(shipment);
    }

    /**
     * Hàng đã về kho: RETURNING → RETURNED và hoàn kho theo đơn, cùng transaction.
     *
     * @throws ShipmentNotFoundException khi không có vận đơn
     * @throws IllegalShipmentTransitionException  khi vận đơn không ở RETURNING; kho không đổi
     */
    @Transactional
    public Shipment markReturned(String id) {
        Shipment shipment = findById(id);
        shipment.markReturned();
        return restockAndSave(shipment);
    }

    /**
     * Huỷ vận đơn (chỉ ở CREATED) và hoàn kho theo đơn, cùng transaction.
     *
     * @throws ShipmentNotFoundException khi không có vận đơn
     * @throws IllegalShipmentTransitionException  khi vận đơn không ở CREATED; kho không đổi
     */
    @Transactional
    public Shipment cancel(String id) {
        Shipment shipment = findById(id);
        shipment.cancel();
        return restockAndSave(shipment);
    }

    private Shipment restockAndSave(Shipment shipment) {
        stock.increaseStock(loadOrder(shipment.getOrderId()));
        return shipments.save(shipment);
    }

    private Order loadOrder(String orderId) {
        return orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
