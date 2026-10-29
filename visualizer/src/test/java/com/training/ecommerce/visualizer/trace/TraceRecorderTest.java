package com.training.ecommerce.visualizer.trace;

import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.visualizer.world.World;
import com.training.ecommerce.visualizer.world.WorldFactory;
import com.training.ecommerce.visualizer.world.WorldSnapshotter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TraceRecorderTest {

    private World world;
    private TraceRecorder recorder;

    @BeforeEach
    void setUp() {
        world = new WorldFactory().create();
        recorder = new TraceRecorder(world, new WorldSnapshotter());
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
    void seqTangDanTheoThuTuGoi() {
        for (int i = 0; i < 5; i++) {
            recorder.run("Cart", "isEmpty", List.of(), null, List.of(), () -> world.cart().isEmpty());
        }
        assertThat(recorder.events()).extracting(TraceEvent::seq).containsExactly(1L, 2L, 3L, 4L, 5L);
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

    @Test
    void objectBiXoaCoAfterNullVaFieldMotPhia() {
        var before = Map.<String, Map<String, Object>>of("X", Map.of("a", 1), "Y", Map.of("k", 1));
        var after = Map.<String, Map<String, Object>>of("Y", Map.of("k", 1, "new", 2));

        List<ObjectChange> changes = TraceRecorder.diff(before, after);

        assertThat(changes).hasSize(2);
        ObjectChange removed = changes.stream().filter(c -> c.ref().equals("X")).findFirst().orElseThrow();
        assertThat(removed.after()).isNull();
        assertThat(removed.before()).containsEntry("a", 1);
        ObjectChange added = changes.stream().filter(c -> c.ref().equals("Y")).findFirst().orElseThrow();
        assertThat(added.before()).isEmpty();
        assertThat(added.after()).containsOnlyKeys("new");
    }

    @Test
    void khongDoiGiThiKhongCoChange() {
        assertThat(TraceRecorder.diff(Map.of("A", Map.of("k", 1)), Map.of("A", Map.of("k", 1)))).isEmpty();
    }

    @Test
    void constructorVaCallValidateNull() {
        assertThatThrownBy(() -> new TraceRecorder(null, new WorldSnapshotter()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> recorder.call("A", "b", List.of(), null, List.of(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
