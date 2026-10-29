package com.training.ecommerce.visualizer.scenario;

import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.inventory.WarehouseTransferService;
import com.training.ecommerce.visualizer.trace.Flow;
import com.training.ecommerce.visualizer.trace.TraceRecorder;
import com.training.ecommerce.visualizer.world.World;
import com.training.ecommerce.visualizer.world.WorldFactory;
import com.training.ecommerce.visualizer.world.WorldSnapshotter;

import java.util.List;

/** S06: chuyển hàng giữa hai kho, gồm ca sang kho chưa có loại hàng và ca thất bại. */
public class TransferScenario implements Scenario {

    @Override
    public String id() {
        return "transfer";
    }

    @Override
    public String title() {
        return "Chuyển hàng giữa hai kho";
    }

    @Override
    public String session() {
        return "S06";
    }

    @Override
    public String description() {
        return "transfer LAPTOP thành công; HEADPHONE sang kho chưa có loại này (pop-in); PHONE ×9 thất bại vì thiếu hàng, không kho nào đổi.";
    }

    @Override
    public void run(World world, TraceRecorder recorder) {
        WarehouseTransferService service = new WarehouseTransferService();
        transfer(world, recorder, service, WorldFactory.WH_HN, WorldFactory.WH_HCM, "LAPTOP", 3);
        transfer(world, recorder, service, WorldFactory.WH_HN, WorldFactory.WH_HCM, "HEADPHONE", 2);
        try {
            transfer(world, recorder, service, WorldFactory.WH_HCM, WorldFactory.WH_HN, "PHONE", 9);
        } catch (OutOfStockException expected) {
            // đã ghi thành event THROWN; hai kho giữ nguyên
        }
    }

    /** Gọi transfer qua recorder; dùng chung với kịch bản đa luồng. */
    static void transfer(World world, TraceRecorder recorder, WarehouseTransferService service,
                         String fromId, String toId, String productId, int quantity) {
        String from = WorldSnapshotter.warehousePanelId(fromId);
        String to = WorldSnapshotter.warehousePanelId(toId);
        recorder.run("WarehouseTransferService", "transfer",
                List.of(fromId, toId, productId, String.valueOf(quantity)), new Flow(from, to), List.of(from, to),
                () -> service.transfer(world.warehouse(fromId), world.warehouse(toId), productId, quantity));
    }
}
