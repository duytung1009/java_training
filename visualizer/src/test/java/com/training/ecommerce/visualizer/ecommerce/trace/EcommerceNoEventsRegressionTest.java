package com.training.ecommerce.visualizer.ecommerce.trace;

import com.training.ecommerce.visualizer.core.scenario.RunResult;
import com.training.ecommerce.visualizer.core.scenario.ScenarioService;
import com.training.ecommerce.visualizer.core.trace.FlowKind;
import com.training.ecommerce.visualizer.ecommerce.EcommerceVisualizerAdapter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Hồi quy: adapter ecommerce không có domain event nên JSON kịch bản chỉ thêm trường mặc định. */
class EcommerceNoEventsRegressionTest {

    @Test
    void moiKichBanKhongCoEventVaMoiFlowLaCallKhongNhan() {
        ScenarioService service = new ScenarioService(new EcommerceVisualizerAdapter());

        assertThat(service.scenarios()).isNotEmpty();
        service.scenarios().forEach(s -> {
            RunResult run = service.run(s.id());
            assertThat(run.events()).as(s.id()).isNotEmpty().allSatisfy(e -> {
                assertThat(e.events()).isEmpty();
                assertThat(e.causedBy()).isNull();
                assertThat(e.flows()).allSatisfy(f -> {
                    assertThat(f.kind()).isEqualTo(FlowKind.CALL);
                    assertThat(f.label()).isNull();
                });
            });
        });
    }
}
