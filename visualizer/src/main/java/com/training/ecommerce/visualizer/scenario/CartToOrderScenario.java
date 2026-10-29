package com.training.ecommerce.visualizer.scenario;

import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.order.Cart;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.visualizer.trace.Flow;
import com.training.ecommerce.visualizer.trace.TraceRecorder;
import com.training.ecommerce.visualizer.world.World;
import com.training.ecommerce.visualizer.world.WorldSnapshotter;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** S02/S04: giỏ hàng thêm, đổi số lượng, xoá rồi chốt thành Order. */
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
        return "Cart.add ×3, changeQuantity, remove rồi toOrder(giảm giá 10%, thuế 8%): ô cart pop-in, đổi số, pop-out; Order mới xuất hiện. Cart chỉ kiểm tồn kho, không trừ; kho giảm ở bước InventoryManager.decreaseStock cuối.";
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
        AtomicReference<Order> created = new AtomicReference<>();
        recorder.callFlows("Cart", "toOrder", List.of("0.10", "0.08"),
                List.of(new Flow(WorldSnapshotter.PRICING, WorldSnapshotter.CART),
                        new Flow(WorldSnapshotter.CART, WorldSnapshotter.ORDER)),
                List.of(WorldSnapshotter.PRICING, WorldSnapshotter.CART, WorldSnapshotter.ORDER), () -> {
                    Order order = cart.toOrder(new BigDecimal("0.10"), new BigDecimal("0.08"));
                    world.addOrder(order);
                    created.set(order);
                    return WorldSnapshotter.orderRef(order);
                });
        Order order = created.get();
        // Cart không trừ kho; kho chỉ giảm khi InventoryManager.decreaseStock(order)
        recorder.run("InventoryManager", "decreaseStock", List.of(WorldSnapshotter.orderRef(order)),
                new Flow(WorldSnapshotter.ORDER, WorldSnapshotter.HUB), List.of(WorldSnapshotter.HUB),
                () -> world.hub().decreaseStock(order));
    }
}
