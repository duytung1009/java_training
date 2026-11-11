package com.training.ecommerce.visualizer.ecommerce.scenario;

import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.shipping.Shipment;
import com.training.ecommerce.domain.shipping.ShipmentStatus;
import com.training.ecommerce.visualizer.core.scenario.Scenario;
import com.training.ecommerce.visualizer.core.trace.Flow;
import com.training.ecommerce.visualizer.core.trace.FlowKind;
import com.training.ecommerce.visualizer.core.trace.Outcome;
import com.training.ecommerce.visualizer.core.trace.TraceEvent;
import com.training.ecommerce.visualizer.core.trace.TraceRecorder;
import com.training.ecommerce.visualizer.core.view.ItemView;
import com.training.ecommerce.visualizer.core.view.NodeView;
import com.training.ecommerce.visualizer.ecommerce.EcommerceVisualizerAdapter;
import com.training.ecommerce.visualizer.ecommerce.world.World;
import com.training.ecommerce.visualizer.ecommerce.world.WorldFactory;
import com.training.ecommerce.visualizer.ecommerce.world.WorldSnapshotter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class ScenarioTest {

    private final WorldSnapshotter snapshotter = new WorldSnapshotter();

    static Stream<Scenario<World>> all() {
        return new EcommerceVisualizerAdapter().scenarios().stream();
    }

    private record Run(World world, List<TraceEvent> events) {
    }

    private Run run(Scenario<World> scenario) {
        World world = new WorldFactory().create();
        TraceRecorder recorder = new TraceRecorder(() -> snapshotter.snapshot(world));
        scenario.run(world, recorder);
        return new Run(world, recorder.events());
    }

    private Run run(String id) {
        return run(all().filter(s -> s.id().equals(id)).findFirst().orElseThrow());
    }

    private int totalStock(World world, String productId) {
        return world.warehouses().stream().mapToInt(w -> w.stockOf(productId)).sum();
    }

    @Test
    void coDungMuoiHaiKichBanTheoThuTu() {
        assertThat(all().map(Scenario::id)).containsExactly("overview", "cart-to-order", "shipping-fee",
                "pricing-types", "decrease-stock-ok", "decrease-stock-fail", "transfer", "transfer-deadlock-safe",
                "last-item-race", "replenish", "shipment-delivered", "shipment-returned");
    }

    @ParameterizedTest
    @MethodSource("all")
    void moiKichBanChayDuocVaCoEvent(Scenario<World> scenario) {
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
        // bước nhập mã SALE10 đứng ngay trước toOrder, mã hiện thành khối dưới domain.pricing
        int toOrderIndex = run.events().indexOf(toOrder);
        TraceEvent newCode = run.events().get(toOrderIndex - 1);
        assertThat(newCode.className()).isEqualTo("DiscountCode");
        assertThat(newCode.worldAfter().nodes()).anySatisfy(n -> {
            assertThat(n.id()).isEqualTo("pricing:code:SALE10");
            assertThat(n.parent()).isEqualTo(WorldSnapshotter.PRICING);
        });
        // toOrder(code, tax): flow giỏ → đơn và mã → đơn; Order chỉ giữ tỉ lệ 10% của mã
        assertThat(toOrder.args()).containsExactly("SALE10", "0.08");
        assertThat(toOrder.flows()).containsExactly(new Flow(WorldSnapshotter.CART, WorldSnapshotter.ORDERS),
                new Flow("pricing:code:SALE10", WorldSnapshotter.ORDERS, FlowKind.CALL, "rate"));
        assertThat(run.world().orders().getFirst().getDiscountRate()).isEqualByComparingTo("0.10");
        String orderNode = WorldSnapshotter.orderNodeId(run.world().orders().getFirst());
        assertThat(toOrder.panels()).containsExactly(WorldSnapshotter.CART, WorldSnapshotter.ORDERS,
                WorldSnapshotter.PRICING, "pricing:code:SALE10", orderNode);
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
        // 3 product của hub có sẵn trong catalog, 4 product kịch bản tự tạo đứng sau
        var catalog = nodes.stream().filter(n -> "catalog".equals(n.parent())).toList();
        assertThat(catalog).extracting(NodeView::id).startsWith(
                "catalog:product:LAPTOP", "catalog:product:PHONE", "catalog:product:BOOK");
        var products = catalog.subList(3, catalog.size());
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
    void overviewDiQuaMoiChangVaTonKhoKhop() {
        Run run = run("overview");
        World w = run.world();

        assertThat(run.events()).extracting(TraceEvent::outcome).containsOnly(Outcome.OK);
        assertThat(run.events()).extracting(TraceEvent::method).containsExactly("transfer", "replenish",
                "add", "add", "new", "toOrder", "decreaseStock", "feeFor", "new", "pickUp",
                "markDelivered");
        // HCM 2 → 0, HN 10 + 2 − 3 = 9, hub LAPTOP 5 + 3 − 2 = 6, PHONE 8 − 1 = 7
        assertThat(w.warehouse(WorldFactory.WH_HCM).stockOf("LAPTOP")).isZero();
        assertThat(w.warehouse(WorldFactory.WH_HN).stockOf("LAPTOP")).isEqualTo(9);
        assertThat(w.product("LAPTOP").getStock()).isEqualTo(6);
        assertThat(w.product("PHONE").getStock()).isEqualTo(7);
        assertThat(w.orders()).hasSize(1);
        Order order = w.orders().getFirst();
        assertThat(order.getDiscountRate()).isEqualByComparingTo("0.10");
        // giao thành công: kho không hoàn; phí cả đơn LAPTOP ×2 + PHONE ×1 về Hà Nội
        assertThat(shipmentStatuses(run)).containsExactly("CREATED", "IN_TRANSIT", "DELIVERED");
        assertThat(run.events().stream().filter(e -> e.method().equals("feeFor")).findFirst().orElseThrow().result())
                .isEqualTo("42000.00");
        // flow đi lần lượt kho → kho → hub → giỏ → đơn (cùng mã → đơn) → hub → vùng ship → List<Shipment>
        assertThat(run.events().stream().flatMap(e -> e.flows().stream())).containsExactly(
                new Flow(WorldSnapshotter.warehousePanelId(WorldFactory.WH_HCM), WorldSnapshotter.warehousePanelId(WorldFactory.WH_HN)),
                new Flow(WorldSnapshotter.warehousePanelId(WorldFactory.WH_HN), WorldSnapshotter.HUB),
                new Flow(WorldSnapshotter.HUB, WorldSnapshotter.CART),
                new Flow(WorldSnapshotter.HUB, WorldSnapshotter.CART),
                new Flow(WorldSnapshotter.CART, WorldSnapshotter.ORDERS),
                new Flow("pricing:code:SALE10", WorldSnapshotter.ORDERS, FlowKind.CALL, "rate"),
                new Flow(WorldSnapshotter.orderNodeId(order), WorldSnapshotter.HUB),
                new Flow(WorldSnapshotter.orderNodeId(order), "zone:INNER_CITY", FlowKind.CALL, "totalWeight"),
                new Flow(WorldSnapshotter.orderNodeId(order), WorldSnapshotter.SHIPMENTS, FlowKind.CALL, "orderId"));
    }

    @Test
    void replenishBaoToanTongVaCoHaiCaThatBai() {
        World seed = new WorldFactory().create();
        Run run = run("replenish");
        World w = run.world();
        for (String id : List.of("LAPTOP", "PHONE", "HEADPHONE", "BOOK")) {
            int before = totalStock(seed, id) + (seed.hub().findById(id).map(p -> p.getStock()).orElse(0));
            int after = totalStock(w, id) + (w.hub().findById(id).map(p -> p.getStock()).orElse(0));
            assertThat(after).as(id).isEqualTo(before);
        }
        assertThat(w.warehouse(WorldFactory.WH_HN).stockOf("LAPTOP")).isEqualTo(7);
        assertThat(w.product("LAPTOP").getStock()).isEqualTo(8);
        assertThat(w.warehouse(WorldFactory.WH_HCM).stockOf("PHONE")).isEqualTo(2);
        assertThat(w.product("PHONE").getStock()).isEqualTo(10);
        assertThat(w.warehouse(WorldFactory.WH_HN).stockOf("HEADPHONE")).isEqualTo(4);
        assertThat(w.product("BOOK").getStock()).isEqualTo(12);

        assertThat(run.events()).hasSize(4).extracting(TraceEvent::method).containsOnly("replenish");
        assertThat(run.events()).extracting(TraceEvent::outcome)
                .containsExactly(Outcome.OK, Outcome.OK, Outcome.THROWN, Outcome.THROWN);
        assertThat(run.events().get(2).exception().type()).isEqualTo("ProductNotFoundException");
        assertThat(run.events().get(3).exception().type()).isEqualTo("OutOfStockException");
        assertThat(run.events().get(2).changes()).isEmpty();
        assertThat(run.events().get(3).changes()).isEmpty();
        assertThat(run.events().getFirst().flows()).containsExactly(
                new Flow(WorldSnapshotter.warehousePanelId(WorldFactory.WH_HN), WorldSnapshotter.HUB));
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

    /** Status của vận đơn sau mỗi event thay đổi nó (ô status trên bản đồ). */
    private List<String> shipmentStatuses(Run run) {
        return run.events().stream().filter(e -> e.changes().stream().anyMatch(c -> c.ref().startsWith("Shipment ")))
                .map(e -> e.worldAfter().nodes().stream().filter(n -> n.id().startsWith("shipping:shipment:"))
                        .findFirst().orElseThrow().items().getFirst().value().toString()).toList();
    }

    @Test
    void shipmentDeliveredDiQuaDuongThuanVaKhongHoanKho() {
        Run run = run("shipment-delivered");
        assertThat(run.events()).extracting(TraceEvent::outcome).containsOnly(Outcome.OK);
        assertThat(run.events()).extracting(TraceEvent::method).containsExactly("new", "addLine", "addLine",
                "decreaseStock", "feeFor", "new", "pickUp", "markDelivered");
        // trừ kho đúng số đã đặt và không hoàn: LAPTOP 5 → 4, PHONE 8 → 7
        assertThat(run.world().product("LAPTOP").getStock()).isEqualTo(4);
        assertThat(run.world().product("PHONE").getStock()).isEqualTo(7);
        assertThat(run.events()).noneMatch(e -> e.method().equals("restock"));
        assertThat(shipmentStatuses(run)).containsExactly("CREATED", "IN_TRANSIT", "DELIVERED");
        TraceEvent fee = run.events().get(4);
        assertThat(fee.className()).isEqualTo("ShippingFeeCalculator");
        assertThat(fee.result()).isEqualTo("32000.00");
        NodeView node = run.events().getLast().worldAfter().nodes().stream()
                .filter(n -> n.id().startsWith("shipping:shipment:")).findFirst().orElseThrow();
        assertThat(node.parent()).isEqualTo(WorldSnapshotter.SHIPMENTS);
        assertThat(node.cls()).isEqualTo("Shipment");
        assertThat(node.items()).extracting(ItemView::key).containsExactly("status", "failedAttempts", "fee",
                "trackingCode");
        assertThat(node.items().getFirst().value()).isEqualTo(ShipmentStatus.DELIVERED.name());
        // tính phí: flow từ đơn tới vùng được chọn (Hà Nội → INNER_CITY); tạo vận đơn: flow tới List<Shipment>
        assertThat(fee.flows()).extracting(f -> f.to()).containsExactly("zone:INNER_CITY");
        assertThat(fee.panels()).contains("zone:INNER_CITY");
        assertThat(run.events().get(5).flows()).extracting(f -> f.to()).containsExactly(WorldSnapshotter.SHIPMENTS);
    }

    @Test
    void shipmentReturnedHoanKhoVeNhuTruocKhiDat() {
        World seed = new WorldFactory().create();
        Run run = run("shipment-returned");
        assertThat(run.world().product("LAPTOP").getStock()).isEqualTo(seed.product("LAPTOP").getStock());
        assertThat(run.world().product("PHONE").getStock()).isEqualTo(seed.product("PHONE").getStock());
        // Đà Nẵng → OTHER_CITY
        assertThat(run.events().stream().filter(e -> e.method().equals("feeFor")).findFirst().orElseThrow().flows())
                .extracting(f -> f.to()).containsExactly("zone:OTHER_CITY");
        // hoàn kho: flow vận đơn → hub mang orderId (shipping không gọi inventory)
        assertThat(run.events().stream().filter(e -> e.method().equals("restock")).flatMap(e -> e.flows().stream()))
                .extracting(Flow::to, Flow::label).containsOnly(tuple(WorldSnapshotter.HUB, "orderId"));
        // ca duy nhất lỗi: cancel khi IN_TRANSIT, trạng thái và history giữ nguyên
        List<TraceEvent> thrown = run.events().stream().filter(e -> e.outcome() == Outcome.THROWN).toList();
        assertThat(thrown).singleElement().satisfies(e -> {
            assertThat(e.method()).isEqualTo("cancel");
            assertThat(e.exception().type()).isEqualTo("IllegalShipmentTransitionException");
            assertThat(e.changes()).isEmpty();
        });
        // lần thất bại thứ 3 tự sang RETURNING
        assertThat(shipmentStatuses(run)).containsExactly("CREATED", "IN_TRANSIT", "IN_TRANSIT", "IN_TRANSIT",
                "RETURNING", "RETURNED");
        assertThat(run.events().stream().filter(e -> e.method().equals("failAttempt"))).hasSize(3);
        TraceEvent restock = run.events().getLast();
        assertThat(restock.method()).isEqualTo("restock");
        assertThat(restock.flows().getFirst().to()).isEqualTo(WorldSnapshotter.HUB);
        assertThat(restock.flows().getFirst().from()).startsWith("shipping:shipment:");
        assertThat(restock.changes()).isNotEmpty();
    }
}
