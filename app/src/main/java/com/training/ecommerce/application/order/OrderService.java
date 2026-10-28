package com.training.ecommerce.application.order;

import com.training.ecommerce.application.inventory.StockStore;
import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.ProductNotFoundException;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;
import com.training.ecommerce.shared.repository.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case đặt đơn và tra cứu đơn.
 *
 * <p>{@link #placeOrder} chạy trong một transaction: trừ kho và lưu đơn cùng thành công hoặc cùng rollback.
 * Với profile {@code dev} (JDBC) đây chính là thứ bảo đảm all-or-nothing của {@link StockStore}.
 * Profile {@code local} không có TransactionManager nên annotation không có tác dụng (kho in-memory tự khóa).
 */
@Service
public class OrderService {
    private final Repository<Product, String> products;
    private final Repository<Order, String> orders;
    private final StockStore stock;

    /**
     * @throws IllegalArgumentException khi một tham số null
     */
    public OrderService(Repository<Product, String> products, Repository<Order, String> orders, StockStore stock) {
        if (products == null || orders == null || stock == null) {
            throw new IllegalArgumentException("products, orders, stock không được null");
        }
        this.products = products;
        this.orders = orders;
        this.stock = stock;
    }

    /**
     * Tạo đơn từ command, trừ kho rồi lưu.
     *
     * @throws IllegalArgumentException khi command null
     * @throws ProductNotFoundException khi một productId không tồn tại
     * @throws OutOfStockException      khi không đủ hàng; kho và đơn không đổi
     */
    @Transactional
    public Order placeOrder(PlaceOrderCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command không được null");
        }
        Order order = new Order(command.discountRate(), command.taxRate());
        for (PlaceOrderCommand.Line line : command.lines()) {
            Product product = products.findById(line.productId())
                    .orElseThrow(() -> new ProductNotFoundException(line.productId()));
            order.addLine(new OrderLine(product, line.quantity()));
        }
        stock.decreaseStock(order);
        return orders.save(order);
    }

    /**
     * @throws OrderNotFoundException khi không có đơn với id này (kể cả id null)
     */
    @Transactional(readOnly = true)
    public Order findById(String id) {
        return orders.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orders.findAll();
    }
}
