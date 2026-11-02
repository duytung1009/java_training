package com.training.ecommerce.visualizer.core.trace;

import com.training.ecommerce.visualizer.core.view.WorldSnapshot;
import com.training.ecommerce.visualizer.core.view.WorldView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TraceRecorderTest {

    /** Domain giả tối thiểu: các object có tên, mỗi object là một map field → giá trị. */
    private static final class FakeWorld {
        private final Map<String, Map<String, Object>> objects = new LinkedHashMap<>();

        synchronized void put(String ref, String field, Object value) {
            objects.computeIfAbsent(ref, k -> new LinkedHashMap<>()).put(field, value);
        }

        synchronized void take(String ref, String field, int amount, int available) {
            if (amount > available) {
                throw new IllegalStateException("Không đủ " + field + ": cần " + amount);
            }
            put(ref, field, available - amount);
        }

        /** Chụp sâu để diff so sánh hai thời điểm khác nhau. */
        synchronized WorldSnapshot snapshot() {
            Map<String, Map<String, Object>> copy = new LinkedHashMap<>();
            objects.forEach((ref, fields) -> copy.put(ref, new LinkedHashMap<>(fields)));
            return new WorldSnapshot(new WorldView(List.of()), copy);
        }
    }

    private FakeWorld world;
    private TraceRecorder recorder;

    @BeforeEach
    void setUp() {
        world = new FakeWorld();
        world.put("Stock A", "LAPTOP", 10);
        Supplier<WorldSnapshot> snapshotter = world::snapshot;
        recorder = new TraceRecorder(snapshotter);
    }

    @Test
    void okEventCoChangesChiGomFieldDoi() {
        recorder.run("Stock", "take", List.of("LAPTOP", "3"), null, List.of("stock:A"),
                () -> world.take("Stock A", "LAPTOP", 3, 10));

        TraceEvent event = recorder.events().getFirst();
        assertThat(event.outcome()).isEqualTo(Outcome.OK);
        assertThat(event.exception()).isNull();
        assertThat(event.thread()).isEqualTo(Thread.currentThread().getName());
        assertThat(event.changes()).containsExactly(
                new ObjectChange("Stock A", Map.of("LAPTOP", 10), Map.of("LAPTOP", 7)));
    }

    @Test
    void thrownEventCoChangesRongVaNemLaiException() {
        assertThatThrownBy(() -> recorder.run("Stock", "take", List.of("LAPTOP", "99"), null, List.of(),
                () -> world.take("Stock A", "LAPTOP", 99, 10)))
                .isInstanceOf(IllegalStateException.class);

        TraceEvent event = recorder.events().getFirst();
        assertThat(event.outcome()).isEqualTo(Outcome.THROWN);
        assertThat(event.changes()).isEmpty();
        assertThat(event.exception().type()).isEqualTo("IllegalStateException");
        assertThat(event.exception().message()).contains("LAPTOP");
    }

    @Test
    void seqTangDanTheoThuTuGoi() {
        for (int i = 0; i < 5; i++) {
            recorder.run("Stock", "noop", List.of(), null, List.of(), () -> { });
        }
        assertThat(recorder.events()).extracting(TraceEvent::seq).containsExactly(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    void objectMoiTaoCoBeforeNull() {
        recorder.call("Item", "new", List.of(), null, List.of(), () -> {
            world.put("Item 1", "lines", 0);
            return "Item 1";
        });

        ObjectChange change = recorder.events().getFirst().changes().getFirst();
        assertThat(change.ref()).startsWith("Item ");
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
        assertThatThrownBy(() -> new TraceRecorder(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> recorder.call("A", "b", List.of(), null, List.of(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
