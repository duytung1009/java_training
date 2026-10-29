package com.training.ecommerce.visualizer.scenario;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** Khai báo thứ tự kịch bản hiển thị trên UI. */
@Configuration
public class ScenarioConfig {

    @Bean
    public ScenarioRegistry scenarioRegistry() {
        return new ScenarioRegistry(List.of(
                new OverviewScenario(),
                new CartToOrderScenario(),
                new ShippingFeeScenario(),
                new PricingTypesScenario(),
                new DecreaseStockOkScenario(),
                new DecreaseStockFailScenario(),
                new TransferScenario(),
                new TransferDeadlockSafeScenario(),
                new LastItemRaceScenario(),
                new ReplenishScenario()
        ));
    }
}
