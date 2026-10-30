package com.training.ecommerce.visualizer.ecommerce;

import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.visualizer.core.scenario.Scenario;
import com.training.ecommerce.visualizer.core.spi.VisualizerAdapter;
import com.training.ecommerce.visualizer.core.sync.DomainApiSpec;
import com.training.ecommerce.visualizer.core.view.WorldSnapshot;
import com.training.ecommerce.visualizer.ecommerce.scenario.CartToOrderScenario;
import com.training.ecommerce.visualizer.ecommerce.scenario.DecreaseStockFailScenario;
import com.training.ecommerce.visualizer.ecommerce.scenario.DecreaseStockOkScenario;
import com.training.ecommerce.visualizer.ecommerce.scenario.LastItemRaceScenario;
import com.training.ecommerce.visualizer.ecommerce.scenario.OverviewScenario;
import com.training.ecommerce.visualizer.ecommerce.scenario.PricingTypesScenario;
import com.training.ecommerce.visualizer.ecommerce.scenario.ReplenishScenario;
import com.training.ecommerce.visualizer.ecommerce.scenario.ShipmentDeliveredScenario;
import com.training.ecommerce.visualizer.ecommerce.scenario.ShipmentReturnedScenario;
import com.training.ecommerce.visualizer.ecommerce.scenario.ShippingFeeScenario;
import com.training.ecommerce.visualizer.ecommerce.scenario.TransferDeadlockSafeScenario;
import com.training.ecommerce.visualizer.ecommerce.scenario.TransferScenario;
import com.training.ecommerce.visualizer.ecommerce.world.World;
import com.training.ecommerce.visualizer.ecommerce.world.WorldFactory;
import com.training.ecommerce.visualizer.ecommerce.world.WorldSnapshotter;
import org.springframework.stereotype.Component;

import java.util.List;

/** Adapter ecommerce: thế giới mẫu, snapshot, thứ tự kịch bản hiển thị trên UI và nơi quét API domain. */
@Component
public class EcommerceVisualizerAdapter implements VisualizerAdapter<World> {

    /** Package gốc của module domain ecommerce. */
    public static final String DOMAIN_PACKAGE = "com.training.ecommerce.domain";
    /** File snapshot API domain trên classpath. */
    public static final String SNAPSHOT_RESOURCE = "/domain-api.txt";

    private final WorldFactory worldFactory = new WorldFactory();
    private final WorldSnapshotter snapshotter = new WorldSnapshotter();

    @Override
    public World createWorld() {
        return worldFactory.create();
    }

    @Override
    public WorldSnapshot snapshot(World world) {
        return snapshotter.snapshot(world);
    }

    @Override
    public List<Scenario<World>> scenarios() {
        return List.of(
                new OverviewScenario(),
                new CartToOrderScenario(),
                new ShippingFeeScenario(),
                new PricingTypesScenario(),
                new DecreaseStockOkScenario(),
                new DecreaseStockFailScenario(),
                new TransferScenario(),
                new TransferDeadlockSafeScenario(),
                new LastItemRaceScenario(),
                new ReplenishScenario(),
                new ShipmentDeliveredScenario(),
                new ShipmentReturnedScenario()
        );
    }

    @Override
    public DomainApiSpec domainApi() {
        return new DomainApiSpec(DOMAIN_PACKAGE, Product.class, SNAPSHOT_RESOURCE);
    }
}
