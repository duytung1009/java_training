package com.training.ecommerce.visualizer.world;

import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.inventory.Warehouse;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.shipping.ShippingZone;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Chụp trạng thái {@link World}, viết tay cho từng loại object (không dùng reflection) để kiểm soát
 * field nào được hiện ra. Thread-safe với World: chỉ đọc qua method đã synchronized của domain.
 */
@Component
public class WorldSnapshotter {

    public static final String HUB = "hub";
    public static final String CART = "cart";
    public static final String PRICING = "pricing";
    public static final String ORDER = "order";

    public static String warehousePanelId(String warehouseId) {
        return "warehouse:" + warehouseId;
    }

    public static String zonePanelId(ShippingZone zone) {
        return "zone:" + zone.name();
    }

    /**
     * Chụp toàn bộ thế giới.
     *
     * @throws IllegalArgumentException nếu world null
     */
    public WorldSnapshot snapshot(World world) {
        if (world == null) {
            throw new IllegalArgumentException("world không được null");
        }
        Map<String, Map<String, Object>> objects = new LinkedHashMap<>();
        List<PanelView> panels = new ArrayList<>();

        for (Warehouse warehouse : world.warehouses()) {
            Map<String, Object> quantities = new LinkedHashMap<>();
            for (String productId : world.productIds()) {
                int quantity = warehouse.stockOf(productId);
                if (quantity > 0) {
                    quantities.put(productId, quantity);
                }
            }
            objects.put("Warehouse " + warehouse.getId(), quantities);
            panels.add(new PanelView(warehousePanelId(warehouse.getId()), "warehouse",
                    "Warehouse " + warehouse.getId(), countItems(quantities)));
        }

        Map<String, Object> stocks = new LinkedHashMap<>();
        for (String productId : world.productIds()) {
            world.hub().findById(productId).ifPresent(product -> {
                int stock = product.getStock();
                stocks.put(product.getId(), stock);
                objects.put("Product " + product.getId(), Map.of("stock", stock));
            });
        }
        panels.add(new PanelView(HUB, "hub", "InventoryManager", countItems(stocks)));

        Map<String, Object> cartItems = new LinkedHashMap<>();
        for (Map.Entry<Product, Integer> entry : world.cart().getItems().entrySet()) {
            cartItems.put(entry.getKey().getId(), entry.getValue());
        }
        objects.put("Cart", cartItems);
        panels.add(new PanelView(CART, "cart", "Cart", countItems(cartItems)));

        List<ItemView> pricingItems = new ArrayList<>();
        world.pricing().forEach((key, value) -> pricingItems.add(new ItemView(key, key, value)));
        panels.add(new PanelView(PRICING, "pricing", "Pricing", pricingItems));

        List<ItemView> orderItems = new ArrayList<>();
        for (Order order : world.orders()) {
            String shortId = shortId(order);
            objects.put("Order " + shortId, orderFields(order));
            orderItems.add(new ItemView(shortId, shortId, MoneyFormat.shortOf(order.finalAmount())));
        }
        panels.add(new PanelView(ORDER, "order", "Order", orderItems));

        // Phí ship chỉ để hiển thị (hàm thuần), không đưa vào objects nên không sinh diff
        for (ShippingZone zone : ShippingZone.values()) {
            List<ItemView> feeItems = new ArrayList<>();
            world.shippingFees(zone).forEach((city, fee) -> feeItems.add(new ItemView(city, city, fee)));
            panels.add(new PanelView(zonePanelId(zone), "zone", zone.name(), feeItems));
        }
        return new WorldSnapshot(new WorldView(List.copyOf(panels)), objects);
    }

    /** Ref dễ đọc của một order, vd. "Order a1f3". */
    public static String orderRef(Order order) {
        return "Order " + shortId(order);
    }

    /** Nhãn 3 chữ cái của một productId. */
    public static String label(String productId) {
        return switch (productId) {
            case "LAPTOP" -> "LAP";
            case "PHONE" -> "PHO";
            case "HEADPHONE" -> "HDP";
            case "BOOK" -> "BOK";
            default -> productId.substring(0, Math.min(3, productId.length())).toUpperCase(Locale.ROOT);
        };
    }

    private static String shortId(Order order) {
        String id = order.getId();
        return id.substring(0, Math.min(4, id.length()));
    }

    private static Map<String, Object> orderFields(Order order) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("lines", order.getLines().length);
        fields.put("subtotal", MoneyFormat.full(order.subtotal()));
        fields.put("discount", MoneyFormat.full(order.discountAmount()));
        fields.put("tax", MoneyFormat.full(order.taxAmount()));
        fields.put("final", MoneyFormat.full(order.finalAmount()));
        return fields;
    }

    private static List<ItemView> countItems(Map<String, Object> quantities) {
        List<ItemView> items = new ArrayList<>();
        quantities.forEach((productId, quantity) -> items.add(new ItemView(productId, label(productId), quantity)));
        return items;
    }
}
