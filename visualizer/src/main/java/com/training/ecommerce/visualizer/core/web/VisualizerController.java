package com.training.ecommerce.visualizer.core.web;

import com.training.ecommerce.visualizer.core.model.DomainModelStore;
import com.training.ecommerce.visualizer.core.model.ModelAnnotator;
import com.training.ecommerce.visualizer.core.scenario.RunResult;
import com.training.ecommerce.visualizer.core.scenario.ScenarioService;
import com.training.ecommerce.visualizer.core.sync.DomainSyncService;
import com.training.ecommerce.visualizer.core.sync.SyncStatus;
import com.training.ecommerce.visualizer.core.view.WorldView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API của Domain Visualizer. Node được điền context/aggregate ở đây (không phải trong {@link ScenarioService})
 * để service không phụ thuộc store, vì store cần service để quan sát quan hệ.
 */
@RestController
@RequestMapping("/api/viz")
public class VisualizerController {

    private final ScenarioService service;
    private final DomainSyncService syncService;
    private final DomainModelStore modelStore;

    /**
     * @throws IllegalArgumentException nếu tham số null
     */
    public VisualizerController(ScenarioService service, DomainSyncService syncService,
                                DomainModelStore modelStore) {
        if (service == null || syncService == null || modelStore == null) {
            throw new IllegalArgumentException("service, syncService và modelStore không được null");
        }
        this.service = service;
        this.syncService = syncService;
        this.modelStore = modelStore;
    }

    /** Domain có lệch so với snapshot lần sync cuối không. */
    @GetMapping("/sync")
    public SyncStatus sync() {
        return syncService.status();
    }

    @GetMapping("/world")
    public WorldView world() {
        return ModelAnnotator.annotate(service.sampleWorld(), modelStore.current().model());
    }

    @GetMapping("/scenarios")
    public List<ScenarioSummary> scenarios() {
        return service.scenarios().stream().map(ScenarioSummary::of).toList();
    }

    /** id không tồn tại thì {@code ScenarioNotFoundException} được {@link RestExceptionHandler} đổi thành 404. */
    @PostMapping("/scenarios/{id}/run")
    public RunResult run(@PathVariable String id) {
        return ModelAnnotator.annotate(service.run(id), modelStore.current().model());
    }
}
