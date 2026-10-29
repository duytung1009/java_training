package com.training.ecommerce.visualizer.scenario;

import com.training.ecommerce.domain.catalog.BundleProduct;
import com.training.ecommerce.domain.catalog.DigitalProduct;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.SubscriptionProduct;
import com.training.ecommerce.domain.pricing.DiscountCode;
import com.training.ecommerce.domain.pricing.PricingRule;
import com.training.ecommerce.visualizer.trace.TraceRecorder;
import com.training.ecommerce.visualizer.world.MoneyFormat;
import com.training.ecommerce.visualizer.world.World;
import com.training.ecommerce.visualizer.world.WorldSnapshotter;

import java.math.BigDecimal;
import java.util.List;

/** S03: đa hình calculateFinalPrice của 4 loại product, PricingRule.andThen/chain và DiscountCode. */
public class PricingTypesScenario implements Scenario {

    private static final List<String> PRICING_PANEL = List.of(WorldSnapshotter.PRICING);

    @Override
    public String id() {
        return "pricing-types";
    }

    @Override
    public String title() {
        return "Giá theo loại sản phẩm";
    }

    @Override
    public String session() {
        return "S03";
    }

    @Override
    public String description() {
        return "calculateFinalPrice cho Physical / Digital / Subscription / Bundle, rồi PricingRule.andThen, chain và DiscountCode: mỗi giá hiện ở trạm pricing.";
    }

    @Override
    public void run(World world, TraceRecorder recorder) {
        PhysicalProduct physical = new PhysicalProduct("LAPTOP-X", "Laptop", new BigDecimal("20000000"), 5,
                new BigDecimal("2.0"));
        DigitalProduct digital = new DigitalProduct("EBOOK", "Ebook", new BigDecimal("99000"), 100);
        SubscriptionProduct subscription = new SubscriptionProduct("NEWS", "Báo điện tử",
                new BigDecimal("100000"), 12, 100);
        BundleProduct bundle = new BundleProduct("COMBO", "Combo", 3, new BigDecimal("0.10"), physical, digital);

        price(world, recorder, physical, "PHY");
        price(world, recorder, digital, "DIG");
        price(world, recorder, subscription, "SUB");
        price(world, recorder, bundle, "BUN");

        BigDecimal tenPercent = new BigDecimal("0.10");
        BigDecimal fixedOff = new BigDecimal("500000");
        PricingRule percentThenAmount = PricingRule.percentOff(tenPercent).andThen(PricingRule.amountOff(fixedOff));
        recorder.call("PricingRule", "andThen", List.of("percentOff(10%)", "amountOff(500 000)", "20 000 000"),
                null, PRICING_PANEL, () -> {
                    BigDecimal result = percentThenAmount.apply(physical.calculateFinalPrice());
                    world.putPricing("THEN", "PricingRule", MoneyFormat.shortOf(result));
                    return MoneyFormat.full(result);
                });

        PricingRule chained = PricingRule.chain(List.of(PricingRule.percentOff(tenPercent),
                PricingRule.amountOff(fixedOff)));
        recorder.call("PricingRule", "chain", List.of("percentOff(10%)", "amountOff(500 000)", "99 000"),
                null, PRICING_PANEL, () -> {
                    BigDecimal result = chained.apply(digital.calculateFinalPrice());
                    world.putPricing("CHAIN", "PricingRule", MoneyFormat.shortOf(result));
                    return MoneyFormat.full(result);
                });

        // DiscountCode không override toString: giữ object qua holder, log trả chuỗi dễ đọc
        DiscountCode[] holder = new DiscountCode[1];
        recorder.call("DiscountCode", "new", List.of("SALE20", "0.20"), null, PRICING_PANEL, () -> {
            holder[0] = new DiscountCode("SALE20", new BigDecimal("0.20"));
            String rate = "-" + MoneyFormat.percent(holder[0].getRate());
            world.putPricing(holder[0].getCode(), "DiscountCode", rate);
            return holder[0].getCode() + " " + rate;
        });
        DiscountCode code = holder[0];
        recorder.call("PricingRule", "percentOff", List.of("SALE20", "20 000 000"), null, PRICING_PANEL, () -> {
            BigDecimal result = PricingRule.percentOff(code.getRate()).apply(physical.calculateFinalPrice());
            return MoneyFormat.full(result);
        });
    }

    private static void price(World world, TraceRecorder recorder, Product product, String key) {
        recorder.call(product.getClass().getSimpleName(), "calculateFinalPrice", List.of(product.getId()), null,
                PRICING_PANEL, () -> {
                    BigDecimal price = product.calculateFinalPrice();
                    world.putPricing(key, "Product", MoneyFormat.shortOf(price));
                    return MoneyFormat.full(price);
                });
    }
}
