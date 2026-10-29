package com.training.ecommerce.visualizer.scenario;

import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.domain.shipping.ShippingZone;
import com.training.ecommerce.visualizer.trace.Flow;
import com.training.ecommerce.visualizer.trace.TraceRecorder;
import com.training.ecommerce.visualizer.world.MoneyFormat;
import com.training.ecommerce.visualizer.world.World;
import com.training.ecommerce.visualizer.world.WorldSnapshotter;

import java.math.BigDecimal;
import java.util.List;

/** S02/S03: chọn vùng giao hàng theo địa chỉ rồi tính phí theo cân nặng. */
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
        return "ShippingZone.of(address) + feeFor(1.5 kg) cho Hà Nội (INNER_CITY) và Đà Nẵng (OTHER_CITY): ô phí hiện ở đúng vùng.";
    }

    @Override
    public void run(World world, TraceRecorder recorder) {
        BigDecimal weight = new BigDecimal("1.5");
        ship(world, recorder, new Address("1 Hàng Bài", "Hà Nội"), weight);
        ship(world, recorder, new Address("5 Lê Lợi", "Đà Nẵng"), weight);
    }

    private static void ship(World world, TraceRecorder recorder, Address address, BigDecimal weight) {
        String addressText = "Address(" + address.getCity() + ")";
        ShippingZone zone = recorder.call("ShippingZone", "of", List.of(addressText), null, List.of(),
                () -> ShippingZone.of(address));
        // Hàm thuần nên changes rỗng; phí được ghi vào world chỉ để ô zone hiện ra
        recorder.call("ShippingZone", "feeFor", List.of(addressText, weight.toPlainString()),
                new Flow(WorldSnapshotter.ORDER, WorldSnapshotter.zonePanelId(zone)),
                List.of(WorldSnapshotter.zonePanelId(zone)), () -> {
                    BigDecimal fee = zone.feeFor(weight);
                    world.putShippingFee(zone, address.getCity(), MoneyFormat.shortOf(fee));
                    return zone.name() + " · " + MoneyFormat.full(fee);
                });
    }
}
