package com.training.ecommerce.visualizer.core.scenario;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Danh sách kịch bản theo thứ tự hiển thị.
 *
 * @param <W> kiểu "thế giới" của adapter
 */
public class ScenarioRegistry<W> {

    private final Map<String, Scenario<W>> byId = new LinkedHashMap<>();

    /**
     * @throws IllegalArgumentException nếu scenarios null, chứa null hoặc trùng id
     */
    public ScenarioRegistry(List<Scenario<W>> scenarios) {
        if (scenarios == null || scenarios.stream().anyMatch(s -> s == null)) {
            throw new IllegalArgumentException("scenarios không được null hoặc chứa null");
        }
        for (Scenario<W> scenario : scenarios) {
            if (byId.putIfAbsent(scenario.id(), scenario) != null) {
                throw new IllegalArgumentException("Trùng id kịch bản: " + scenario.id());
            }
        }
    }

    public List<Scenario<W>> all() {
        return List.copyOf(byId.values());
    }

    /**
     * @throws ScenarioNotFoundException nếu id không tồn tại
     */
    public Scenario<W> get(String id) {
        Scenario<W> scenario = id == null ? null : byId.get(id);
        if (scenario == null) {
            throw new ScenarioNotFoundException(id);
        }
        return scenario;
    }
}
