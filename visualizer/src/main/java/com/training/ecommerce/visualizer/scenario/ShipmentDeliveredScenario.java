package com.training.ecommerce.visualizer.scenario;

import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.shipping.Shipment;
import com.training.ecommerce.visualizer.trace.TraceRecorder;
import com.training.ecommerce.visualizer.world.World;

import java.math.BigDecimal;
import java.util.List;

/** S07: vận đơn đi đường thuận CREATED → IN_TRANSIT → DELIVERED, kho không hoàn. */
public class ShipmentDeliveredScenario implements Scenario {

    @Override
    public String id() {
        return "shipment-delivered";
    }

    @Override
    public String title() {
        return "Vận đơn: giao thành công";
    }

    @Override
    public String session() {
        return "S07";
    }

    @Override
    public String description() {
        return "Đặt đơn LAPTOP ×1 + PHONE ×1 (trừ kho hub), ShippingFeeCalculator.feeFor tính phí cả đơn một lần "
                + "(20k + 5k × 2.4 kg = 32k, giao Hà Nội), tạo Shipment rồi pickUp → markDelivered. "
                + "DELIVERED là trạng thái kết thúc, hàng đã tới tay khách nên kho không hoàn.";
    }

    @Override
    public void run(World world, TraceRecorder recorder) {
        Order order = ShipmentSteps.placeOrder(world, recorder);
        Address address = new Address("1 Hàng Bài", "Hà Nội");
        BigDecimal fee = ShipmentSteps.fee(recorder, order, address);
        Shipment shipment = ShipmentSteps.create(world, recorder, order, address, fee);
        ShipmentSteps.step(recorder, shipment, "pickUp", List.of(), shipment::pickUp);
        ShipmentSteps.step(recorder, shipment, "markDelivered", List.of(), shipment::markDelivered);
    }
}
