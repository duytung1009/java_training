package com.training.ecommerce.visualizer.world;

import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.ProductNotFoundException;
import com.training.ecommerce.domain.inventory.InventoryManager;
import com.training.ecommerce.domain.inventory.Warehouse;
import com.training.ecommerce.domain.order.Cart;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.shipping.ShippingZone;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thế giới của một lượt chạy: các object domain thật mà kịch bản thao tác, cộng với vài mục chỉ để hiển thị
 * (bảng giá, phí ship vừa tính). Mỗi lượt chạy dùng một World mới ({@link WorldFactory}).
 */
public class World {

    private final Map<String, Warehouse> warehouses;
    private final InventoryManager hub = new InventoryManager();
    private final Cart cart = new Cart();
    private final List<Order> orders = new CopyOnWriteArrayList<>();
    /** Warehouse không liệt kê được sản phẩm, nên snapshot phải biết trước danh sách productId cần hỏi. */
    private final List<String> productIds = new CopyOnWriteArrayList<>();
    private final Map<String, String> pricing = new LinkedHashMap<>();                 // guarded by this.pricing
    private final Map<ShippingZone, Map<String, String>> shippingFees = new EnumMap<>(ShippingZone.class);

    /**
     * @param warehouses các kho theo thứ tự hiển thị
     * @throws IllegalArgumentException nếu warehouses null hoặc rỗng
     */
    public World(Collection<Warehouse> warehouses) {
        if (warehouses == null || warehouses.isEmpty()) {
            throw new IllegalArgumentException("warehouses không được rỗng");
        }
        Map<String, Warehouse> byId = new LinkedHashMap<>();
        warehouses.forEach(w -> byId.put(w.getId(), w));
        this.warehouses = Collections.unmodifiableMap(byId);
        for (ShippingZone zone : ShippingZone.values()) {
            shippingFees.put(zone, new LinkedHashMap<>());
        }
    }

    /**
     * @throws IllegalArgumentException nếu id không có
     */
    public Warehouse warehouse(String id) {
        Warehouse warehouse = id == null ? null : warehouses.get(id);
        if (warehouse == null) {
            throw new IllegalArgumentException("Không có kho: " + id);
        }
        return warehouse;
    }

    public Collection<Warehouse> warehouses() {
        return warehouses.values();
    }

    public InventoryManager hub() {
        return hub;
    }

    public Cart cart() {
        return cart;
    }

    /**
     * @throws ProductNotFoundException nếu hub chưa có sản phẩm này
     */
    public Product product(String id) {
        return hub.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    /**
     * Thêm sản phẩm vào hub và ghi nhớ id để snapshot hỏi kho.
     *
     * @throws IllegalArgumentException nếu product null hoặc trùng id
     */
    public void registerProduct(Product product) {
        hub.addProduct(product);
        trackProductId(product.getId());
    }

    /**
     * Ghi nhớ một productId (vd. kho chứa hàng mà hub không bán) để snapshot hiển thị.
     *
     * @throws IllegalArgumentException nếu productId null
     */
    public void trackProductId(String productId) {
        if (productId == null) {
            throw new IllegalArgumentException("productId không được null");
        }
        if (!productIds.contains(productId)) {
            productIds.add(productId);
        }
    }

    public List<String> productIds() {
        return List.copyOf(productIds);
    }

    /**
     * @throws IllegalArgumentException nếu order null
     */
    public void addOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("order không được null");
        }
        orders.add(order);
    }

    public List<Order> orders() {
        return List.copyOf(orders);
    }

    /**
     * Ghi một mục hiển thị ở panel pricing (chỉ để xem, không phải state của domain).
     *
     * @throws IllegalArgumentException nếu key hoặc value null
     */
    public void putPricing(String key, String value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("key và value không được null");
        }
        synchronized (pricing) {
            pricing.put(key, value);
        }
    }

    public Map<String, String> pricing() {
        synchronized (pricing) {
            return new LinkedHashMap<>(pricing);
        }
    }

    /**
     * Ghi phí ship vừa tính cho một thành phố (chỉ để hiển thị ở ô zone).
     *
     * @throws IllegalArgumentException nếu tham số null
     */
    public void putShippingFee(ShippingZone zone, String city, String fee) {
        if (zone == null || city == null || fee == null) {
            throw new IllegalArgumentException("zone, city và fee không được null");
        }
        Map<String, String> fees = shippingFees.get(zone);
        synchronized (fees) {
            fees.put(city, fee);
        }
    }

    public Map<String, String> shippingFees(ShippingZone zone) {
        Map<String, String> fees = shippingFees.get(zone);
        synchronized (fees) {
            return new LinkedHashMap<>(fees);
        }
    }
}
