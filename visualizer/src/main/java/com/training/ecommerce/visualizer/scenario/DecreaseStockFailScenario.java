package com.training.ecommerce.visualizer.scenario;

import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.visualizer.trace.Flow;
import com.training.ecommerce.visualizer.trace.TraceRecorder;
import com.training.ecommerce.visualizer.world.World;
import com.training.ecommerce.visualizer.world.WorldSnapshotter;

import java.util.List;

/** S04: một dòng thiếu hàng làm cả đơn thất bại, không dòng nào bị trừ (all-or-nothing). */
public class DecreaseStockFailScenario implements Scenario {

    @Override
    public String id() {
        return "decrease-stock-fail";
    }

    @Override
    public String title() {
        return "Trừ tồn kho thất bại (all-or-nothing)";
    }

    @Override
    public String session() {
        return "S04";
    }

    @Override
    public String description() {
        return "Đơn có LAPTOP ×1 (đủ hàng) và PHONE ×99 (thiếu): decreaseStock ném OutOfStockException và tồn kho không đổi.";
    }

    @Override
    public void run(World world, TraceRecorder recorder) {
        Product laptop = world.product("LAPTOP");
        Product phone = world.product("PHONE");
        Order order = OrderSteps.create(world, recorder);
        OrderSteps.addLine(recorder, order, laptop, 1);
        // 3-arg OrderLine không kiểm tồn kho, nên mới dựng được đơn đặt quá số còn lại
        OrderSteps.addLineUnchecked(recorder, order, phone, 99);
        try {
            recorder.run("InventoryManager", "decreaseStock", List.of(WorldSnapshotter.orderRef(order)),
                    new Flow(WorldSnapshotter.ORDER, WorldSnapshotter.HUB), List.of(WorldSnapshotter.HUB),
                    () -> world.hub().decreaseStock(order));
        } catch (OutOfStockException expected) {
            // đã được recorder ghi lại thành event THROWN; đây là điều kịch bản muốn cho thấy
        }
    }
}
