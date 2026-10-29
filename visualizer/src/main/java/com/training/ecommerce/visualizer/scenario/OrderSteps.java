package com.training.ecommerce.visualizer.scenario;

import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;
import com.training.ecommerce.visualizer.trace.TraceRecorder;
import com.training.ecommerce.visualizer.world.World;
import com.training.ecommerce.visualizer.world.WorldSnapshotter;

import java.math.BigDecimal;
import java.util.List;

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

    /** Panel nháy khi đơn đổi: collection orders và node con của chính đơn. */
    private static List<String> orderPanels(Order order) {
        return List.of(WorldSnapshotter.ORDERS, WorldSnapshotter.orderNodeId(order));
    }
}
