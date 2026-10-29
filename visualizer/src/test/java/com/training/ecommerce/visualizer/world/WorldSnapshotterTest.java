package com.training.ecommerce.visualizer.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorldSnapshotterTest {

    private final WorldSnapshotter snapshotter = new WorldSnapshotter();

    @Test
    void panelIdVaThuTuCoDinh() {
        WorldView view = snapshotter.snapshot(new WorldFactory().create()).view();

        assertThat(view.panels()).extracting(PanelView::id).containsExactly(
                "warehouse:WH-HN", "warehouse:WH-HCM", "hub", "cart", "pricing", "order",
                "zone:INNER_CITY", "zone:OTHER_CITY");
    }

    @Test
    void soLuongLaIntegerVaTienLaString() {
        WorldView view = snapshotter.snapshot(new WorldFactory().create()).view();

        PanelView hn = view.panels().getFirst();
        assertThat(hn.items()).extracting(ItemView::key, ItemView::label, ItemView::value)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("LAPTOP", "LAP", 10),
                        org.assertj.core.groups.Tuple.tuple("PHONE", "PHO", 6),
                        org.assertj.core.groups.Tuple.tuple("HEADPHONE", "HDP", 4));
        assertThat(hn.items().getFirst().value()).isInstanceOf(Integer.class);
        PanelView pricing = view.panels().get(4);
        assertThat(pricing.items()).extracting(ItemView::value).containsExactly("-10%", "+8%");
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
