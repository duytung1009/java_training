package com.training.ecommerce.visualizer.scenario;

import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.domain.shipping.Shippable;
import com.training.ecommerce.domain.shipping.ShippingZone;
import com.training.ecommerce.visualizer.trace.Flow;
import com.training.ecommerce.visualizer.trace.TraceRecorder;
import com.training.ecommerce.visualizer.world.MoneyFormat;
import com.training.ecommerce.visualizer.world.World;
import com.training.ecommerce.visualizer.world.WorldSnapshotter;

import java.math.BigDecimal;
import java.util.List;

/** S02/S03: sản phẩm vật lý tính phí giao theo vùng của địa chỉ và cân nặng của chính nó. */
public class ShippingFeeScenario implements Scenario {

    @Override
    public String id() {
        return "shipping-fee";
    }

    @Override
    public String title() {
        return "Phí vận chuyển theo vùng";
    }

    @Override
    public String session() {
        return "S02";
    }

    @Override
    public String description() {
        return "PhysicalProduct.calculateShipping(address) = ShippingZone.of(address).feeFor(cân nặng): LAPTOP (2 kg) "
                + "giao Hà Nội (INNER_CITY), PHONE (0.4 kg) giao Đà Nẵng (OTHER_CITY); ô phí hiện ở đúng vùng.";
    }

    @Override
    public void run(World world, TraceRecorder recorder) {
        ship(world, recorder, "LAPTOP", new Address("1 Hàng Bài", "Hà Nội"));
        ship(world, recorder, "PHONE", new Address("5 Lê Lợi", "Đà Nẵng"));
    }

    private static void ship(World world, TraceRecorder recorder, String productId, Address address) {
        String addressText = "Address(" + address.getCity() + ")";
        ShippingZone zone = recorder.call("ShippingZone", "of", List.of(addressText), null, List.of(),
                () -> ShippingZone.of(address));
        // Product (nằm trong hub) tự tính phí qua Shippable; hàm thuần nên changes rỗng,
        // phí được ghi vào world chỉ để ô zone hiện ra
        Shippable product = (Shippable) world.product(productId);
        recorder.call("PhysicalProduct", "calculateShipping", List.of(productId, addressText),
                new Flow(WorldSnapshotter.HUB, WorldSnapshotter.zonePanelId(zone)),
                List.of(WorldSnapshotter.HUB, WorldSnapshotter.zonePanelId(zone)), () -> {
                    BigDecimal fee = product.calculateShipping(address);
                    world.putShippingFee(zone, address.getCity(), MoneyFormat.shortOf(fee));
                    return zone.name() + " · " + MoneyFormat.full(fee);
                });
    }
}
