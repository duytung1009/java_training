package com.training.ecommerce.visualizer.ecommerce.model;

import com.training.ecommerce.visualizer.core.model.DomainModel;
import com.training.ecommerce.visualizer.core.model.DomainModel.ClassDef;
import com.training.ecommerce.visualizer.core.model.DomainModel.ContextDef;
import com.training.ecommerce.visualizer.core.model.DomainModel.RelationDef;
import com.training.ecommerce.visualizer.core.model.DomainModelStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Mốc so sánh: gợi ý mô hình cho domain ecommerce thật (kèm quan hệ quan sát từ 12 kịch bản). */
@SpringBootTest
class EcommerceDomainModelTest {

    @Autowired
    private DomainModelStore store;

    private DomainModel suggestion() {
        return store.suggestion();
    }

    private ClassDef cls(String name) {
        return suggestion().classes().stream().filter(c -> c.cls().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("Không có " + name));
    }

    @Test
    void contextTheoPackageCuaDomain() {
        assertThat(suggestion().contexts()).extracting(ContextDef::id)
                .containsExactly("catalog", "customer", "inventory", "order", "pricing", "shipping");
    }

    @Test
    void orderLaAggregateConOrderLineThiKhong() {
        assertThat(cls("Order").role()).isEqualTo("aggregate");
        assertThat(cls("Order").context()).isEqualTo("order");
        assertThat(cls("OrderLine").role()).isNotEqualTo("aggregate");
        assertThat(cls("Warehouse").role()).isEqualTo("aggregate");
        // BundleProduct chứa List<Product> nhưng là lớp con: Product vẫn là aggregate
        assertThat(cls("Product").role()).isEqualTo("aggregate");
        assertThat(cls("ShippingFeeCalculator").role()).isEqualTo("service");
        assertThat(cls("WarehouseTransferService").role()).isEqualTo("service");
    }

    @Test
    void enumLaValueVaExceptionBiAn() {
        assertThat(cls("ShipmentStatus").role()).isEqualTo("value");
        assertThat(cls("OutOfStockException").hidden()).isTrue();
    }

    @Test
    void coQuanHeQuanSatTuFlowCuaKichBan() {
        assertThat(suggestion().relations()).anyMatch(r -> r.source().startsWith("observed:"));
        assertThat(suggestion().relations()).extracting(RelationDef::source).anyMatch(s -> s.equals("field"));
    }

    @Test
    void goiYChiGomClassDomain() {
        assertThat(suggestion().classes()).extracting(ClassDef::cls)
                .doesNotContain("Identifiable", "TextUtils", "Repository");
    }
}
