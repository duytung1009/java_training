package com.training.ecommerce.visualizer.core.trace;

import com.training.ecommerce.visualizer.core.spi.DomainEventMapper;
import com.training.ecommerce.visualizer.core.spi.EventFlow;
import com.training.ecommerce.visualizer.core.view.WorldSnapshot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.IdentityHashMap;
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
 *
 * <p><b>Domain event.</b> Sau mỗi lời gọi, recorder gom event theo thứ tự: trước hết các event được
 * {@link #emit} trong lúc lời gọi chạy trên cùng thread, rồi kết quả của {@code drainEvents}. Việc rút chạy cả khi
 * action ném exception: {@code TraceEvent} THROWN vẫn mang các event đã ghi trước lúc ném. Mỗi event thành một
 * {@link DomainEventView}; event có kết quả mapper còn sinh thêm {@link Flow} kiểu EVENT, đặt sau các flow của
 * lời gọi. Phản ứng của listener ghi bằng {@link #react}, có {@code causedBy} = id của event gây ra.
 * Event {@code emit} luôn đúng thread; còn {@code drainEvents} rút chung cả thế giới, nên trong kịch bản đa luồng
 * event do thread khác ghi xen có thể gắn vào lời gọi đang kết thúc (cùng giới hạn với {@code changes}).
 */
public class TraceRecorder {

    private final Supplier<WorldSnapshot> snapshotter;
    private final Supplier<? extends List<?>> drainEvents;
    private final DomainEventMapper mapper;
    private final AtomicLong sequence = new AtomicLong();
    private final AtomicLong eventSequence = new AtomicLong();
    private final Collection<TraceEvent> events = new ConcurrentLinkedQueue<>();
    // Event object → id đã cấp (so sánh theo identity, vì record event có thể equals nhau)
    private final Map<Object, String> eventIds = Collections.synchronizedMap(new IdentityHashMap<>());
    // Ngăn xếp buffer của các lời gọi đang chạy trên thread này; emit ghi vào buffer trong cùng (lời gọi lồng sâu nhất)
    private final ThreadLocal<Deque<List<Object>>> buffers = ThreadLocal.withInitial(ArrayDeque::new);
    // Event của lời gọi cấp ngoài cùng vừa xong trên thread này (không tính react)
    private final ThreadLocal<List<Object>> lastEvents = ThreadLocal.withInitial(List::of);

    /**
     * Recorder không có domain event: {@code events} luôn rỗng.
     *
     * @param snapshotter chụp trạng thái hiện tại của thế giới; được gọi trước và sau mỗi lần gọi domain
     * @throws IllegalArgumentException nếu snapshotter null
     */
    public TraceRecorder(Supplier<WorldSnapshot> snapshotter) {
        this(snapshotter, List::of, DomainEventMapper.none());
    }

    /**
     * @param snapshotter chụp trạng thái hiện tại của thế giới; được gọi trước và sau mỗi lần gọi domain
     * @param drainEvents rút (và xoá) các event domain đã ghi từ lần rút trước; gọi sau mỗi lời gọi, kể cả khi lỗi
     * @param mapper      biến event thành flow trên bản đồ
     * @throws IllegalArgumentException nếu tham số nào null
     */
    public TraceRecorder(Supplier<WorldSnapshot> snapshotter, Supplier<? extends List<?>> drainEvents,
                         DomainEventMapper mapper) {
        if (snapshotter == null || drainEvents == null || mapper == null) {
            throw new IllegalArgumentException("snapshotter, drainEvents và mapper không được null");
        }
        this.snapshotter = snapshotter;
        this.drainEvents = drainEvents;
        this.mapper = mapper;
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
        return callFlows(className, method, args, flows, panels, action, null);
    }

    /** Lõi của {@code callFlows}; {@code cause} là id event gây ra lời gọi (chỉ {@link #react} truyền), null nếu không có. */
    private <T> T callFlows(String className, String method, List<String> args, List<Flow> flows,
                            List<String> panels, Supplier<T> action, String cause) {
        if (className == null || method == null || args == null || panels == null || action == null) {
            throw new IllegalArgumentException("className, method, args, panels và action không được null");
        }
        List<Flow> safeFlows;
        try {
            safeFlows = flows == null ? List.of() : List.copyOf(flows);
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("flows không được chứa phần tử null", e);
        }
        Deque<List<Object>> stack = buffers.get();
        List<Object> emitted = new ArrayList<>();
        stack.push(emitted);
        boolean topLevel = stack.size() == 1 && cause == null;
        try {
            WorldSnapshot before = snapshotter.get();
            T result;
            try {
                result = action.get();
            } catch (RuntimeException e) {
                record(className, method, args, Outcome.THROWN, null, ExceptionInfo.of(e), safeFlows, panels, before,
                        collect(emitted), cause, topLevel);
                throw e;
            }
            record(className, method, args, Outcome.OK, result == null ? null : String.valueOf(result), null,
                    safeFlows, panels, before, collect(emitted), cause, topLevel);
            return result;
        } finally {
            stack.pop();
            if (stack.isEmpty()) {
                buffers.remove(); // thread trong pool không giữ lại ThreadLocal
            }
        }
    }

    /**
     * Ghi một domain event từ bên trong lời gọi đang chạy (vd. từ listener) vào lời gọi trong cùng nhất của
     * <b>thread hiện tại</b>. Không chụp snapshot và không gọi gì khác, nên an toàn khi domain đang giữ lock.
     *
     * @throws IllegalArgumentException nếu event null
     * @throws IllegalStateException    nếu thread hiện tại không có lời gọi {@code call}/{@code run} nào đang chạy
     */
    public void emit(Object event) {
        if (event == null) {
            throw new IllegalArgumentException("event không được null");
        }
        List<Object> current = buffers.get().peek();
        if (current == null) {
            buffers.remove();
            throw new IllegalStateException("emit chỉ gọi được bên trong một lời gọi call/run đang chạy");
        }
        current.add(event);
    }

    /**
     * Các event (đối tượng gốc) gắn vào lời gọi cấp ngoài cùng hoàn tất gần nhất của thread hiện tại; lời
     * {@link #react} không ghi đè. Rỗng nếu chưa có.
     */
    public List<Object> lastEvents() {
        return lastEvents.get();
    }

    /**
     * Ghi phản ứng của một listener trước một domain event, như {@link #runFlows}, nhưng {@code TraceEvent} kết quả
     * có {@code causedBy} = id của event. Chỉ chính lần ghi này mang {@code causedBy}; lời gọi lồng bên trong thì không.
     *
     * @param domainEvent event gây ra phản ứng, phải do recorder này ghi
     * @throws IllegalArgumentException nếu domainEvent null hoặc chưa từng được recorder này ghi, hoặc tham số
     *                                  khác không hợp lệ như {@link #runFlows}
     */
    public void react(Object domainEvent, String className, String method, List<String> args, List<Flow> flows,
                      List<String> panels, Runnable action) {
        if (domainEvent == null) {
            throw new IllegalArgumentException("domainEvent không được null");
        }
        String id = eventIds.get(domainEvent);
        if (id == null) {
            throw new IllegalArgumentException("domainEvent chưa được recorder này ghi");
        }
        if (action == null) {
            throw new IllegalArgumentException("action không được null");
        }
        callFlows(className, method, args, flows, panels, () -> {
            action.run();
            return null;
        }, id);
    }

    /** Event của lời gọi: trước hết các event emit cùng thread, rồi event rút từ nguồn của domain. */
    private List<Object> collect(List<Object> emitted) {
        List<Object> all = new ArrayList<>(emitted);
        List<?> drained = drainEvents.get();
        if (drained != null) {
            all.addAll(drained);
        }
        return all;
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
                        ExceptionInfo exception, List<Flow> flows, List<String> panels, WorldSnapshot before,
                        List<Object> rawEvents, String causedBy, boolean topLevel) {
        List<DomainEventView> views = new ArrayList<>();
        List<Flow> allFlows = new ArrayList<>(flows);
        for (Object event : rawEvents) {
            String id = "ev-" + eventSequence.incrementAndGet();
            eventIds.put(event, id);
            Optional<EventFlow> mapped = mapper.map(event);
            EventFlow ef = mapped == null ? null : mapped.orElse(null);
            String type = typeOf(event);
            views.add(new DomainEventView(id, type, ef == null ? null : ef.context(), String.valueOf(event),
                    ef == null ? null : ef.from(), ef == null ? null : ef.to()));
            if (ef != null) {
                allFlows.add(new Flow(ef.from(), ef.to(), FlowKind.EVENT, ef.label() != null ? ef.label() : type));
            }
        }
        if (topLevel) {
            lastEvents.set(List.copyOf(rawEvents));
        }
        WorldSnapshot after = snapshotter.get();
        // seq cấp lúc hoàn tất để thứ tự log = thứ tự thực sự kết thúc
        events.add(new TraceEvent(sequence.incrementAndGet(), Instant.now().toString(),
                Thread.currentThread().getName(), className, method, List.copyOf(args), outcome, result,
                exception, List.copyOf(allFlows), List.copyOf(panels), diff(before.objects(), after.objects()),
                after.view(), views, causedBy));
    }

    private static String typeOf(Object event) {
        String name = event.getClass().getSimpleName();
        return name.isEmpty() ? event.getClass().getName() : name;
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
