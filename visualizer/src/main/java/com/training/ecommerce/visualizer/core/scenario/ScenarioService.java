package com.training.ecommerce.visualizer.core.scenario;

import com.training.ecommerce.visualizer.core.spi.DomainEventSource;
import com.training.ecommerce.visualizer.core.spi.VisualizerAdapter;
import com.training.ecommerce.visualizer.core.trace.TraceRecorder;
import com.training.ecommerce.visualizer.core.view.WorldView;
import org.springframework.stereotype.Service;

import java.util.List;

/** Chạy một kịch bản trên thế giới mới và gói kết quả thành {@link RunResult}. */
@Service
public class ScenarioService {

    private final Bound<?> bound;

    /**
     * @throws IllegalArgumentException nếu adapter null, hoặc danh sách kịch bản null/trùng id
     */
    public ScenarioService(VisualizerAdapter<?> adapter) {
        if (adapter == null) {
            throw new IllegalArgumentException("adapter không được null");
        }
        this.bound = bind(adapter);
    }

    // Bắt wildcard thành một kiểu W cụ thể để adapter và registry dùng chung cùng W.
    private static <W> Bound<W> bind(VisualizerAdapter<W> adapter) {
        return new Bound<>(adapter, new ScenarioRegistry<>(adapter.scenarios()));
    }

    public WorldView sampleWorld() {
        return bound.sampleWorld();
    }

    public List<Scenario<?>> scenarios() {
        return bound.scenarios();
    }

    /**
     * @throws ScenarioNotFoundException nếu id không tồn tại
     */
    public RunResult run(String id) {
        return bound.run(id);
    }

    private record Bound<W>(VisualizerAdapter<W> adapter, ScenarioRegistry<W> registry) {

        WorldView sampleWorld() {
            return adapter.snapshot(adapter.createWorld()).view();
        }

        List<Scenario<?>> scenarios() {
            return List.copyOf(registry.all());
        }

        RunResult run(String id) {
            Scenario<W> scenario = registry.get(id);
            DomainEventSource<W> source = adapter.eventSource();
            W world = adapter.createWorld();
            WorldView before = adapter.snapshot(world).view();
            TraceRecorder recorder = new TraceRecorder(() -> adapter.snapshot(world),
                    () -> source.drain(world), adapter.eventMapper());
            scenario.run(world, recorder);
            return new RunResult(scenario.id(), scenario.title(), scenario.concurrent(), before, recorder.events());
        }
    }
}
