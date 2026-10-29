package com.training.ecommerce.visualizer.scenario;

/** Không có kịch bản với id yêu cầu. */
public class ScenarioNotFoundException extends RuntimeException {

    private final String scenarioId;

    public ScenarioNotFoundException(String scenarioId) {
        super("Không tìm thấy kịch bản: " + scenarioId);
        this.scenarioId = scenarioId;
    }

    public String getScenarioId() {
        return scenarioId;
    }
}
