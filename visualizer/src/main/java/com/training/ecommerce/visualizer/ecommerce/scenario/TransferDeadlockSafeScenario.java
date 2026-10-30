package com.training.ecommerce.visualizer.ecommerce.scenario;

import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.inventory.WarehouseTransferService;
import com.training.ecommerce.visualizer.core.scenario.ParallelRunner;
import com.training.ecommerce.visualizer.core.scenario.Scenario;
import com.training.ecommerce.visualizer.core.trace.TraceRecorder;
import com.training.ecommerce.visualizer.ecommerce.world.World;
import com.training.ecommerce.visualizer.ecommerce.world.WorldFactory;

import java.util.List;

/** S06: hai thread chuyển hàng ngược chiều đồng thời; khoá theo thứ tự id nên không deadlock. */
public class TransferDeadlockSafeScenario implements Scenario<World> {

    private static final int ROUNDS = 4;

    @Override
    public String id() {
        return "transfer-deadlock-safe";
    }

    @Override
    public String title() {
        return "Chuyển hàng hai chiều đồng thời (không deadlock)";
    }

    @Override
    public String session() {
        return "S06";
    }

    @Override
    public String description() {
        return "Hai thread chuyển PHONE HN→HCM và HCM→HN song song, mỗi bên " + ROUNDS
                + " lần: log xen kẽ hai thread, không treo, tổng hàng giữ nguyên.";
    }

    @Override
    public boolean concurrent() {
        return true;
    }

    @Override
    public void run(World world, TraceRecorder recorder) {
        WarehouseTransferService service = new WarehouseTransferService();
        ParallelRunner.runTogether(List.of(
                repeat(world, recorder, service, WorldFactory.WH_HN, WorldFactory.WH_HCM),
                repeat(world, recorder, service, WorldFactory.WH_HCM, WorldFactory.WH_HN)));
    }

    private static Runnable repeat(World world, TraceRecorder recorder, WarehouseTransferService service,
                                   String from, String to) {
        return () -> {
            for (int i = 0; i < ROUNDS; i++) {
                try {
                    TransferScenario.transfer(world, recorder, service, from, to, "PHONE", 1);
                } catch (OutOfStockException expected) {
                    // kho nguồn tạm hết hàng: đã ghi thành THROWN, thử tiếp vòng sau
                }
            }
        };
    }
}
