package com.training.ecommerce.visualizer.ecommerce.trace;

import com.training.ecommerce.visualizer.core.scenario.RunResult;
import com.training.ecommerce.visualizer.core.trace.FlowKind;
import com.training.ecommerce.visualizer.core.trace.Outcome;
import com.training.ecommerce.visualizer.core.trace.TraceEvent;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** File fixture commit phải khớp kết quả sinh lại, nếu không thì chạy lại {@link SampleEventsFixture}. */
class SampleEventsFixtureTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static JsonNode withoutVolatile(JsonNode root) {
        JsonNode copy = root.deepCopy();
        for (JsonNode e : copy.get("events")) {
            ((ObjectNode) e).remove("at");
            ((ObjectNode) e).remove("thread");
        }
        return copy;
    }

    @Test
    void fileCommitKhopVoiKetQuaSinhLai() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/static/fixtures/sample-events.json")) {
            assertThat(in).as("thiếu fixtures/sample-events.json").isNotNull();
            JsonNode committed = MAPPER.readTree(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            JsonNode fresh = MAPPER.readTree(SampleEventsFixture.toJson());
            assertThat(withoutVolatile(committed)).isEqualTo(withoutVolatile(fresh));
        }
    }

    @Test
    void kichBanMauCoDuBaTinhHuong() {
        RunResult run = SampleEventsFixture.generate();

        assertThat(run.events()).hasSize(3);
        TraceEvent call = run.events().get(0);
        assertThat(call.events()).hasSize(2);
        assertThat(call.flows()).extracting(f -> f.kind()).containsExactly(FlowKind.CALL, FlowKind.EVENT);
        assertThat(call.events().get(1).from()).isNull();

        TraceEvent reaction = run.events().get(1);
        assertThat(reaction.causedBy()).isEqualTo(call.events().get(0).id());

        TraceEvent thrown = run.events().get(2);
        assertThat(thrown.outcome()).isEqualTo(Outcome.THROWN);
        assertThat(thrown.events()).extracting(e -> e.type()).containsExactly("OrderPlaced");
    }
}
