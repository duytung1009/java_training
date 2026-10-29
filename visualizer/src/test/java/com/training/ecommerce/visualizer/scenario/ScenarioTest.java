package com.training.ecommerce.visualizer.scenario;

import com.training.ecommerce.visualizer.trace.Outcome;
import com.training.ecommerce.visualizer.trace.TraceEvent;
import com.training.ecommerce.visualizer.trace.TraceRecorder;
import com.training.ecommerce.visualizer.world.World;
import com.training.ecommerce.visualizer.world.WorldFactory;
import com.training.ecommerce.visualizer.world.WorldSnapshotter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class ScenarioTest {

    private final WorldSnapshotter snapshotter = new WorldSnapshotter();

    static Stream<Scenario> all() {
        return new ScenarioConfig().scenarioRegistry().all().stream();
    }

    private record Run(World world, List<TraceEvent> events) {
    }

    private Run run(Scenario scenario) {
        World world = new WorldFactory().create();
        TraceRecorder recorder = new TraceRecorder(world, snapshotter);
        scenario.run(world, recorder);
        return new Run(world, recorder.events());
    }

    private Run run(String id) {
        return run(new ScenarioConfig().scenarioRegistry().get(id));
    }

    private int totalStock(World world, String productId) {
        return world.warehouses().stream().mapToInt(w -> w.stockOf(productId)).sum();
    }

    @Test
    void coDungTamKichBanTheoThuTu() {
        assertThat(all().map(Scenario::id)).containsExactly("cart-to-order", "pricing-types",
                "decrease-stock-ok", "decrease-stock-fail", "transfer", "transfer-deadlock-safe",
                "last-item-race", "shipping-fee");
    }

    @ParameterizedTest
    @MethodSource("all")
    void moiKichBanChayDuocVaCoEvent(Scenario scenario) {
        Run run = assertTimeoutPreemptively(Duration.ofSeconds(10), () -> run(scenario));
        assertThat(run.events()).isNotEmpty();
        assertThat(run.events()).extracting(TraceEvent::seq).isSorted();
    }

    @Test
    void cartToOrderCoPopInPopOutVaOrder() {
        Run run = run("cart-to-order");

        assertThat(run.world().orders()).hasSize(1);
        assertThat(run.events().stream().filter(e -> e.method().equals("add")).findFirst().orElseThrow()
                .changes().getFirst().after()).containsEntry("LAPTOP", 1);
        TraceEvent remove = run.events().stream().filter(e -> e.method().equals("remove")).findFirst().orElseThrow();
        assertThat(remove.changes().getFirst().before()).containsKey("BOOK");
        assertThat(remove.changes().getFirst().after()).doesNotContainKey("BOOK");
        TraceEvent toOrder = run.events().getLast();
        assertThat(toOrder.changes().getFirst().before()).isNull();
        assertThat(toOrder.result()).startsWith("Order ");
    }

    @Test
    void pricingTypesGhiGiaVaoPanelPricing() {
        Run run = run("pricing-types");
        assertThat(run.events().getLast().worldAfter().panels().get(4).items())
                .extracting(i -> i.key()).containsExactly("DISC", "TAX", "PHY", "DIG", "SUB", "BUN", "THEN", "CHAIN", "SALE20");
    }

    @Test
    void decreaseStockOkTruCongDonMotLan() {
        Run run = run("decrease-stock-ok");
        assertThat(run.world().product("LAPTOP").getStock()).isEqualTo(2);
        assertThat(run.events().getLast().changes()).hasSize(1);
    }

    @Test
    void decreaseStockFailThrownVaChangesRong() {
        Run run = run("decrease-stock-fail");
        TraceEvent last = run.events().getLast();
        assertThat(last.outcome()).isEqualTo(Outcome.THROWN);
        assertThat(last.exception().type()).isEqualTo("OutOfStockException");
        assertThat(last.changes()).isEmpty();
        assertThat(run.world().product("LAPTOP").getStock()).isEqualTo(5);
    }

    @Test
    void transferBaoToanTongVaCoCaThatBai() {
        Run run = run("transfer");
        assertThat(totalStock(run.world(), "LAPTOP")).isEqualTo(12);
        assertThat(totalStock(run.world(), "HEADPHONE")).isEqualTo(4);
        assertThat(run.events().getLast().outcome()).isEqualTo(Outcome.THROWN);
        assertThat(run.events().getLast().changes()).isEmpty();
        assertThat(run.events().get(1).changes().get(1).before()).isEmpty();
    }

    @Test
    void transferDeadlockSafeXongTrongThoiGianVaBaoToanTong() {
        Run run = assertTimeoutPreemptively(Duration.ofSeconds(10), () -> run("transfer-deadlock-safe"));
        assertThat(totalStock(run.world(), "PHONE")).isEqualTo(10);
        assertThat(run.events().stream().map(TraceEvent::thread).distinct().count()).isEqualTo(2);
    }

    @Test
    void lastItemRaceDungMotOkVaStockKhongAm() {
        Run run = run("last-item-race");
        // event đầu: addProduct GIFT; sau đó mỗi thread một decreaseStock
        assertThat(run.events()).hasSize(LastItemRaceScenario.THREADS + 1);
        assertThat(run.events().getFirst().method()).isEqualTo("addProduct");
        assertThat(run.events().stream().filter(e -> e.method().equals("decreaseStock"))
                .filter(e -> e.outcome() == Outcome.OK)).hasSize(1);
        assertThat(run.world().product(LastItemRaceScenario.PRODUCT_ID).getStock()).isZero();
    }

    @Test
    void shippingFeeChonDungVung() {
        Run run = run("shipping-fee");
        List<TraceEvent> zones = run.events().stream().filter(e -> e.method().equals("of")).toList();
        assertThat(zones).extracting(TraceEvent::result).containsExactly("INNER_CITY", "OTHER_CITY");
        assertThat(run.events().stream().filter(e -> e.method().equals("feeFor")))
                .allSatisfy(e -> assertThat(e.changes()).isEmpty());
        var panels = run.events().getLast().worldAfter().panels();
        assertThat(panels.get(6).items()).extracting(i -> i.value()).containsExactly("27.5k");
        assertThat(panels.get(7).items()).extracting(i -> i.value()).containsExactly("42.5k");
    }
}
