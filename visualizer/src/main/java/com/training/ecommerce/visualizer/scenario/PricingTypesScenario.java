package com.training.ecommerce.visualizer.scenario;

import com.training.ecommerce.domain.catalog.BundleProduct;
import com.training.ecommerce.domain.catalog.DigitalProduct;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.SubscriptionProduct;
import com.training.ecommerce.domain.pricing.DiscountCode;
import com.training.ecommerce.domain.pricing.PricingRule;
import com.training.ecommerce.visualizer.trace.TraceRecorder;
import com.training.ecommerce.visualizer.world.ItemView;
import com.training.ecommerce.visualizer.world.MoneyFormat;
import com.training.ecommerce.visualizer.world.World;
import com.training.ecommerce.visualizer.world.WorldSnapshotter;

import java.math.BigDecimal;
import java.util.List;

/** S03: đa hình calculateFinalPrice của 4 loại product, PricingRule.andThen/chain và DiscountCode. */
public class PricingTypesScenario implements Scenario {

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
        return "calculateFinalPrice cho Physical / Digital / Subscription / Bundle, rồi PricingRule.andThen, chain và DiscountCode: product hiện dưới domain.catalog, rule và code dưới domain.pricing.";
    }

    @Override
    public void run(World world, TraceRecorder recorder) {
        PhysicalProduct physical = new PhysicalProduct("LAPTOP-X", "Laptop", new BigDecimal("20000000"), 5,
                new BigDecimal("2.0"));
        DigitalProduct digital = new DigitalProduct("EBOOK", "Ebook", new BigDecimal("99000"), 100);
        SubscriptionProduct subscription = new SubscriptionProduct("NEWS", "Báo điện tử",
                new BigDecimal("100000"), 12, 100);
        BundleProduct bundle = new BundleProduct("COMBO", "Combo", 3, new BigDecimal("0.10"), physical, digital);

        price(world, recorder, physical);
        price(world, recorder, digital);
        price(world, recorder, subscription);
        price(world, recorder, bundle);

        BigDecimal tenPercent = new BigDecimal("0.10");
        BigDecimal fixedOff = new BigDecimal("500000");
        PricingRule percentThenAmount = PricingRule.percentOff(tenPercent).andThen(PricingRule.amountOff(fixedOff));
        recorder.call("PricingRule", "andThen", List.of("percentOff(10%)", "amountOff(500 000)", "20 000 000"),
                null, pricingPanels("pricing:rule:then"), () -> {
                    BigDecimal result = percentThenAmount.apply(physical.calculateFinalPrice());
                    showRule(world, "pricing:rule:then", percentThenAmount, "percentOff(10%)→amountOff(500k)",
                            physical.calculateFinalPrice(), result);
                    return MoneyFormat.full(result);
                });

        PricingRule chained = PricingRule.chain(List.of(PricingRule.percentOff(tenPercent),
                PricingRule.amountOff(fixedOff)));
        recorder.call("PricingRule", "chain", List.of("percentOff(10%)", "amountOff(500 000)", "99 000"),
                null, pricingPanels("pricing:rule:chain"), () -> {
                    BigDecimal result = chained.apply(digital.calculateFinalPrice());
                    showRule(world, "pricing:rule:chain", chained, "chain[percentOff(10%), amountOff(500k)]",
                            digital.calculateFinalPrice(), result);
                    return MoneyFormat.full(result);
                });

        // DiscountCode không override toString: giữ object qua holder, log trả chuỗi dễ đọc
        DiscountCode[] holder = new DiscountCode[1];
        recorder.call("DiscountCode", "new", List.of("SALE20", "0.20"), null, pricingPanels("pricing:code:SALE20"),
                () -> {
                    holder[0] = new DiscountCode("SALE20", new BigDecimal("0.20"));
                    world.registerObject(WorldSnapshotter.PRICING, "pricing:code:" + holder[0].getCode(), holder[0], null);
                    return holder[0].getCode() + " -" + MoneyFormat.percent(holder[0].getRate());
                });
        DiscountCode code = holder[0];
        recorder.call("PricingRule", "percentOff", List.of("SALE20", "20 000 000"), null,
                pricingPanels("pricing:rule:sale20"), () -> {
                    PricingRule rule = PricingRule.percentOff(code.getRate());
                    BigDecimal base = physical.calculateFinalPrice();
                    BigDecimal result = rule.apply(base);
                    showRule(world, "pricing:rule:sale20", rule,
                            "percentOff(" + MoneyFormat.percent(code.getRate()) + ")", base, result);
                    return MoneyFormat.full(result);
                });
    }

    private static List<String> pricingPanels(String childId) {
        return List.of(WorldSnapshotter.PRICING, childId);
    }

    /** Đăng ký node rule và ghi kết quả apply (hàm thuần, computed). */
    private static void showRule(World world, String nodeId, PricingRule rule, String sub, BigDecimal input,
                                 BigDecimal result) {
        world.registerObject(WorldSnapshotter.PRICING, nodeId, rule, sub);
        String label = "apply(" + MoneyFormat.shortOf(input) + ")";
        world.putComputed(nodeId, new ItemView("apply", label, MoneyFormat.shortOf(result), "BigDecimal", true));
    }

    private static void price(World world, TraceRecorder recorder, Product product) {
        String nodeId = "catalog:product:" + product.getId();
        recorder.call(product.getClass().getSimpleName(), "calculateFinalPrice", List.of(product.getId()), null,
                List.of(WorldSnapshotter.CATALOG, nodeId), () -> {
                    BigDecimal price = product.calculateFinalPrice();
                    world.registerObject(WorldSnapshotter.CATALOG, nodeId, product, null);
                    world.putComputed(nodeId, new ItemView("finalPrice", "= finalPrice", MoneyFormat.shortOf(price),
                            "BigDecimal", true));
                    return MoneyFormat.full(price);
                });
    }
}
