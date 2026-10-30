package com.training.ecommerce.visualizer.core.model;

import com.training.ecommerce.visualizer.core.model.DomainModel.ClassDef;
import com.training.ecommerce.visualizer.core.model.DomainModel.ContextDef;
import com.training.ecommerce.visualizer.core.model.DomainModel.RelationDef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.training.ecommerce.visualizer.core.model.FakeDomain.SPEC;
import static com.training.ecommerce.visualizer.core.model.FakeDomain.classDef;
import static com.training.ecommerce.visualizer.core.model.FakeDomain.relation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainModelSuggesterTest {

    private final DomainModel model = DomainModelSuggester.suggest(SPEC);

    @Test
    void contextLaPackageConDuoiGocVaSapXepTheoId() {
        assertThat(model.version()).isEqualTo(1);
        assertThat(model.contexts()).extracting(ContextDef::id).containsExactly("billing", "sales");
        assertThat(model.contexts()).allSatisfy(c -> {
            assertThat(c.title()).isEqualTo(c.id());
            assertThat(c.color()).isNull();
        });
        assertThat(classDef(model, "Order").context()).isEqualTo("sales");
        assertThat(classDef(model, "Invoice").context()).isEqualTo("billing");
        assertThat(classDef(model, "Order").source()).isNotNull();
    }

    @Test
    void classNamTrucTiepTrongPackageGocCoContextNull() {
        ClassDef root = classDef(model, "RootThing");
        assertThat(root.context()).isNull();
        assertThat(model.contexts()).extracting(ContextDef::id).doesNotContain((String) null);
    }

    @Test
    void boQuaLopKhongPublic() {
        assertThat(model.classes()).extracting(ClassDef::cls).doesNotContain("Hidden");
    }

    @Test
    void throwableLaOtherVaAn() {
        ClassDef c = classDef(model, "OutOfStock");
        assertThat(c.role()).isEqualTo("other");
        assertThat(c.hidden()).isTrue();
    }

    @Test
    void enumVaRecordLaValue() {
        assertThat(classDef(model, "Status").role()).isEqualTo("value");
        assertThat(classDef(model, "Money").role()).isEqualTo("value");
        assertThat(classDef(model, "Status").hidden()).isFalse();
    }

    @Test
    void interfaceLaOther() {
        assertThat(classDef(model, "Priced").role()).isEqualTo("other");
        assertThat(classDef(model, "Identifiable").role()).isEqualTo("other");
    }

    @Test
    void dinhDanhCongStateDoiDuocKhongBiSoHuuLaAggregate() {
        ClassDef order = classDef(model, "Order");
        assertThat(order.role()).isEqualTo("aggregate");
        assertThat(order.source()).isEqualTo("identity");
    }

    @Test
    void dinhDanhQuaInterfaceIdentifiableVaKeThua() {
        assertThat(classDef(model, "Invoice").role()).isEqualTo("aggregate");
        assertThat(classDef(model, "Customer").role()).isEqualTo("aggregate");
        // lớp trừu tượng có field được xét như lớp thường
        assertThat(classDef(model, "BaseEntity").role()).isEqualTo("aggregate");
    }

    @Test
    void biSoHuuQuaGenericThiKhongPhaiAggregate() {
        ClassDef line = classDef(model, "Line");
        assertThat(line.role()).isEqualTo("entity");
        assertThat(line.source()).isEqualTo("field");
    }

    @Test
    void khongDinhDanhKhongStateThiService() {
        assertThat(classDef(model, "TaxRules").role()).isEqualTo("service");
    }

    @Test
    void lopConChuaLopChaKhongLamLopChaBiSoHuu() {
        assertThat(classDef(model, "Order").role()).isEqualTo("aggregate");
        assertThat(classDef(model, "BundleOrder").role()).isEqualTo("aggregate");
    }

    @Test
    void khongDinhDanhThiServiceHoacValue() {
        assertThat(classDef(model, "Discounter").role()).isEqualTo("service");
        assertThat(classDef(model, "RootThing").role()).isEqualTo("value");
        assertThat(classDef(model, "RootThing").source()).isEqualTo("field");
    }

    @Test
    void quanHeTuFieldVaTuParamVaFieldThangParam() {
        assertThat(relation(model, "Order", "Line").source()).isEqualTo("field");
        assertThat(relation(model, "Order", "Invoice").source()).isEqualTo("field");
        assertThat(relation(model, "Invoice", "Money").source()).isEqualTo("field");
        assertThat(relation(model, "Discounter", "Order").source()).isEqualTo("param");
    }

    @Test
    void khongCoQuanHeTuThanVaKhongTrung() {
        assertThat(model.relations()).noneMatch(r -> r.from().equals(r.to()));
        assertThat(model.relations().stream().map(r -> r.from() + ">" + r.to()).distinct().count())
                .isEqualTo(model.relations().size());
    }

    @Test
    void ketQuaXacDinhVaDaSapXep() {
        assertThat(DomainModelSuggester.suggest(SPEC)).isEqualTo(model);
        List<String> classes = model.classes().stream().map(ClassDef::cls).toList();
        assertThat(classes).isSorted();
        List<String> rel = model.relations().stream().map(r -> r.from() + ">" + r.to()).toList();
        assertThat(rel).isSorted();
    }

    @Test
    void gopQuanHeQuanSatGiuNguonTinh() {
        DomainModel merged = DomainModelSuggester.suggest(SPEC, List.of(
                new RelationDef("Order", "Line", "observed:s1"),
                new RelationDef("Customer", "Order", "observed:s1"),
                new RelationDef("Order", "Order", "observed:s1"),
                new RelationDef("Order", "KhongCo", "observed:s1")));
        assertThat(relation(merged, "Order", "Line").source()).isEqualTo("field");
        assertThat(relation(merged, "Customer", "Order").source()).isEqualTo("observed:s1");
        assertThat(merged.relations().size()).isEqualTo(model.relations().size() + 1);
    }

    @Test
    void validateThamSo() {
        assertThatThrownBy(() -> DomainModelSuggester.suggest(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DomainModelSuggester.suggest(SPEC, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
