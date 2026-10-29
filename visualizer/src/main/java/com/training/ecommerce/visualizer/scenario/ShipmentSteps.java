package com.training.ecommerce.visualizer.scenario;

import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.shipping.Shipment;
import com.training.ecommerce.domain.shipping.ShippingFeeCalculator;
import com.training.ecommerce.visualizer.trace.Flow;
import com.training.ecommerce.visualizer.trace.TraceRecorder;
import com.training.ecommerce.visualizer.world.MoneyFormat;
import com.training.ecommerce.visualizer.world.World;
import com.training.ecommerce.visualizer.world.WorldSnapshotter;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/** Bước dùng chung cho các kịch bản vận đơn (đơn đã có và đã trừ kho từ {@link OrderSteps}). */
final class ShipmentSteps {

    private ShipmentSteps() {
    }

    /** Đơn mẫu: LAPTOP ×1 + PHONE ×1 (2.4 kg), đã trừ kho hub. */
    static Order placeOrder(World world, TraceRecorder recorder) {
        Order order = OrderSteps.create(world, recorder);
        OrderSteps.addLine(recorder, order, world.product("LAPTOP"), 1);
        OrderSteps.addLine(recorder, order, world.product("PHONE"), 1);
        OrderSteps.decreaseStock(world, recorder, order);
        return order;
    }

    /** Phí ship cả đơn (hàm thuần, chỉ đọc đơn): flow từ khối Order sang vùng shipping. */
    static BigDecimal fee(TraceRecorder recorder, Order order, Address address) {
        return recorder.call("ShippingFeeCalculator", "feeFor",
                List.of(WorldSnapshotter.orderRef(order), WorldSnapshotter.addressRef(address)),
                new Flow(WorldSnapshotter.orderNodeId(order), WorldSnapshotter.SHIPPING),
                List.of(WorldSnapshotter.SHIPPING), () -> ShippingFeeCalculator.feeFor(order, address));
    }

    /** Tạo vận đơn và hiện thành khối con của domain.shipping. */
    static Shipment create(World world, TraceRecorder recorder, Order order, Address address, BigDecimal fee) {
        AtomicReference<Shipment> created = new AtomicReference<>();
        List<String> panels = new CopyOnWriteArrayList<>(List.of(WorldSnapshotter.SHIPPING));
        recorder.call("Shipment", "new",
                List.of(WorldSnapshotter.orderRef(order), WorldSnapshotter.addressRef(address), MoneyFormat.full(fee)),
                new Flow(WorldSnapshotter.orderNodeId(order), WorldSnapshotter.SHIPPING), panels, () -> {
                    Shipment shipment = new Shipment(order.getId(), address, fee);
                    world.registerObject(WorldSnapshotter.SHIPPING, WorldSnapshotter.shipmentNodeId(shipment), shipment,
                            WorldSnapshotter.orderRef(order));
                    created.set(shipment);
                    panels.add(WorldSnapshotter.shipmentNodeId(shipment));
                    return shipment.getTrackingCode() + " · " + shipment.getStatus();
                });
        return created.get();
    }

    /** Một thao tác chuyển trạng thái của vận đơn; ném lại exception của domain (recorder đã ghi THROWN). */
    static void step(TraceRecorder recorder, Shipment shipment, String method, List<String> args, Runnable action) {
        recorder.run("Shipment", method, args, null,
                List.of(WorldSnapshotter.SHIPPING, WorldSnapshotter.shipmentNodeId(shipment)), action);
    }

    /** Hoàn kho cho cả đơn; flow từ vận đơn về hub. */
    static void restock(World world, TraceRecorder recorder, Order order, Shipment shipment) {
        recorder.run("InventoryManager", "restock", List.of(WorldSnapshotter.orderRef(order)),
                new Flow(WorldSnapshotter.shipmentNodeId(shipment), WorldSnapshotter.HUB), List.of(WorldSnapshotter.HUB),
                () -> world.hub().restock(order));
    }
}
