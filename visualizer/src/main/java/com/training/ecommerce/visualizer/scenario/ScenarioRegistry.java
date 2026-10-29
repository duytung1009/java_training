package com.training.ecommerce.visualizer.scenario;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Danh sách kịch bản theo thứ tự hiển thị. */
public class ScenarioRegistry {

    private final Map<String, Scenario> byId = new LinkedHashMap<>();

    /**
     * @throws IllegalArgumentException nếu scenarios null, chứa null hoặc trùng id
     */
    public ScenarioRegistry(List<Scenario> scenarios) {
        if (scenarios == null || scenarios.stream().anyMatch(s -> s == null)) {
            throw new IllegalArgumentException("scenarios không được null hoặc chứa null");
        }
        for (Scenario scenario : scenarios) {
            if (byId.putIfAbsent(scenario.id(), scenario) != null) {
                throw new IllegalArgumentException("Trùng id kịch bản: " + scenario.id());
            }
        }
    }

    public List<Scenario> all() {
        return List.copyOf(byId.values());
    }

    /**
     * @throws ScenarioNotFoundException nếu id không tồn tại
     */
    public Scenario get(String id) {
        Scenario scenario = id == null ? null : byId.get(id);
        if (scenario == null) {
            throw new ScenarioNotFoundException(id);
        }
        return scenario;
    }
}
