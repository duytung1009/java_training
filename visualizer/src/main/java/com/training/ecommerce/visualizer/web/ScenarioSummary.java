package com.training.ecommerce.visualizer.web;

import com.training.ecommerce.visualizer.scenario.Scenario;

/**
 * Mô tả ngắn một kịch bản để UI dựng danh sách chọn.
 *
 * @param id          id dùng trong URL
 * @param title       tiêu đề
 * @param session     buổi học liên quan
 * @param description mô tả điểm cần thấy
 */
public record ScenarioSummary(String id, String title, String session, String description) {

    public static ScenarioSummary of(Scenario scenario) {
        return new ScenarioSummary(scenario.id(), scenario.title(), scenario.session(), scenario.description());
    }
}
