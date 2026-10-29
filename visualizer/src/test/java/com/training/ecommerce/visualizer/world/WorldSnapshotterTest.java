package com.training.ecommerce.visualizer.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorldSnapshotterTest {

    private final WorldSnapshotter snapshotter = new WorldSnapshotter();

    @Test
    void nodeIdThuTuVaChaCoDinh() {
        WorldView view = snapshotter.snapshot(new WorldFactory().create()).view();

        assertThat(view.nodes()).extracting(NodeView::id).containsExactly(
                "inventory", "warehouse:WH-HN", "warehouse:WH-HCM", "hub",
                "order", "cart", "orders", "catalog", "catalog:product:LAPTOP", "catalog:product:PHONE",
                "catalog:product:BOOK", "pricing", "shipping", "zone:INNER_CITY", "zone:OTHER_CITY");
        assertThat(view.nodes()).extracting(NodeView::kind).containsExactly(
                "area", "object", "object", "object", "area", "object", "collection", "area",
                "object", "object", "object", "area", "area", "object", "object");
        assertThat(view.nodes()).extracting(NodeView::parent).containsExactly(
                null, "inventory", "inventory", "inventory", null, "order", "order", null,
                "catalog", "catalog", "catalog", null, null, "shipping", "shipping");
        assertThat(view.nodes().stream().filter(n -> !n.kind().equals("object")))
                .allSatisfy(n -> assertThat(n.items()).isEmpty());
    }

    @Test
    void productCuaHubCoMatOCatalogNgayTuDau() {
        WorldView view = snapshotter.snapshot(new WorldFactory().create()).view();

        var products = view.nodes().stream().filter(n -> "catalog".equals(n.parent())).toList();
        assertThat(products).extracting(NodeView::title)
                .containsExactly("PhysicalProduct LAPTOP", "PhysicalProduct PHONE", "PhysicalProduct BOOK");
        // chưa tính giá: chỉ có basePrice, chưa có ô computed finalPrice
        products.forEach(n -> assertThat(n.items()).extracting(ItemView::key).containsExactly("basePrice"));
    }

    @Test
    void soLuongLaIntegerVaTienLaString() {
        WorldView view = snapshotter.snapshot(new WorldFactory().create()).view();

        NodeView hn = view.nodes().get(1);
        assertThat(hn.items()).extracting(ItemView::key, ItemView::label, ItemView::value)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("LAPTOP", "LAP", 10),
                        org.assertj.core.groups.Tuple.tuple("PHONE", "PHO", 6),
                        org.assertj.core.groups.Tuple.tuple("HEADPHONE", "HDP", 4));
        assertThat(hn.items().getFirst().value()).isInstanceOf(Integer.class);
    }

    @Test
    void nhanVietTatVaMoneyFormat() {
        assertThat(WorldSnapshotter.label("GIFT")).isEqualTo("GIF");
        assertThat(WorldSnapshotter.label("AB")).isEqualTo("AB");
        assertThat(MoneyFormat.shortOf(new java.math.BigDecimal("19440000"))).isEqualTo("19.4tr");
        assertThat(MoneyFormat.shortOf(new java.math.BigDecimal("27500"))).isEqualTo("27.5k");
        assertThat(MoneyFormat.full(new java.math.BigDecimal("19440000"))).isEqualTo("19 440 000");
        assertThat(MoneyFormat.percent(new java.math.BigDecimal("0.10"))).isEqualTo("10%");
    }

    @Test
    void snapshotNullNemIllegalArgument() {
        assertThatThrownBy(() -> snapshotter.snapshot(null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(List.of()).isEmpty();
    }
}
