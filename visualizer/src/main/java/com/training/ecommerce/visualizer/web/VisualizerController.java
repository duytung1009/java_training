package com.training.ecommerce.visualizer.web;

import com.training.ecommerce.visualizer.scenario.RunResult;
import com.training.ecommerce.visualizer.scenario.ScenarioService;
import com.training.ecommerce.visualizer.sync.DomainSyncService;
import com.training.ecommerce.visualizer.sync.SyncStatus;
import com.training.ecommerce.visualizer.world.WorldView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** REST API của Domain Visualizer. */
@RestController
@RequestMapping("/api/viz")
public class VisualizerController {

    private final ScenarioService service;
    private final DomainSyncService syncService;

    /**
     * @throws IllegalArgumentException nếu tham số null
     */
    public VisualizerController(ScenarioService service, DomainSyncService syncService) {
        if (service == null || syncService == null) {
            throw new IllegalArgumentException("service và syncService không được null");
        }
        this.service = service;
        this.syncService = syncService;
    }

    /** Domain có lệch so với snapshot lần sync cuối không. */
    @GetMapping("/sync")
    public SyncStatus sync() {
        return syncService.status();
    }

    @GetMapping("/world")
    public WorldView world() {
        return service.sampleWorld();
    }

    @GetMapping("/scenarios")
    public List<ScenarioSummary> scenarios() {
        return service.scenarios().stream().map(ScenarioSummary::of).toList();
    }

    /** id không tồn tại thì {@code ScenarioNotFoundException} được {@link RestExceptionHandler} đổi thành 404. */
    @PostMapping("/scenarios/{id}/run")
    public RunResult run(@PathVariable String id) {
        return service.run(id);
    }
}
