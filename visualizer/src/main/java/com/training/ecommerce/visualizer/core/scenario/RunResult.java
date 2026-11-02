package com.training.ecommerce.visualizer.core.scenario;

import com.training.ecommerce.visualizer.core.trace.TraceEvent;
import com.training.ecommerce.visualizer.core.view.WorldView;

import java.util.List;

/**
 * Kết quả cả một lượt chạy để frontend phát lại.
 *
 * @param scenarioId  id kịch bản
 * @param title       tiêu đề kịch bản
 * @param concurrent  true nếu kịch bản đa luồng (diff có thể xen kẽ)
 * @param worldBefore trạng thái trước khi chạy
 * @param events      các event theo thứ tự seq
 */
public record RunResult(String scenarioId, String title, boolean concurrent, WorldView worldBefore,
                        List<TraceEvent> events) {
}
