package com.training.ecommerce.visualizer.scenario;

import com.training.ecommerce.visualizer.trace.Flow;
import com.training.ecommerce.visualizer.trace.Outcome;
import com.training.ecommerce.visualizer.trace.TraceEvent;
import com.training.ecommerce.visualizer.trace.TraceRecorder;
import com.training.ecommerce.visualizer.world.ItemView;
import com.training.ecommerce.visualizer.world.NodeView;
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
        assertThat(all().map(Scenario::id)).containsExactly("cart-to-order", "shipping-fee",
                "pricing-types", "decrease-stock-ok", "decrease-stock-fail", "transfer", "transfer-deadlock-safe",
                "last-item-race");
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
        TraceEvent toOrder = run.events().stream().filter(e -> e.method().equals("toOrder")).findFirst().orElseThrow();
        assertThat(toOrder.changes().getFirst().before()).isNull();
        assertThat(toOrder.result()).startsWith("Order ");
        assertThat(toOrder.flows()).containsExactly(new Flow(WorldSnapshotter.CART, WorldSnapshotter.ORDERS));
        String orderNode = WorldSnapshotter.orderNodeId(run.world().orders().getFirst());
        assertThat(toOrder.panels()).containsExactly(WorldSnapshotter.CART, WorldSnapshotter.ORDERS, orderNode);
        var orderNodes = run.events().getLast().worldAfter().nodes().stream()
                .filter(n -> n.id().startsWith("order:")).toList();
        assertThat(orderNodes).hasSize(1);
        assertThat(orderNodes.getFirst().parent()).isEqualTo("orders");
        var orders = run.events().getLast().worldAfter().nodes().stream()
                .filter(n -> n.id().equals("orders")).findFirst().orElseThrow();
        assertThat(orders.parent()).isEqualTo("order");
        assertThat(orderNodes.getFirst().id()).isEqualTo(orderNode);
        assertThat(orderNodes.getFirst().items()).hasSize(2)
                .allSatisfy(i -> {
                    assertThat(i.type()).isEqualTo("OrderLine");
                    assertThat(i.computed()).isFalse();
                });
        // Cart không trừ kho: sau add/changeQuantity/remove/toOrder tồn kho vẫn như seed (LAPTOP 5, PHONE 8)
        TraceEvent decrease = run.events().getLast();
        assertThat(decrease.method()).isEqualTo("decreaseStock");
        assertThat(run.events()).filteredOn(e -> !e.method().equals("decreaseStock"))
                .allSatisfy(e -> assertThat(e.changes()).noneMatch(c -> c.ref().startsWith("Product")));
        assertThat(decrease.flows()).containsExactly(new Flow(orderNode, WorldSnapshotter.HUB));
        // chỉ InventoryManager.decreaseStock(order) trừ: LAPTOP 5 → 4, PHONE 8 → 7
        assertThat(run.world().product("LAPTOP").getStock()).isEqualTo(4);
        assertThat(run.world().product("PHONE").getStock()).isEqualTo(7);
    }

    @Test
    void pricingTypesTaoNodeConCuaPricing() {
        Run run = run("pricing-types");
        var nodes = run.events().getLast().worldAfter().nodes();
        var products = nodes.stream().filter(n -> "catalog".equals(n.parent())).toList();
        assertThat(products).extracting(NodeView::id).containsExactly(
                "catalog:product:LAPTOP-X", "catalog:product:EBOOK", "catalog:product:NEWS",
                "catalog:product:COMBO");
        assertThat(products).extracting(NodeView::cls).containsExactly(
                "PhysicalProduct", "DigitalProduct", "SubscriptionProduct", "BundleProduct");
        var children = nodes.stream().filter(n -> "pricing".equals(n.parent())).toList();
        assertThat(children).extracting(NodeView::id).containsExactly(
                "pricing:rule:then", "pricing:rule:chain", "pricing:code:SALE20", "pricing:rule:sale20");
        assertThat(children).extracting(NodeView::cls).containsExactly(
                "PricingRule", "PricingRule", "DiscountCode", "PricingRule");
        products.forEach(n -> {
            assertThat(n.items()).extracting(ItemView::key).containsExactly("basePrice", "finalPrice");
            assertThat(n.items()).extracting(ItemView::computed).containsExactly(false, true);
        });
        NodeView rule = children.stream().filter(n -> n.id().equals("pricing:rule:sale20")).findFirst().orElseThrow();
        assertThat(rule.items()).singleElement().satisfies(i -> {
            assertThat(i.computed()).isTrue();
            assertThat(i.label()).startsWith("apply(");
        });
        NodeView code = children.stream().filter(n -> n.id().equals("pricing:code:SALE20")).findFirst().orElseThrow();
        assertThat(code.items()).extracting(ItemView::key, ItemView::value)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("code", "SALE20"),
                        org.assertj.core.groups.Tuple.tuple("rate", "20%"));
        // giá là hàm thuần: bước tính giá của rule không tạo diff cho chính rule/giá
        assertThat(run.events().getLast().changes()).isEmpty();
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
        // event đầu: addProduct GIFT; rồi tạo các đơn (vào List<Order>); sau đó mỗi thread một decreaseStock
        assertThat(run.events()).hasSize(LastItemRaceScenario.THREADS + 2);
        assertThat(run.events().getFirst().method()).isEqualTo("addProduct");
        assertThat(run.events().get(1).method()).isEqualTo("new");
        assertThat(run.world().orders()).hasSize(LastItemRaceScenario.THREADS);
        // flow decreaseStock xuất phát từ đúng khối Order đang có trên bản đồ
        assertThat(run.events().stream().filter(e -> e.method().equals("decreaseStock"))).allSatisfy(e ->
                assertThat(e.worldAfter().nodes()).extracting(n -> n.id())
                        .contains(e.flows().getFirst().from()));
        assertThat(run.events().stream().filter(e -> e.method().equals("decreaseStock"))
                .filter(e -> e.outcome() == Outcome.OK)).hasSize(1);
        assertThat(run.world().product(LastItemRaceScenario.PRODUCT_ID).getStock()).isZero();
    }

    @Test
    void shippingFeeChonDungVung() {
        Run run = run("shipping-fee");
        List<TraceEvent> zones = run.events().stream().filter(e -> e.method().equals("of")).toList();
        assertThat(zones).extracting(TraceEvent::result).containsExactly("INNER_CITY", "OTHER_CITY");
        List<TraceEvent> fees = run.events().stream().filter(e -> e.method().equals("calculateShipping")).toList();
        assertThat(fees).hasSize(2).allSatisfy(e -> assertThat(e.changes()).isEmpty());
        assertThat(fees).extracting(e -> e.flows().getFirst())
                .containsExactly(new Flow(WorldSnapshotter.HUB, "zone:INNER_CITY"),
                        new Flow(WorldSnapshotter.HUB, "zone:OTHER_CITY"));
        var nodes = run.events().getLast().worldAfter().nodes().stream()
                .filter(n -> n.id().startsWith("zone:")).toList();
        assertThat(nodes).extracting(NodeView::cls).containsOnly("ShippingZone");
        nodes.forEach(n -> {
            assertThat(n.items().getFirst().key()).isEqualTo("baseFee");
            assertThat(n.items().getFirst().computed()).isFalse();
            assertThat(n.items().subList(1, n.items().size())).allSatisfy(i -> assertThat(i.computed()).isTrue());
        });
        assertThat(nodes.get(0).items().subList(1, 2)).extracting(i -> i.value()).containsExactly("30.0k");
        assertThat(nodes.get(1).items().subList(1, 2)).extracting(i -> i.value()).containsExactly("37.0k");
    }
}
