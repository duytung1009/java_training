package com.training.ecommerce.visualizer.scenario;

import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.domain.inventory.WarehouseTransferService;
import com.training.ecommerce.domain.order.Cart;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.pricing.DiscountCode;
import com.training.ecommerce.domain.shipping.Shipment;
import com.training.ecommerce.visualizer.trace.Flow;
import com.training.ecommerce.visualizer.trace.TraceRecorder;
import com.training.ecommerce.visualizer.world.World;
import com.training.ecommerce.visualizer.world.WorldFactory;
import com.training.ecommerce.visualizer.world.WorldSnapshotter;

import java.math.BigDecimal;
import java.util.List;

/**
 * S02–S07: một lượt mua hàng đi qua mọi package, ghép từ các bước của kịch bản theo buổi.
 * Gom hàng giữa hai kho → nhập lên hub → tính giá → giỏ hàng → mã giảm giá → chốt đơn → trừ kho → phí ship cả đơn
 * → vận đơn giao thành công.
 */
public class OverviewScenario implements Scenario {

    @Override
    public String id() {
        return "overview";
    }

    @Override
    public String title() {
        return "Tổng quan: từ kho tới đơn hàng";
    }

    @Override
    public String session() {
        return "S02–S07";
    }

    @Override
    public String description() {
        return "Một lượt mua đi qua mọi package: transfer LAPTOP ×2 HCM → HN, replenish LAPTOP ×3 HN → hub, "
                + "calculateFinalPrice, Cart.add LAPTOP ×2 + PHONE ×1, nhập mã SALE10, toOrder(SALE10, thuế 8%), "
                + "decreaseStock trừ hub, ShippingFeeCalculator.feeFor tính phí ship cả đơn về Hà Nội, "
                + "rồi tạo Shipment và pickUp → markDelivered.";
    }

    @Override
    public void run(World world, TraceRecorder recorder) {
        // 1. Kho: gom LAPTOP về HN rồi nhập lên hub (hàng dự trữ → hàng sẵn bán)
        TransferScenario.transfer(world, recorder, new WarehouseTransferService(),
                WorldFactory.WH_HCM, WorldFactory.WH_HN, "LAPTOP", 2);
        ReplenishScenario.replenish(world, recorder, WorldFactory.WH_HN, "LAPTOP", 3);

        // 2. Catalog: giá cuối theo loại sản phẩm (đơn giá mà OrderLine sẽ chụp lại)
        Product laptop = world.product("LAPTOP");
        Product phone = world.product("PHONE");
        PricingTypesScenario.price(world, recorder, laptop);

        // 3. Giỏ hàng → đơn: Cart chỉ kiểm tồn kho, chưa trừ; chốt bằng mã giảm giá (domain.pricing)
        Cart cart = world.cart();
        Flow hubToCart = new Flow(WorldSnapshotter.HUB, WorldSnapshotter.CART);
        List<String> cartPanel = List.of(WorldSnapshotter.CART);
        recorder.run("Cart", "add", List.of("LAPTOP", "2"), hubToCart, cartPanel, () -> cart.add(laptop, 2));
        recorder.run("Cart", "add", List.of("PHONE", "1"), hubToCart, cartPanel, () -> cart.add(phone, 1));
        DiscountCode sale10 = PricingTypesScenario.createCode(world, recorder, "SALE10", new BigDecimal("0.10"));
        Order order = OrderSteps.checkout(world, recorder, sale10, new BigDecimal("0.08"));

        // 4. Trừ kho trên hub (all-or-nothing)
        OrderSteps.decreaseStock(world, recorder, order);

        // 5. Giao hàng: phí ship cả đơn (chọn vùng theo địa chỉ) → tạo vận đơn → lấy hàng → giao thành công
        Address address = new Address("1 Hàng Bài", "Hà Nội");
        BigDecimal fee = ShipmentSteps.fee(recorder, order, address);
        Shipment shipment = ShipmentSteps.create(world, recorder, order, address, fee);
        ShipmentSteps.step(recorder, shipment, "pickUp", List.of(), shipment::pickUp);
        ShipmentSteps.step(recorder, shipment, "markDelivered", List.of(), shipment::markDelivered);
    }
}
