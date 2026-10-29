package com.training.ecommerce.infrastructure.persistence.inmemory;

import com.training.ecommerce.application.shipping.ShipmentRepository;
import com.training.ecommerce.domain.shipping.Shipment;

import java.util.List;

/**
 * {@link ShipmentRepository} in-memory: kế thừa {@link InMemoryRepository}, thêm tra cứu theo đơn.
 * Lưu chính object truyền vào (như Order); cùng giới hạn không thread-safe.
 */
public class InMemoryShipmentRepository extends InMemoryRepository<Shipment, String> implements ShipmentRepository {

    @Override
    public List<Shipment> findByOrderId(String orderId) {
        return findAll().stream().filter(shipment -> shipment.getOrderId().equals(orderId)).toList();
    }
}
