package com.training.ecommerce.visualizer.scenario;

import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.catalog.ProductNotFoundException;
import com.training.ecommerce.visualizer.trace.Flow;
import com.training.ecommerce.visualizer.trace.TraceRecorder;
import com.training.ecommerce.visualizer.world.World;
import com.training.ecommerce.visualizer.world.WorldFactory;
import com.training.ecommerce.visualizer.world.WorldSnapshotter;

import java.util.List;

/** S07: nhập hàng từ kho (dự trữ) lên hub (sẵn bán), gồm hai ca thất bại. */
public class ReplenishScenario implements Scenario {

    @Override
    public String id() {
        return "replenish";
    }

    @Override
    public String title() {
        return "Nhập hàng từ kho lên hub";
    }

    @Override
    public String session() {
        return "S07";
    }

    @Override
    public String description() {
        return "replenish LAPTOP ×3 và PHONE ×2 từ kho lên hub; HEADPHONE hub không bán (ProductNotFoundException); "
                + "BOOK không kho nào có (OutOfStockException). Ca lỗi không đổi gì.";
    }

    @Override
    public void run(World world, TraceRecorder recorder) {
        replenish(world, recorder, WorldFactory.WH_HN, "LAPTOP", 3);
        replenish(world, recorder, WorldFactory.WH_HCM, "PHONE", 2);
        try {
            replenish(world, recorder, WorldFactory.WH_HN, "HEADPHONE", 1);
        } catch (ProductNotFoundException expected) {
            // đã ghi thành event THROWN; kho giữ nguyên
        }
        try {
            replenish(world, recorder, WorldFactory.WH_HN, "BOOK", 1);
        } catch (OutOfStockException expected) {
            // đã ghi thành event THROWN; kho và hub giữ nguyên
        }
    }

    /** Gọi replenish qua recorder; dùng chung với kịch bản tổng quan. */
    static void replenish(World world, TraceRecorder recorder, String warehouseId, String productId,
                                  int quantity) {
        String from = WorldSnapshotter.warehousePanelId(warehouseId);
        recorder.run("InventoryManager", "replenish",
                List.of(warehouseId, productId, String.valueOf(quantity)), new Flow(from, WorldSnapshotter.HUB),
                List.of(from, WorldSnapshotter.HUB),
                () -> world.hub().replenish(world.warehouse(warehouseId), productId, quantity));
    }
}
