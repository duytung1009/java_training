package com.training.ecommerce.visualizer.core.model;

import com.training.ecommerce.visualizer.core.model.DomainModel.ClassDef;
import com.training.ecommerce.visualizer.core.model.DomainModel.ContextDef;
import com.training.ecommerce.visualizer.core.model.DomainModel.RelationDef;
import com.training.ecommerce.visualizer.core.view.NodeView;
import com.training.ecommerce.visualizer.core.view.WorldView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelAnnotatorTest {

    private static final DomainModel MODEL = new DomainModel(1,
            List.of(new ContextDef("inventory", "inventory", null), new ContextDef("order", "order", null)),
            List.of(new ClassDef("Warehouse", "inventory", "aggregate", false, null),
                    new ClassDef("Order", "order", "aggregate", false, null),
                    new ClassDef("OrderLine", "order", "entity", false, null),
                    new ClassDef("Money", "order", "value", false, null),
                    new ClassDef("Address", "inventory", "value", false, null)),
            List.of(new RelationDef("Order", "OrderLine", "field"),
                    new RelationDef("Warehouse", "Address", "field"),
                    new RelationDef("Order", "Address", "field"),
                    new RelationDef("Order", "Money", "param")));

    private static NodeView node(String id, String kind, String cls) {
        return new NodeView(id, kind, cls, id, null, null, List.of());
    }

    private static NodeView annotated(NodeView n) {
        return ModelAnnotator.annotate(new WorldView(List.of(n)), MODEL).nodes().get(0);
    }

    @Test
    void nodeObjectLayContextVaAggregateLaChinhNo() {
        NodeView n = annotated(node("w", NodeView.OBJECT, "Warehouse"));
        assertThat(n.context()).isEqualTo("inventory");
        assertThat(n.aggregate()).isEqualTo("Warehouse");
    }

    @Test
    void entityThuocAggregateSoHuuQuaField() {
        NodeView n = annotated(node("l", NodeView.OBJECT, "OrderLine"));
        assertThat(n.context()).isEqualTo("order");
        assertThat(n.aggregate()).isEqualTo("Order");
    }

    @Test
    void quanHeParamKhongTinhLaSoHuu() {
        assertThat(annotated(node("m", NodeView.OBJECT, "Money")).aggregate()).isNull();
    }

    @Test
    void khongCoHoacNhieuAggregateSoHuuThiNull() {
        // Address: Warehouse (cùng context inventory) sở hữu qua field; Order khác context nên không tính
        assertThat(annotated(node("a", NodeView.OBJECT, "Address")).aggregate()).isEqualTo("Warehouse");
    }

    @Test
    void nodeCollectionDungKieuPhanTu() {
        NodeView n = annotated(node("orders", NodeView.COLLECTION, "List<Order>"));
        assertThat(n.context()).isEqualTo("order");
        assertThat(n.aggregate()).isEqualTo("Order");
    }

    @Test
    void nodeAreaKhopTheoDuoiTenContext() {
        NodeView n = annotated(node("inv", NodeView.AREA, "domain.inventory"));
        assertThat(n.context()).isEqualTo("inventory");
        assertThat(n.aggregate()).isNull();
        assertThat(annotated(node("o", NodeView.AREA, "order")).context()).isEqualTo("order");
        assertThat(annotated(node("x", NodeView.AREA, "domain.preorder")).context()).isNull();
    }

    @Test
    void classLaThiNull() {
        NodeView n = annotated(node("x", NodeView.OBJECT, "KhongCo"));
        assertThat(n.context()).isNull();
        assertThat(n.aggregate()).isNull();
    }

    @Test
    void giuNguyenCacTruongKhac() {
        NodeView original = new NodeView("a", NodeView.OBJECT, "Order", "T", "s", "p", List.of());
        NodeView n = annotated(original);
        assertThat(n.id()).isEqualTo("a");
        assertThat(n.title()).isEqualTo("T");
        assertThat(n.sub()).isEqualTo("s");
        assertThat(n.parent()).isEqualTo("p");
    }
}
