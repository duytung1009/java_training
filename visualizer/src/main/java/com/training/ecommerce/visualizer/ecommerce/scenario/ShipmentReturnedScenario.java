package com.training.ecommerce.visualizer.ecommerce.scenario;

import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.shipping.IllegalShipmentTransitionException;
import com.training.ecommerce.domain.shipping.Shipment;
import com.training.ecommerce.visualizer.core.scenario.Scenario;
import com.training.ecommerce.visualizer.core.trace.TraceRecorder;
import com.training.ecommerce.visualizer.ecommerce.world.World;

import java.math.BigDecimal;
import java.util.List;

/** S07: giao thất bại 3 lần → RETURNING → RETURNED, rồi hoàn kho cho đơn. */
public class ShipmentReturnedScenario implements Scenario<World> {

    @Override
    public String id() {
        return "shipment-returned";
    }

    @Override
    public String title() {
        return "Vận đơn: giao thất bại, hoàn hàng";
    }

    @Override
    public String session() {
        return "S07";
    }

    @Override
    public String description() {
        return "Đặt đơn LAPTOP ×1 + PHONE ×1 giao Đà Nẵng, pickUp, cancel() khi đang IN_TRANSIT bị từ chối "
                + "(IllegalShipmentTransitionException, trạng thái giữ nguyên), rồi failAttempt ×3 (lần thứ 3 tự sang RETURNING). "
                + "markReturned rồi InventoryManager.restock(order): tồn kho hub quay về như trước khi đặt.";
    }

    @Override
    public void run(World world, TraceRecorder recorder) {
        Order order = ShipmentSteps.placeOrder(world, recorder);
        Address address = new Address("5 Lê Lợi", "Đà Nẵng");
        BigDecimal fee = ShipmentSteps.fee(recorder, order, address);
        Shipment shipment = ShipmentSteps.create(world, recorder, order, address, fee);
        ShipmentSteps.step(recorder, shipment, "pickUp", List.of(), shipment::pickUp);
        try {
            ShipmentSteps.step(recorder, shipment, "cancel", List.of(), shipment::cancel);
        } catch (IllegalShipmentTransitionException expected) {
            // đã được recorder ghi thành event THROWN: huỷ chỉ hợp lệ khi còn CREATED, vận đơn giữ nguyên IN_TRANSIT
        }
        for (String reason : List.of("Không có người nhận", "Khách hẹn lại", "Sai địa chỉ")) {
            ShipmentSteps.step(recorder, shipment, "failAttempt", List.of(reason), () -> shipment.failAttempt(reason));
        }
        ShipmentSteps.step(recorder, shipment, "markReturned", List.of(), shipment::markReturned);
        ShipmentSteps.restock(world, recorder, order, shipment);
    }
}
