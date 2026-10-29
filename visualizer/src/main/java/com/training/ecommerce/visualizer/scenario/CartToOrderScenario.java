package com.training.ecommerce.visualizer.scenario;

import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.order.Cart;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.pricing.DiscountCode;
import com.training.ecommerce.visualizer.trace.Flow;
import com.training.ecommerce.visualizer.trace.TraceRecorder;
import com.training.ecommerce.visualizer.world.World;
import com.training.ecommerce.visualizer.world.WorldSnapshotter;

import java.math.BigDecimal;
import java.util.List;

/** S02/S04: giỏ hàng thêm, đổi số lượng, xoá, nhập mã giảm giá rồi chốt thành Order. */
public class CartToOrderScenario implements Scenario {

    @Override
    public String id() {
        return "cart-to-order";
    }

    @Override
    public String title() {
        return "Giỏ hàng → Đơn hàng";
    }

    @Override
    public String session() {
        return "S02";
    }

    @Override
    public String description() {
        return "Cart.add ×3, changeQuantity, remove, nhập mã SALE10 (giảm 10%) rồi toOrder(SALE10, thuế 8%): ô cart pop-in, đổi số, pop-out; mã hiện ở domain.pricing; Order mới xuất hiện. Cart chỉ kiểm tồn kho, không trừ; kho giảm ở bước InventoryManager.decreaseStock cuối.";
    }

    @Override
    public void run(World world, TraceRecorder recorder) {
        Cart cart = world.cart();
        Product laptop = world.product("LAPTOP");
        Product phone = world.product("PHONE");
        Product book = world.product("BOOK");
        Flow hubToCart = new Flow(WorldSnapshotter.HUB, WorldSnapshotter.CART);
        List<String> cartPanel = List.of(WorldSnapshotter.CART);

        recorder.run("Cart", "add", List.of("LAPTOP", "1"), hubToCart, cartPanel, () -> cart.add(laptop, 1));
        recorder.run("Cart", "add", List.of("PHONE", "2"), hubToCart, cartPanel, () -> cart.add(phone, 2));
        recorder.run("Cart", "add", List.of("BOOK", "3"), hubToCart, cartPanel, () -> cart.add(book, 3));
        recorder.run("Cart", "changeQuantity", List.of("PHONE", "1"), null, cartPanel,
                () -> cart.changeQuantity(phone, 1));
        recorder.run("Cart", "remove", List.of("BOOK"), null, cartPanel, () -> cart.remove(book));
        // Nhập mã giảm giá rồi chốt giỏ bằng chính mã đó: Order chỉ giữ tỉ lệ 10%, không giữ mã
        DiscountCode sale10 = PricingTypesScenario.createCode(world, recorder, "SALE10", new BigDecimal("0.10"));
        Order order = OrderSteps.checkout(world, recorder, sale10, new BigDecimal("0.08"));
        // Cart không trừ kho; kho chỉ giảm khi InventoryManager.decreaseStock(order)
        OrderSteps.decreaseStock(world, recorder, order);
    }
}
