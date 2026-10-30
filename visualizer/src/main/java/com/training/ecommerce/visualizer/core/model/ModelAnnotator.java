package com.training.ecommerce.visualizer.core.model;

import com.training.ecommerce.visualizer.core.model.DomainModel.ClassDef;
import com.training.ecommerce.visualizer.core.model.DomainModel.ContextDef;
import com.training.ecommerce.visualizer.core.model.DomainModel.RelationDef;
import com.training.ecommerce.visualizer.core.scenario.RunResult;
import com.training.ecommerce.visualizer.core.trace.TraceEvent;
import com.training.ecommerce.visualizer.core.view.NodeView;
import com.training.ecommerce.visualizer.core.view.WorldView;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Điền {@code context} và {@code aggregate} vào node theo một {@link DomainModel}. */
public final class ModelAnnotator {

    private ModelAnnotator() {
    }

    /**
     * Trả về WorldView mới với context/aggregate được điền; class không có trong mô hình cho null.
     *
     * @throws IllegalArgumentException nếu tham số null
     */
    public static WorldView annotate(WorldView world, DomainModel model) {
        if (world == null || model == null) {
            throw new IllegalArgumentException("world và model không được null");
        }
        return new WorldView(world.nodes().stream().map(n -> annotate(n, model)).toList());
    }

    /**
     * Annotate {@code worldBefore} và mọi {@code worldAfter} của lượt chạy.
     *
     * @throws IllegalArgumentException nếu tham số null
     */
    public static RunResult annotate(RunResult run, DomainModel model) {
        if (run == null || model == null) {
            throw new IllegalArgumentException("run và model không được null");
        }
        List<TraceEvent> events = run.events().stream().map(e -> new TraceEvent(e.seq(), e.at(), e.thread(),
                e.className(), e.method(), e.args(), e.outcome(), e.result(), e.exception(), e.flows(), e.panels(),
                e.changes(), e.worldAfter() == null ? null : annotate(e.worldAfter(), model), e.events(),
                e.causedBy())).toList();
        return new RunResult(run.scenarioId(), run.title(), run.concurrent(),
                run.worldBefore() == null ? null : annotate(run.worldBefore(), model), events);
    }

    private static NodeView annotate(NodeView node, DomainModel model) {
        String context = null;
        String aggregate = null;
        if (NodeView.AREA.equals(node.kind())) {
            context = areaContext(node.cls(), model);
        } else {
            ClassDef def = find(model, NodeClasses.classOf(node));
            if (def != null) {
                context = def.context();
                aggregate = aggregateOf(def, model);
            }
        }
        return new NodeView(node.id(), node.kind(), node.cls(), node.title(), node.sub(), node.parent(),
                node.items(), context, aggregate);
    }

    private static ClassDef find(DomainModel model, String cls) {
        if (cls == null) {
            return null;
        }
        return model.classes().stream().filter(c -> cls.equals(c.cls())).findFirst().orElse(null);
    }

    private static String areaContext(String cls, DomainModel model) {
        if (cls == null) {
            return null;
        }
        return model.contexts().stream().map(ContextDef::id)
                .filter(id -> cls.equals(id) || cls.endsWith("." + id)).findFirst().orElse(null);
    }

    private static String aggregateOf(ClassDef def, DomainModel model) {
        switch (def.role() == null ? "" : def.role()) {
            case "aggregate":
                return def.cls();
            case "entity":
            case "value":
                List<String> owners = model.relations().stream()
                        .filter(r -> "field".equals(r.source()) && def.cls().equals(r.to()))
                        .map(RelationDef::from).distinct()
                        .filter(from -> {
                            ClassDef owner = find(model, from);
                            return owner != null && "aggregate".equals(owner.role())
                                    && Objects.equals(owner.context(), def.context());
                        }).collect(Collectors.toList());
                return owners.size() == 1 ? owners.get(0) : null;
            default:
                return null;
        }
    }
}
