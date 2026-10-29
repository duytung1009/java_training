package com.training.ecommerce.visualizer.world;

import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.domain.inventory.Warehouse;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;
import com.training.ecommerce.domain.pricing.DiscountCode;
import com.training.ecommerce.domain.pricing.PricingRule;
import com.training.ecommerce.domain.shipping.Shipment;
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

    public static final String INVENTORY = "inventory";
    public static final String ORDER_AREA = "order";
    public static final String CATALOG = "catalog";
    public static final String HUB = "hub";
    public static final String CART = "cart";
    public static final String ORDERS = "orders";
    public static final String PRICING = "pricing";
    public static final String SHIPPING = "shipping";
    public static final String SHIPMENTS = "shipments";

    public static String warehousePanelId(String warehouseId) {
        return "warehouse:" + warehouseId;
    }

    public static String zoneNodeId(ShippingZone zone) {
        return "zone:" + zone.name();
    }

    /** Node id của một order, vd. "order:a1f3" (con của {@link #ORDERS}). */
    public static String productNodeId(String productId) {
        return "catalog:product:" + productId;
    }

    /** Node id của một vận đơn, vd. "shipping:shipment:SHP-1A2B3C4D" (con của {@link #SHIPMENTS}). */
    public static String shipmentNodeId(Shipment shipment) {
        return "shipping:shipment:" + shipment.getTrackingCode();
    }

    public static String orderNodeId(Order order) {
        return "order:" + shortId(order);
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
        List<NodeView> nodes = new ArrayList<>();

        nodes.add(new NodeView(INVENTORY, NodeView.AREA, "domain.inventory", "domain.inventory",
                "Warehouse · InventoryManager", null, List.of()));
        for (Warehouse warehouse : world.warehouses()) {
            Map<String, Object> quantities = new LinkedHashMap<>();
            for (String productId : world.productIds()) {
                int quantity = warehouse.stockOf(productId);
                if (quantity > 0) {
                    quantities.put(productId, quantity);
                }
            }
            objects.put("Warehouse " + warehouse.getId(), quantities);
            // Warehouse chỉ giữ Map<String, Integer>: mỗi ô là một entry productId → qty, không phải object Product
            nodes.add(new NodeView(warehousePanelId(warehouse.getId()), NodeView.OBJECT, "Warehouse",
                    "Warehouse " + warehouse.getId(), "Map<productId, qty>", INVENTORY,
                    countItems(quantities, "Map.Entry")));
        }

        Map<String, Object> stocks = new LinkedHashMap<>();
        for (String productId : world.productIds()) {
            world.hub().findById(productId).ifPresent(product -> {
                int stock = product.getStock();
                stocks.put(product.getId(), stock);
                objects.put("Product " + product.getId(), Map.of("stock", stock));
            });
        }
        nodes.add(new NodeView(HUB, NodeView.OBJECT, "InventoryManager", "InventoryManager",
                "catalog: Product.stock", INVENTORY, countItems(stocks, "Product")));

        nodes.add(new NodeView(ORDER_AREA, NodeView.AREA, "domain.order", "domain.order",
                "Cart · Order · OrderLine", null, List.of()));
        Map<String, Object> cartItems = new LinkedHashMap<>();
        for (Map.Entry<Product, Integer> entry : world.cart().getItems().entrySet()) {
            cartItems.put(entry.getKey().getId(), entry.getValue());
        }
        objects.put("Cart", cartItems);
        nodes.add(new NodeView(CART, NodeView.OBJECT, "Cart", "Cart", "Map<Product, qty>", ORDER_AREA,
                countItems(cartItems, "Product")));

        nodes.add(new NodeView(ORDERS, NodeView.COLLECTION, "List<Order>", "List<Order> · World", "đơn đã tạo",
                ORDER_AREA, List.of()));
        for (Order order : world.orders()) {
            String shortId = shortId(order);
            objects.put("Order " + shortId, orderFields(order));
            nodes.add(new NodeView(orderNodeId(order), NodeView.OBJECT, "Order", "Order " + shortId,
                    "-" + MoneyFormat.percent(order.getDiscountRate()) + " +" + MoneyFormat.percent(order.getTaxRate())
                            + " = " + MoneyFormat.shortOf(order.finalAmount()), ORDERS, orderLineItems(order)));
        }

        nodes.add(new NodeView(CATALOG, NodeView.AREA, "domain.catalog", "domain.catalog",
                "Product và các lớp con", null, List.of()));
        addRegistered(world, CATALOG, nodes, objects);

        nodes.add(new NodeView(PRICING, NodeView.AREA, "domain.pricing", "domain.pricing",
                "PricingRule · DiscountCode", null, List.of()));
        addRegistered(world, PRICING, nodes, objects);

        nodes.add(new NodeView(SHIPPING, NodeView.AREA, "domain.shipping", "domain.shipping",
                "Shipment · ShippingZone · Address", null, List.of()));
        // Phí ship là hàm thuần (computed), không đưa vào objects nên không sinh diff
        for (ShippingZone zone : ShippingZone.values()) {
            List<ItemView> items = new ArrayList<>();
            items.add(new ItemView("baseFee", "baseFee", MoneyFormat.shortOf(zone.getBaseFee()), "BigDecimal"));
            items.addAll(world.computed(zoneNodeId(zone)));
            nodes.add(new NodeView(zoneNodeId(zone), NodeView.OBJECT, "ShippingZone", "ShippingZone." + zone.name(),
                    null, SHIPPING, items));
        }
        nodes.add(new NodeView(SHIPMENTS, NodeView.COLLECTION, "List<Shipment>", "List<Shipment> · World",
                "vận đơn đã tạo", SHIPPING, List.of()));
        addRegistered(world, SHIPMENTS, nodes, objects);
        return new WorldSnapshot(new WorldView(List.copyOf(nodes)), objects);
    }

    /** Thêm các node đã đăng ký dưới area {@code parentId}, theo thứ tự đăng ký. */
    private static void addRegistered(World world, String parentId, List<NodeView> nodes,
                                       Map<String, Map<String, Object>> objects) {
        for (World.RegisteredNode entry : world.registeredObjects()) {
            if (entry.parentId().equals(parentId)) {
                nodes.add(registeredNode(world, entry, objects));
            }
        }
    }

    /** Node con của một area: field đọc từ object thật, computed lấy từ world. */
    private static NodeView registeredNode(World world, World.RegisteredNode entry,
                                           Map<String, Map<String, Object>> objects) {
        Object object = entry.domainObject();
        String name = entry.nodeId().substring(entry.nodeId().lastIndexOf(':') + 1);
        List<ItemView> items = new ArrayList<>();
        String cls = object.getClass().getSimpleName();
        String title;
        switch (object) {
            case Product product -> {
                title = cls + " " + product.getId();
                items.add(new ItemView("basePrice", "basePrice", MoneyFormat.shortOf(product.getBasePrice()),
                        "BigDecimal"));
                objects.put(title, Map.of("basePrice", MoneyFormat.full(product.getBasePrice())));
            }
            case DiscountCode code -> {
                title = "DiscountCode " + code.getCode();
                String rate = MoneyFormat.percent(code.getRate());
                items.add(new ItemView("code", "code", code.getCode(), "String"));
                items.add(new ItemView("rate", "rate", rate, "BigDecimal"));
                objects.put(title, Map.of("code", code.getCode(), "rate", rate));
            }
            case Shipment shipment -> {
                title = "Shipment " + shipment.getTrackingCode();
                String status = shipment.getStatus().name();
                items.add(new ItemView("status", "status", status, "ShipmentStatus"));
                items.add(new ItemView("failedAttempts", "failed", shipment.getFailedAttempts(), "int"));
                items.add(new ItemView("fee", "fee", MoneyFormat.shortOf(shipment.getFee()), "BigDecimal"));
                items.add(new ItemView("trackingCode", "tracking", shipment.getTrackingCode(), "String"));
                Map<String, Object> fields = new LinkedHashMap<>();
                fields.put("status", status);
                fields.put("failedAttempts", shipment.getFailedAttempts());
                fields.put("fee", MoneyFormat.full(shipment.getFee()));
                fields.put("history", shipment.history().size() + " event");
                objects.put(title, fields);
            }
            case PricingRule rule -> {
                cls = "PricingRule";   // lambda/class ẩn danh: dùng tên interface cho dễ đọc
                title = "PricingRule " + name;
            }
            default -> title = cls + " " + name;
        }
        items.addAll(world.computed(entry.nodeId()));
        return new NodeView(entry.nodeId(), NodeView.OBJECT, cls, title, entry.sub(), entry.parentId(), items);
    }

    private static List<ItemView> orderLineItems(Order order) {
        List<ItemView> items = new ArrayList<>();
        OrderLine[] lines = order.getLines();
        for (int i = 0; i < lines.length; i++) {
            String productId = lines[i].getProduct().getId();
            items.add(new ItemView(i + ":" + productId, label(productId) + " ×" + lines[i].getQuantity(),
                    MoneyFormat.shortOf(lines[i].getUnitPrice()), "OrderLine"));
        }
        return items;
    }

    /** Ref dễ đọc của một order, vd. "Order a1f3". */
    public static String orderRef(Order order) {
        return "Order " + shortId(order);
    }

    /** Nhãn địa chỉ trong call log, vd. "Address(Hà Nội)". */
    public static String addressRef(Address address) {
        return "Address(" + address.getCity() + ")";
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

    private static List<ItemView> countItems(Map<String, Object> quantities, String type) {
        List<ItemView> items = new ArrayList<>();
        quantities.forEach((productId, quantity) ->
                items.add(new ItemView(productId, label(productId), quantity, type)));
        return items;
    }
}
