package com.training.ecommerce.visualizer.scenario;

import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;
import com.training.ecommerce.domain.pricing.DiscountCode;
import com.training.ecommerce.visualizer.trace.Flow;
import com.training.ecommerce.visualizer.trace.TraceRecorder;
import com.training.ecommerce.visualizer.world.World;
import com.training.ecommerce.visualizer.world.WorldSnapshotter;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/** Bước dùng chung cho các kịch bản tự dựng Order (không đi qua Cart). */
final class OrderSteps {

    private OrderSteps() {
    }

    /** Tạo Order rỗng (không giảm giá, không thuế) và đưa vào world: event "pop-in" của order. */
    static Order create(World world, TraceRecorder recorder) {
        return recorder.call("Order", "new", List.of("0", "0"), null, List.of(WorldSnapshotter.ORDERS), () -> {
            Order order = new Order(BigDecimal.ZERO, BigDecimal.ZERO);
            world.addOrder(order);
            return order;
        });
    }

    /** Thêm dòng qua constructor kiểm tồn kho của OrderLine. */
    static void addLine(TraceRecorder recorder, Order order, Product product, int quantity) {
        recorder.run("Order", "addLine", List.of(product.getId(), String.valueOf(quantity)), null,
                orderPanels(order), () -> order.addLine(new OrderLine(product, quantity)));
    }

    /** Thêm dòng bằng giá snapshot sẵn, bỏ qua kiểm tồn kho lúc tạo dòng để mô phỏng đơn đặt quá tay. */
    static void addLineUnchecked(TraceRecorder recorder, Order order, Product product, int quantity) {
        recorder.run("Order", "addLine", List.of(product.getId(), String.valueOf(quantity)), null,
                orderPanels(order),
                () -> order.addLine(new OrderLine(product, quantity, product.calculateFinalPrice())));
    }

    /** Chốt giỏ hàng của world thành Order (event pop-in của đơn); Cart.toOrder không xoá giỏ. */
    static Order checkout(World world, TraceRecorder recorder, BigDecimal discount, BigDecimal tax) {
        AtomicReference<Order> created = new AtomicReference<>();
        // toOrder không dùng domain.pricing: đơn giá lấy từ Product.calculateFinalPrice trong OrderLine.
        // Danh sách panel được ghi nhận sau action nên thêm node con của đơn vào ngay khi đơn có id.
        List<String> toOrderPanels = new CopyOnWriteArrayList<>(List.of(WorldSnapshotter.CART, WorldSnapshotter.ORDERS));
        recorder.callFlows("Cart", "toOrder", List.of(discount.toPlainString(), tax.toPlainString()),
                List.of(new Flow(WorldSnapshotter.CART, WorldSnapshotter.ORDERS)), toOrderPanels, () -> {
                    Order order = world.cart().toOrder(discount, tax);
                    world.addOrder(order);
                    created.set(order);
                    toOrderPanels.add(WorldSnapshotter.orderNodeId(order));
                    return WorldSnapshotter.orderRef(order);
                });
        return created.get();
    }

    /**
     * Chốt giỏ bằng mã giảm giá ({@code Cart.toOrder(code, taxRate)}): ngoài flow giỏ → đơn còn flow mã → đơn,
     * mã nháy cùng giỏ và đơn.
     */
    static Order checkout(World world, TraceRecorder recorder, DiscountCode code, BigDecimal tax) {
        AtomicReference<Order> created = new AtomicReference<>();
        String codeNode = PricingTypesScenario.codeNodeId(code.getCode());
        List<String> toOrderPanels = new CopyOnWriteArrayList<>(List.of(WorldSnapshotter.CART, WorldSnapshotter.ORDERS,
                WorldSnapshotter.PRICING, codeNode));
        recorder.callFlows("Cart", "toOrder", List.of(code.getCode(), tax.toPlainString()),
                List.of(new Flow(WorldSnapshotter.CART, WorldSnapshotter.ORDERS), new Flow(codeNode, WorldSnapshotter.ORDERS)),
                toOrderPanels, () -> {
                    Order order = world.cart().toOrder(code, tax);
                    world.addOrder(order);
                    created.set(order);
                    toOrderPanels.add(WorldSnapshotter.orderNodeId(order));
                    return WorldSnapshotter.orderRef(order);
                });
        return created.get();
    }

    /** Trừ kho cho cả đơn; flow từ khối Order về hub. */
    static void decreaseStock(World world, TraceRecorder recorder, Order order) {
        recorder.run("InventoryManager", "decreaseStock", List.of(WorldSnapshotter.orderRef(order)),
                new Flow(WorldSnapshotter.orderNodeId(order), WorldSnapshotter.HUB), List.of(WorldSnapshotter.HUB),
                () -> world.hub().decreaseStock(order));
    }

    /** Panel nháy khi đơn đổi: collection orders và node con của chính đơn. */
    private static List<String> orderPanels(Order order) {
        return List.of(WorldSnapshotter.ORDERS, WorldSnapshotter.orderNodeId(order));
    }
}
