package com.training.ecommerce.visualizer.scenario;

import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.visualizer.trace.Flow;
import com.training.ecommerce.visualizer.trace.TraceRecorder;
import com.training.ecommerce.visualizer.world.World;
import com.training.ecommerce.visualizer.world.WorldSnapshotter;

import java.util.List;

/** S04: đơn có 2 dòng cùng một sản phẩm, tồn kho bị trừ cộng dồn đúng một lần. */
public class DecreaseStockOkScenario implements Scenario {

    @Override
    public String id() {
        return "decrease-stock-ok";
    }

    @Override
    public String title() {
        return "Trừ tồn kho thành công";
    }

    @Override
    public String session() {
        return "S04";
    }

    @Override
    public String description() {
        return "Đơn có 2 dòng LAPTOP (1 + 2): InventoryManager.decreaseStock cộng dồn theo product rồi trừ đúng một lần (5 → 2).";
    }

    @Override
    public void run(World world, TraceRecorder recorder) {
        Product laptop = world.product("LAPTOP");
        Order order = OrderSteps.create(world, recorder);
        OrderSteps.addLine(recorder, order, laptop, 1);
        OrderSteps.addLine(recorder, order, laptop, 2);
        recorder.run("InventoryManager", "decreaseStock", List.of(WorldSnapshotter.orderRef(order)),
                new Flow(WorldSnapshotter.ORDER, WorldSnapshotter.HUB), List.of(WorldSnapshotter.HUB),
                () -> world.hub().decreaseStock(order));
    }
}
