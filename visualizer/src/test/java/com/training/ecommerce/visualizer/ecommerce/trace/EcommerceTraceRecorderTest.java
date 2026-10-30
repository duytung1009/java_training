package com.training.ecommerce.visualizer.ecommerce.trace;

import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.visualizer.core.trace.ObjectChange;
import com.training.ecommerce.visualizer.core.trace.Outcome;
import com.training.ecommerce.visualizer.core.trace.TraceEvent;
import com.training.ecommerce.visualizer.core.trace.TraceRecorder;
import com.training.ecommerce.visualizer.ecommerce.world.World;
import com.training.ecommerce.visualizer.ecommerce.world.WorldFactory;
import com.training.ecommerce.visualizer.ecommerce.world.WorldSnapshotter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** {@code TraceRecorder} ghép với World và domain ecommerce thật (phần lõi test bằng domain giả ở core). */
class EcommerceTraceRecorderTest {

    private World world;
    private TraceRecorder recorder;

    @BeforeEach
    void setUp() {
        world = new WorldFactory().create();
        WorldSnapshotter snapshotter = new WorldSnapshotter();
        recorder = new TraceRecorder(() -> snapshotter.snapshot(world));
    }

    @Test
    void okEventCoChangesChiGomFieldDoi() {
        recorder.run("Warehouse", "remove", List.of("LAPTOP", "3"), null, List.of("warehouse:WH-HN"),
                () -> world.warehouse("WH-HN").remove("LAPTOP", 3));

        TraceEvent event = recorder.events().getFirst();
        assertThat(event.outcome()).isEqualTo(Outcome.OK);
        assertThat(event.exception()).isNull();
        assertThat(event.thread()).isEqualTo(Thread.currentThread().getName());
        assertThat(event.changes()).containsExactly(
                new ObjectChange("Warehouse WH-HN", Map.of("LAPTOP", 10), Map.of("LAPTOP", 7)));
    }

    @Test
    void thrownEventCoChangesRongVaNemLaiException() {
        assertThatThrownBy(() -> recorder.run("Warehouse", "remove", List.of("PHONE", "99"), null, List.of(),
                () -> world.warehouse("WH-HN").remove("PHONE", 99)))
                .isInstanceOf(OutOfStockException.class);

        TraceEvent event = recorder.events().getFirst();
        assertThat(event.outcome()).isEqualTo(Outcome.THROWN);
        assertThat(event.changes()).isEmpty();
        assertThat(event.exception().type()).isEqualTo("OutOfStockException");
        assertThat(event.exception().message()).contains("PHONE");
    }

    @Test
    void objectMoiTaoCoBeforeNull() {
        recorder.call("Order", "new", List.of(), null, List.of(), () -> {
            Order order = new Order(BigDecimal.ZERO, BigDecimal.ZERO);
            world.addOrder(order);
            return order;
        });

        ObjectChange change = recorder.events().getFirst().changes().getFirst();
        assertThat(change.ref()).startsWith("Order ");
        assertThat(change.before()).isNull();
        assertThat(change.after()).containsEntry("lines", 0);
    }
}
