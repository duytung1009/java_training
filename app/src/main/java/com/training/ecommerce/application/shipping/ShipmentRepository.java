package com.training.ecommerce.application.shipping;

import com.training.ecommerce.domain.shipping.Shipment;
import com.training.ecommerce.shared.repository.Repository;

import java.util.List;

/**
 * Port lưu vận đơn: {@link Repository} generic kèm tra cứu theo đơn.
 */
public interface ShipmentRepository extends Repository<Shipment, String> {

    /**
     * @return mọi vận đơn của đơn này theo thứ tự tạo (kể cả đã huỷ / hoàn); list không sửa được.
     *         orderId null → list rỗng
     */
    List<Shipment> findByOrderId(String orderId);
}
