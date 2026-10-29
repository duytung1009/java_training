package com.training.ecommerce.visualizer.trace;

import com.training.ecommerce.visualizer.world.World;
import com.training.ecommerce.visualizer.world.WorldSnapshot;
import com.training.ecommerce.visualizer.world.WorldSnapshotter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Ghi lại từng lần gọi method domain: chụp snapshot trước, gọi method thật, chụp snapshot sau,
 * rồi so sánh để ra {@code changes}. Thread-safe: nhiều thread cùng gọi {@link #call} được.
 *
 * <p>Với kịch bản đa luồng, snapshot chụp ngoài lock của domain nên {@code changes} của một event
 * có thể gồm thay đổi của thread khác chạy xen. Đây là giới hạn đã chấp nhận.
 */
public class TraceRecorder {

    private final World world;
    private final WorldSnapshotter snapshotter;
    private final AtomicLong sequence = new AtomicLong();
    private final Collection<TraceEvent> events = new ConcurrentLinkedQueue<>();

    /**
     * @throws IllegalArgumentException nếu world hoặc snapshotter null
     */
    public TraceRecorder(World world, WorldSnapshotter snapshotter) {
        if (world == null || snapshotter == null) {
            throw new IllegalArgumentException("world và snapshotter không được null");
        }
        this.world = world;
        this.snapshotter = snapshotter;
    }

    /**
     * Gọi {@code action} và ghi lại một event với tối đa một flow.
     *
     * @param flow flow duy nhất của bước này, null nếu không có
     * @return kết quả của action
     * @throws IllegalArgumentException nếu className, method, args, panels hoặc action null
     * @throws RuntimeException         exception của action được ghi lại (THROWN) rồi ném lại nguyên vẹn
     */
    public <T> T call(String className, String method, List<String> args, Flow flow, List<String> panels,
                      Supplier<T> action) {
        return callFlows(className, method, args, flow == null ? List.of() : List.of(flow), panels, action);
    }

    /**
     * Như {@link #call} nhưng một bước phát nhiều flow lần lượt theo thứ tự trong {@code flows}.
     *
     * @param flows các flow theo thứ tự phát, null coi như rỗng
     * @throws IllegalArgumentException nếu className, method, args, panels hoặc action null, hoặc flows có phần tử null
     * @throws RuntimeException         exception của action được ghi lại (THROWN) rồi ném lại nguyên vẹn
     */
    public <T> T callFlows(String className, String method, List<String> args, List<Flow> flows,
                           List<String> panels, Supplier<T> action) {
        if (className == null || method == null || args == null || panels == null || action == null) {
            throw new IllegalArgumentException("className, method, args, panels và action không được null");
        }
        List<Flow> safeFlows;
        try {
            safeFlows = flows == null ? List.of() : List.copyOf(flows);
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("flows không được chứa phần tử null", e);
        }
        WorldSnapshot before = snapshotter.snapshot(world);
        T result;
        try {
            result = action.get();
        } catch (RuntimeException e) {
            record(className, method, args, Outcome.THROWN, null, ExceptionInfo.of(e), safeFlows, panels, before);
            throw e;
        }
        record(className, method, args, Outcome.OK, result == null ? null : String.valueOf(result), null,
                safeFlows, panels, before);
        return result;
    }

    /**
     * Như {@link #call} cho action không có kết quả (void).
     *
     * @throws IllegalArgumentException nếu tham số null
     */
    public void run(String className, String method, List<String> args, Flow flow, List<String> panels,
                    Runnable action) {
        runFlows(className, method, args, flow == null ? List.of() : List.of(flow), panels, action);
    }

    /**
     * Như {@link #callFlows} cho action không có kết quả (void).
     *
     * @throws IllegalArgumentException nếu tham số null hoặc flows có phần tử null
     */
    public void runFlows(String className, String method, List<String> args, List<Flow> flows,
                         List<String> panels, Runnable action) {
        if (action == null) {
            throw new IllegalArgumentException("action không được null");
        }
        callFlows(className, method, args, flows, panels, () -> {
            action.run();
            return null;
        });
    }

    /** Các event đã ghi, sắp theo seq tăng dần. */
    public List<TraceEvent> events() {
        return events.stream().sorted(Comparator.comparingLong(TraceEvent::seq)).toList();
    }

    private void record(String className, String method, List<String> args, Outcome outcome, String result,
                        ExceptionInfo exception, List<Flow> flows, List<String> panels, WorldSnapshot before) {
        WorldSnapshot after = snapshotter.snapshot(world);
        // seq cấp lúc hoàn tất để thứ tự log = thứ tự thực sự kết thúc
        events.add(new TraceEvent(sequence.incrementAndGet(), Instant.now().toString(),
                Thread.currentThread().getName(), className, method, List.copyOf(args), outcome, result,
                exception, flows, List.copyOf(panels), diff(before.objects(), after.objects()), after.view()));
    }

    /** So sánh hai snapshot object, chỉ giữ field có đổi. */
    static List<ObjectChange> diff(Map<String, Map<String, Object>> before, Map<String, Map<String, Object>> after) {
        Set<String> refs = new LinkedHashSet<>(before.keySet());
        refs.addAll(after.keySet());
        List<ObjectChange> changes = new ArrayList<>();
        for (String ref : refs) {
            Map<String, Object> b = before.get(ref);
            Map<String, Object> a = after.get(ref);
            if (b == null) {
                changes.add(new ObjectChange(ref, null, a));
            } else if (a == null) {
                changes.add(new ObjectChange(ref, b, null));
            } else {
                diffFields(ref, b, a).ifPresent(changes::add);
            }
        }
        return List.copyOf(changes);
    }

    private static Optional<ObjectChange> diffFields(String ref, Map<String, Object> b,
                                                                Map<String, Object> a) {
        Set<String> keys = new LinkedHashSet<>(b.keySet());
        keys.addAll(a.keySet());
        Map<String, Object> changedBefore = new LinkedHashMap<>();
        Map<String, Object> changedAfter = new LinkedHashMap<>();
        for (String key : keys) {
            if (!Objects.equals(b.get(key), a.get(key))) {
                if (b.containsKey(key)) {
                    changedBefore.put(key, b.get(key));
                }
                if (a.containsKey(key)) {
                    changedAfter.put(key, a.get(key));
                }
            }
        }
        if (changedBefore.isEmpty() && changedAfter.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ObjectChange(ref, changedBefore, changedAfter));
    }
}
