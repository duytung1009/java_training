package com.training.ecommerce.visualizer.ecommerce.trace;

import com.training.ecommerce.visualizer.core.scenario.RunResult;
import com.training.ecommerce.visualizer.core.scenario.ScenarioService;
import com.training.ecommerce.visualizer.core.trace.FlowKind;
import com.training.ecommerce.visualizer.ecommerce.EcommerceVisualizerAdapter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hồi quy: adapter ecommerce không có domain event nên mọi flow là CALL. Nhãn của flow CALL (nếu có) là tên dữ liệu chảy
 * theo lời gọi (vd. {@code orderId}), dùng khi hai package không gọi nhau trực tiếp.
 */
class EcommerceNoEventsRegressionTest {

    @Test
    void moiKichBanKhongCoEventVaMoiFlowLaCall() {
        ScenarioService service = new ScenarioService(new EcommerceVisualizerAdapter());

        assertThat(service.scenarios()).isNotEmpty();
        service.scenarios().forEach(s -> {
            RunResult run = service.run(s.id());
            assertThat(run.events()).as(s.id()).isNotEmpty().allSatisfy(e -> {
                assertThat(e.events()).isEmpty();
                assertThat(e.causedBy()).isNull();
                assertThat(e.flows()).allSatisfy(f -> {
                    assertThat(f.kind()).isEqualTo(FlowKind.CALL);
                    if (f.label() != null) {
                        assertThat(f.label()).isNotBlank();
                    }
                });
            });
        });
    }
}
