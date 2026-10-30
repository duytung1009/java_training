package com.training.ecommerce.visualizer.core.model;

import com.training.ecommerce.visualizer.core.model.DomainModel.RelationDef;
import com.training.ecommerce.visualizer.core.scenario.RunResult;
import com.training.ecommerce.visualizer.core.scenario.Scenario;
import com.training.ecommerce.visualizer.core.scenario.ScenarioService;
import com.training.ecommerce.visualizer.core.trace.Flow;
import com.training.ecommerce.visualizer.core.trace.TraceEvent;
import com.training.ecommerce.visualizer.core.view.NodeView;
import com.training.ecommerce.visualizer.core.view.WorldView;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Quan hệ quan sát được: chạy mọi kịch bản, quy node hai đầu của mỗi flow về class domain. Bắt được tham chiếu
 * bằng id mà reflection sót.
 */
public final class ObservedRelations {

    private ObservedRelations() {
    }

    /**
     * Chạy mọi kịch bản của {@code service}. Chỉ giữ cặp có hai đầu là hai class domain khác nhau; nguồn là
     * {@code observed:<id kịch bản đầu tiên thấy cặp đó>}.
     *
     * @param knownClasses tên đơn giản các class domain
     * @throws IllegalArgumentException nếu tham số null
     */
    public static List<RelationDef> observe(ScenarioService service, Set<String> knownClasses) {
        if (service == null || knownClasses == null) {
            throw new IllegalArgumentException("service và knownClasses không được null");
        }
        Map<String, RelationDef> pairs = new LinkedHashMap<>();
        for (Scenario<?> scenario : service.scenarios()) {
            RunResult run = service.run(scenario.id());
            Map<String, String> before = classByNodeId(run.worldBefore());
            for (TraceEvent event : run.events()) {
                Map<String, String> after = classByNodeId(event.worldAfter());
                for (Flow flow : event.flows()) {
                    String from = after.getOrDefault(flow.from(), before.get(flow.from()));
                    String to = after.getOrDefault(flow.to(), before.get(flow.to()));
                    if (from != null && to != null && !from.equals(to)
                            && knownClasses.contains(from) && knownClasses.contains(to)) {
                        pairs.putIfAbsent(from + "\u0000" + to,
                                new RelationDef(from, to, "observed:" + scenario.id()));
                    }
                }
            }
        }
        return List.copyOf(pairs.values());
    }

    private static Map<String, String> classByNodeId(WorldView world) {
        Map<String, String> result = new HashMap<>();
        if (world != null) {
            for (NodeView node : world.nodes()) {
                String cls = NodeClasses.classOf(node);
                if (cls != null) {
                    result.put(node.id(), cls);
                }
            }
        }
        return result;
    }
}
