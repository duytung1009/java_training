package com.training.ecommerce.visualizer.scenario;

import com.training.ecommerce.visualizer.trace.TraceRecorder;
import com.training.ecommerce.visualizer.world.World;
import com.training.ecommerce.visualizer.world.WorldFactory;
import com.training.ecommerce.visualizer.world.WorldSnapshotter;
import com.training.ecommerce.visualizer.world.WorldView;
import org.springframework.stereotype.Service;

import java.util.List;

/** Chạy một kịch bản trên thế giới mới và gói kết quả thành {@link RunResult}. */
@Service
public class ScenarioService {

    private final ScenarioRegistry registry;
    private final WorldFactory worldFactory;
    private final WorldSnapshotter snapshotter;

    /**
     * @throws IllegalArgumentException nếu tham số null
     */
    public ScenarioService(ScenarioRegistry registry, WorldFactory worldFactory, WorldSnapshotter snapshotter) {
        if (registry == null || worldFactory == null || snapshotter == null) {
            throw new IllegalArgumentException("registry, worldFactory và snapshotter không được null");
        }
        this.registry = registry;
        this.worldFactory = worldFactory;
        this.snapshotter = snapshotter;
    }

    public WorldView sampleWorld() {
        return snapshotter.snapshot(worldFactory.create()).view();
    }

    public List<Scenario> scenarios() {
        return registry.all();
    }

    /**
     * @throws ScenarioNotFoundException nếu id không tồn tại
     */
    public RunResult run(String id) {
        Scenario scenario = registry.get(id);
        World world = worldFactory.create();
        WorldView before = snapshotter.snapshot(world).view();
        TraceRecorder recorder = new TraceRecorder(world, snapshotter);
        scenario.run(world, recorder);
        return new RunResult(scenario.id(), scenario.title(), scenario.concurrent(), before, recorder.events());
    }
}
