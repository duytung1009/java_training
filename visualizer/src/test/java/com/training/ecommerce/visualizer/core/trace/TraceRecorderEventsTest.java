package com.training.ecommerce.visualizer.core.trace;

import com.training.ecommerce.visualizer.core.spi.DomainEventMapper;
import com.training.ecommerce.visualizer.core.spi.EventFlow;
import com.training.ecommerce.visualizer.core.trace.fake.FakeAccount;
import com.training.ecommerce.visualizer.core.trace.fake.FakeLedger;
import com.training.ecommerce.visualizer.core.trace.fake.LargeDeposit;
import com.training.ecommerce.visualizer.core.trace.fake.MoneyDeposited;
import com.training.ecommerce.visualizer.core.view.WorldSnapshot;
import com.training.ecommerce.visualizer.core.view.WorldView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cơ chế domain event của {@link TraceRecorder}: rút event, flow event, emit theo thread, phản ứng causedBy. */
class TraceRecorderEventsTest {

    private static final Flow CALL_FLOW = new Flow("a", "b");

    private FakeAccount account;
    private TraceRecorder recorder;

    /** Chỉ ánh xạ MoneyDeposited; LargeDeposit không có flow. */
    private static final DomainEventMapper MAPPER = event ->
            event instanceof MoneyDeposited m
                    ? Optional.of(new EventFlow("b", "c", null, m.account()))
                    : Optional.empty();

    private static WorldSnapshot emptySnapshot() {
        return new WorldSnapshot(new WorldView(List.of()), Map.of());
    }

    @BeforeEach
    void setUp() {
        account = new FakeAccount("ACC-1");
        recorder = new TraceRecorder(TraceRecorderEventsTest::emptySnapshot, account::pullDomainEvents, MAPPER);
    }

    private void deposit(long amount) {
        recorder.run("FakeAccount", "deposit", List.of(String.valueOf(amount)), CALL_FLOW, List.of("b"),
                () -> account.deposit(amount));
    }

    @Test
    void loiGoiSinhHaiEventGhiDungThuTuVaFlowEventSauFlowGoi() {
        deposit(5000);

        TraceEvent e = recorder.events().get(0);
        assertThat(e.events()).extracting(DomainEventView::type).containsExactly("MoneyDeposited", "LargeDeposit");
        assertThat(e.events()).extracting(DomainEventView::id).containsExactly("ev-1", "ev-2");
        assertThat(e.flows()).containsExactly(CALL_FLOW, new Flow("b", "c", FlowKind.EVENT, "MoneyDeposited"));
        assertThat(e.causedBy()).isNull();
    }

    @Test
    void eventKhongCoMapperVanGhiVaoLogNhungKhongSinhFlow() {
        deposit(5000);

        DomainEventView large = recorder.events().get(0).events().get(1);
        assertThat(large.type()).isEqualTo("LargeDeposit");
        assertThat(large.payload()).isEqualTo(String.valueOf(new LargeDeposit("ACC-1", 5000)));
        assertThat(large.from()).isNull();
        assertThat(large.to()).isNull();
        assertThat(large.context()).isNull();
        DomainEventView mapped = recorder.events().get(0).events().get(0);
        assertThat(mapped.from()).isEqualTo("b");
        assertThat(mapped.to()).isEqualTo("c");
        assertThat(mapped.context()).isEqualTo("ACC-1");
        assertThat(recorder.events().get(0).flows()).hasSize(2);
    }

    @Test
    void khongCoNguonEventThiEventsRongVaCausedByNull() {
        TraceRecorder plain = new TraceRecorder(TraceRecorderEventsTest::emptySnapshot);
        plain.run("FakeAccount", "deposit", List.of(), CALL_FLOW, List.of(), () -> account.deposit(5000));

        TraceEvent e = plain.events().get(0);
        assertThat(e.events()).isEmpty();
        assertThat(e.causedBy()).isNull();
        assertThat(e.flows()).containsExactly(CALL_FLOW);
        assertThat(CALL_FLOW.kind()).isEqualTo(FlowKind.CALL);
        assertThat(CALL_FLOW.label()).isNull();
    }

    @Test
    void actionNemExceptionVanRutEventDaGhiTruocKhiNem() {
        assertThatThrownBy(() -> recorder.run("FakeAccount", "depositThenFail", List.of(), CALL_FLOW,
                List.of(), () -> account.depositThenFail(10))).isInstanceOf(IllegalStateException.class);

        TraceEvent e = recorder.events().get(0);
        assertThat(e.outcome()).isEqualTo(Outcome.THROWN);
        assertThat(e.events()).extracting(DomainEventView::type).containsExactly("MoneyDeposited");
        assertThat(e.flows()).extracting(Flow::kind).containsExactly(FlowKind.CALL, FlowKind.EVENT);
    }

    @Test
    void emitTrongLoiGoiGanVaoLoiGoiDoVaDungTruocEventRutTuNguon() {
        recorder.run("FakeAccount", "deposit", List.of(), CALL_FLOW, List.of(), () -> {
            recorder.emit(new MoneyDeposited("EMITTED", 1));
            account.deposit(2);
        });

        assertThat(recorder.events().get(0).events()).extracting(DomainEventView::context)
                .containsExactly("EMITTED", "ACC-1");
    }

    @Test
    void emitTuHaiThreadChayChongNhauKhongLanEvent() throws Exception {
        // Mỗi thread một recorder-chung: hai lời gọi phải chồng thời gian mới chứng minh được ThreadLocal
        TraceRecorder shared = new TraceRecorder(TraceRecorderEventsTest::emptySnapshot, List::of, MAPPER);
        CountDownLatch bothInside = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> fs = List.of("T1", "T2").stream().map(name -> pool.submit(() ->
                    shared.run("X", "m", List.of(), null, List.of(), () -> {
                        shared.emit(new MoneyDeposited(name, 1));
                        bothInside.countDown();
                        try {
                            assertThat(bothInside.await(5, TimeUnit.SECONDS)).isTrue();
                        } catch (InterruptedException ex) {
                            throw new IllegalStateException(ex);
                        }
                        shared.emit(new LargeDeposit(name, 2));
                    }))).collect(java.util.stream.Collectors.toList());
            for (Future<?> f : fs) {
                f.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        List<TraceEvent> events = shared.events();
        assertThat(events).hasSize(2);
        List<String> owners = new java.util.ArrayList<>();
        for (TraceEvent e : events) {
            assertThat(e.events()).hasSize(2);
            String owner = e.events().get(0).payload().contains("T1") ? "T1" : "T2";
            assertThat(e.events()).allSatisfy(ev -> assertThat(ev.payload()).contains(owner));
            owners.add(owner);
        }
        assertThat(owners).containsExactlyInAnyOrder("T1", "T2");
    }

    @Test
    void emitNgoaiLoiGoiNemIllegalState() {
        assertThatThrownBy(() -> recorder.emit(new MoneyDeposited("x", 1))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void emitNullNemIllegalArgument() {
        assertThatThrownBy(() -> recorder.emit(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emitSauKhiLoiGoiKetThucKhongConBuffer() {
        recorder.run("X", "m", List.of(), null, List.of(), () -> { });
        assertThatThrownBy(() -> recorder.emit(new MoneyDeposited("x", 1))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reactGhiTraceEventRiengCoCausedByVaSeqLonHon() {
        FakeLedger ledger = new FakeLedger();
        deposit(5000);
        List<Object> raw = recorder.lastEvents();
        assertThat(raw).hasSize(2);
        for (Object e : raw) {
            recorder.react(e, "FakeLedger", "on", List.of(e.getClass().getSimpleName()), List.of(new Flow("c", "d")),
                    List.of("d"), () -> ledger.on(e));
        }

        List<TraceEvent> all = recorder.events();
        assertThat(all).hasSize(3);
        assertThat(all.get(0).causedBy()).isNull();
        assertThat(all.get(1).causedBy()).isEqualTo("ev-1");
        assertThat(all.get(2).causedBy()).isEqualTo("ev-2");
        assertThat(all.get(1).seq()).isGreaterThan(all.get(0).seq());
        assertThat(all.get(1).className()).isEqualTo("FakeLedger");
        assertThat(ledger.entries()).hasSize(2);
        // react không ghi đè lastEvents của lời gọi gốc
        assertThat(recorder.lastEvents()).hasSize(2);
    }

    @Test
    void reactVoiEventLaHoacNullNemIllegalArgument() {
        assertThatThrownBy(() -> recorder.react(new MoneyDeposited("x", 1), "L", "on", List.of(), List.of(),
                List.of(), () -> { })).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> recorder.react(null, "L", "on", List.of(), List.of(), List.of(), () -> { }))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(recorder.events()).isEmpty();
    }

    @Test
    void lastEventsRongKhiChuaCoLoiGoi() {
        assertThat(recorder.lastEvents()).isEmpty();
    }

    @Test
    void loiGoiLongTrongReactChiReactEventCoCausedBy() {
        deposit(5);
        Object event = recorder.lastEvents().get(0);

        recorder.react(event, "FakeLedger", "on", List.of(), List.of(), List.of(), () ->
                recorder.run("FakeLedger", "inner", List.of(), null, List.of(), () -> { }));

        List<TraceEvent> all = recorder.events();
        assertThat(all).hasSize(3);
        TraceEvent inner = all.stream().filter(e -> e.method().equals("inner")).findFirst().orElseThrow();
        TraceEvent outer = all.stream().filter(e -> e.method().equals("on")).findFirst().orElseThrow();
        assertThat(inner.causedBy()).isNull();
        assertThat(outer.causedBy()).isEqualTo("ev-1");
    }

    @Test
    void khoiTaoVoiThamSoNullNemIllegalArgument() {
        assertThatThrownBy(() -> new TraceRecorder(null, List::of, DomainEventMapper.none()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TraceRecorder(TraceRecorderEventsTest::emptySnapshot, null,
                DomainEventMapper.none())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TraceRecorder(TraceRecorderEventsTest::emptySnapshot, List::of, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void eventFlowVaFlowKiemTraThamSo() {
        assertThatThrownBy(() -> new EventFlow(" ", "b", null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EventFlow("a", null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new Flow("a", "b", null, null).kind()).isEqualTo(FlowKind.CALL);
    }
}
