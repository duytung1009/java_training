package com.training.ecommerce.visualizer.core.model;

import com.training.ecommerce.visualizer.core.model.DomainModel.ClassDef;
import com.training.ecommerce.visualizer.core.model.DomainModel.ContextDef;
import com.training.ecommerce.visualizer.core.model.DomainModel.RelationDef;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainModelValidatorTest {

    private final DomainModelValidator validator = new DomainModelValidator(Set.of("Order", "Line", "Invoice"));

    private static DomainModel model(Integer version, List<ContextDef> contexts, List<ClassDef> classes,
                                     List<RelationDef> relations) {
        return new DomainModel(version, contexts, classes, relations);
    }

    private static ContextDef ctx(String id) {
        return new ContextDef(id, id, null);
    }

    private static ClassDef cls(String name, String context, String role) {
        return new ClassDef(name, context, role, false, null);
    }

    private List<ModelError> errors(DomainModel m) {
        return validator.validate(m);
    }

    @Test
    void moHinhHopLeKhongCoLoi() {
        assertThat(errors(model(1, List.of(ctx("sales")),
                List.of(cls("Order", "sales", "aggregate"), cls("Line", "sales", "entity")),
                List.of(new RelationDef("Order", "Line", "field"))))).isEmpty();
    }

    @Test
    void contextNullDuocPhep() {
        assertThat(errors(model(1, List.of(), List.of(cls("Order", null, "value")), List.of()))).isEmpty();
    }

    @Test
    void thieuHoacSaiVersion() {
        assertThat(errors(model(null, List.of(), List.of(), List.of()))).extracting(ModelError::message)
                .anyMatch(m -> m.contains("version")).hasSize(1);
        assertThat(errors(model(2, List.of(), List.of(), List.of()))).extracting(ModelError::message)
                .anyMatch(m -> m.contains("version"));
        assertThat(errors(model(null, List.of(), List.of(), List.of())).get(0).cls()).isNull();
    }

    @Test
    void classKhongCoTrongDomain() {
        List<ModelError> e = errors(model(1, List.of(ctx("a")), List.of(cls("Ma", "a", "value")), List.of()));
        assertThat(e).hasSize(1);
        assertThat(e.get(0).cls()).isEqualTo("Ma");
    }

    @Test
    void classTrungLapTrongNhieuContext() {
        List<ModelError> e = errors(model(1, List.of(ctx("a"), ctx("b")),
                List.of(cls("Order", "a", "value"), cls("Order", "b", "value")), List.of()));
        assertThat(e).hasSize(1);
        assertThat(e.get(0).cls()).isEqualTo("Order");
    }

    @Test
    void contextKhongKhaiBao() {
        List<ModelError> e = errors(model(1, List.of(ctx("a")), List.of(cls("Order", "zzz", "value")), List.of()));
        assertThat(e).hasSize(1);
        assertThat(e.get(0).cls()).isEqualTo("Order");
    }

    @Test
    void roleKhongHopLe() {
        assertThat(errors(model(1, List.of(ctx("a")), List.of(cls("Order", "a", "king")), List.of())))
                .hasSize(1);
        assertThat(errors(model(1, List.of(ctx("a")), List.of(cls("Order", "a", null)), List.of())))
                .hasSize(1);
    }

    @Test
    void quanHeTroToClassKhongLietKe() {
        List<ModelError> e = errors(model(1, List.of(ctx("a")), List.of(cls("Order", "a", "value")),
                List.of(new RelationDef("Order", "Line", "field"), new RelationDef("Ma", "Order", "field"))));
        assertThat(e).hasSize(2);
    }

    @Test
    void contextTrungId() {
        List<ModelError> e = errors(model(1, List.of(ctx("a"), ctx("a")), List.of(), List.of()));
        assertThat(e).hasSize(1);
        assertThat(e.get(0).cls()).isNull();
    }

    @Test
    void moHinhNullLaLoi() {
        assertThat(errors(null)).isNotEmpty();
    }

    @Test
    void constructorValidate() {
        assertThatThrownBy(() -> new DomainModelValidator(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
