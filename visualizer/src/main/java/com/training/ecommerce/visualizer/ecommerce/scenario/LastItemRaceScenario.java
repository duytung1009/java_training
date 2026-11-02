package com.training.ecommerce.visualizer.ecommerce.scenario;

import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;
import com.training.ecommerce.visualizer.core.scenario.ParallelRunner;
import com.training.ecommerce.visualizer.core.scenario.Scenario;
import com.training.ecommerce.visualizer.core.trace.Flow;
import com.training.ecommerce.visualizer.core.trace.TraceRecorder;
import com.training.ecommerce.visualizer.ecommerce.world.World;
import com.training.ecommerce.visualizer.ecommerce.world.WorldSnapshotter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** S06: nhiều thread cùng mua sản phẩm chỉ còn 1 cái; đúng một thread thành công, stock không âm. */
public class LastItemRaceScenario implements Scenario<World> {

    /** Id sản phẩm chỉ còn 1 cái trong hub. */
    public static final String PRODUCT_ID = "GIFT";
    static final int THREADS = 6;

    @Override
    public String id() {
        return "last-item-race";
    }

    @Override
    public String title() {
        return "Tranh nhau món hàng cuối";
    }

    @Override
    public String session() {
        return "S06";
    }

    @Override
    public String description() {
        return THREADS + " thread cùng decreaseStock sản phẩm GIFT chỉ còn 1 cái: đúng một OK, các thread còn lại OutOfStockException, stock không âm.";
    }

    @Override
    public boolean concurrent() {
        return true;
    }

    @Override
    public void run(World world, TraceRecorder recorder) {
        PhysicalProduct gift = new PhysicalProduct(PRODUCT_ID, "Quà cuối", new BigDecimal("100000"), 1,
                new BigDecimal("0.1"));
        // Ghi thành event để ô GIFT (hub) và khối GIFT (catalog) pop-in với stock 1 trước khi các thread tranh nhau
        recorder.run("InventoryManager", "addProduct", List.of(PRODUCT_ID + ", stock 1"), null,
                List.of(WorldSnapshotter.HUB, WorldSnapshotter.CATALOG, WorldSnapshotter.productNodeId(PRODUCT_ID)),
                () -> world.registerProduct(gift));

        // Dựng đơn trước khi các thread chạy: OrderLine kiểm tồn kho lúc tạo, lúc này còn đúng 1 cái.
        // Ghi thành một event và đưa vào world để các đơn hiện trong List<Order> trước khi tranh nhau
        List<Order> orders = new ArrayList<>();
        recorder.run("Order", "new", List.of("×" + THREADS, "addLine(" + PRODUCT_ID + ", 1)"), null,
                List.of(WorldSnapshotter.ORDERS), () -> {
                    for (int i = 0; i < THREADS; i++) {
                        Order order = new Order(BigDecimal.ZERO, BigDecimal.ZERO);
                        order.addLine(new OrderLine(gift, 1));
                        orders.add(order);
                        world.addOrder(order);
                    }
                });
        List<Runnable> tasks = orders.stream().map(order -> (Runnable) () -> {
            String orderNode = WorldSnapshotter.orderNodeId(order);
            try {
                recorder.run("InventoryManager", "decreaseStock", List.of(WorldSnapshotter.orderRef(order)),
                        new Flow(orderNode, WorldSnapshotter.HUB), List.of(orderNode, WorldSnapshotter.HUB),
                        () -> world.hub().decreaseStock(order));
            } catch (OutOfStockException expected) {
                // thread thua cuộc: đã ghi thành THROWN
            }
        }).toList();
        ParallelRunner.runTogether(tasks);
    }
}
