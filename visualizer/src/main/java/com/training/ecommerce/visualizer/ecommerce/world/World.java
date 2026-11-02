package com.training.ecommerce.visualizer.ecommerce.world;

import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.ProductNotFoundException;
import com.training.ecommerce.domain.inventory.InventoryManager;
import com.training.ecommerce.domain.inventory.Warehouse;
import com.training.ecommerce.domain.order.Cart;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.visualizer.core.view.ItemView;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thế giới của một lượt chạy: các object domain thật mà kịch bản thao tác, cộng với vài mục chỉ để hiển thị
 * (object pricing đã tạo, kết quả hàm thuần như phí ship). Mỗi lượt chạy dùng một World mới ({@link WorldFactory}).
 */
public class World {

    private final Map<String, Warehouse> warehouses;
    private final InventoryManager hub = new InventoryManager();
    private final Cart cart = new Cart();
    private final List<Order> orders = new CopyOnWriteArrayList<>();
    /** Warehouse không liệt kê được sản phẩm, nên snapshot phải biết trước danh sách productId cần hỏi. */
    private final List<String> productIds = new CopyOnWriteArrayList<>();
    private final Map<String, RegisteredNode> registered = new LinkedHashMap<>();     // guarded by this.registered
    private final Map<String, Map<String, ItemView>> computed = new LinkedHashMap<>();  // guarded by this.computed

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
     * Thêm sản phẩm vào hub, ghi nhớ id để snapshot hỏi kho, và hiện chính object đó thành khối trong
     * {@code domain.catalog}: mọi Product của hub đều có mặt ở catalog ngay từ đầu, không đợi tới lúc tính giá.
     *
     * @throws IllegalArgumentException nếu product null hoặc trùng id
     */
    public void registerProduct(Product product) {
        hub.addProduct(product);
        trackProductId(product.getId());
        registerObject(WorldSnapshotter.CATALOG, WorldSnapshotter.productNodeId(product.getId()), product, null);
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
     * Đăng ký một object domain (Product, PricingRule, DiscountCode…) làm node con của một area.
     * Field của node được đọc từ chính object này lúc snapshot. Đăng ký lại cùng nodeId thì thay object.
     *
     * @param parentId id area cha, vd. "pricing", "catalog"
     * @param nodeId   id node, vd. "pricing:code:SALE20"
     * @param sub    chú thích ngắn của node, null nếu không có
     * @throws IllegalArgumentException nếu parentId, nodeId hoặc domainObject null
     */
    public void registerObject(String parentId, String nodeId, Object domainObject, String sub) {
        if (parentId == null || nodeId == null || domainObject == null) {
            throw new IllegalArgumentException("parentId, nodeId và domainObject không được null");
        }
        synchronized (registered) {
            registered.put(nodeId, new RegisteredNode(parentId, nodeId, domainObject, sub));
        }
    }

    /** Các object đã đăng ký, theo thứ tự đăng ký lần đầu. */
    public List<RegisteredNode> registeredObjects() {
        synchronized (registered) {
            return List.copyOf(registered.values());
        }
    }

    /**
     * Ghi một kết quả hàm thuần để hiển thị trên node (không phải state). Cùng key thì ghi đè.
     *
     * @throws IllegalArgumentException nếu nodeId hoặc item null
     */
    public void putComputed(String nodeId, ItemView item) {
        if (nodeId == null || item == null) {
            throw new IllegalArgumentException("nodeId và item không được null");
        }
        synchronized (computed) {
            computed.computeIfAbsent(nodeId, k -> new LinkedHashMap<>()).put(item.key(), item);
        }
    }

    /** Các ô computed của một node theo thứ tự ghi lần đầu; rỗng nếu chưa có. */
    public List<ItemView> computed(String nodeId) {
        synchronized (computed) {
            Map<String, ItemView> items = computed.get(nodeId);
            return items == null ? List.of() : List.copyOf(items.values());
        }
    }

    /**
     * Một object domain đăng ký dưới một area.
     *
     * @param parentId     id area cha
     * @param nodeId       id node
     * @param domainObject object thật để đọc field
     * @param sub          chú thích ngắn, có thể null
     */
    public record RegisteredNode(String parentId, String nodeId, Object domainObject, String sub) {
    }
}
