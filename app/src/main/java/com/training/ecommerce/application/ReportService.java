package com.training.ecommerce.application;

import com.training.ecommerce.domain.catalog.Category;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Báo cáo bán hàng. Pure: không sửa list hay Order đầu vào.
 */
public class ReportService {

    /**
     * @return category → Σ lineTotal (trước giảm giá/thuế của đơn), scale 2; category không có doanh thu thì không có key
     * @throws IllegalArgumentException khi orders null
     */
    public Map<Category, BigDecimal> revenueByCategory(List<Order> orders) {
        checkNotNull(orders, "orders");
        return lines(orders).collect(Collectors.groupingBy(
                line -> line.getProduct().getCategory(),
                () -> new EnumMap<>(Category.class),          // key là enum: EnumMap, thứ tự theo khai báo
                Collectors.reducing(BigDecimal.ZERO, OrderLine::lineTotal, BigDecimal::add)));
    }

    /**
     * @return tối đa n product bán nhiều nhất theo tổng số lượng (giảm dần); bằng nhau thì id tăng dần
     * @throws IllegalArgumentException khi orders null hoặc n &lt;= 0
     */
    public List<Product> topSellingProducts(List<Order> orders, int n) {
        checkNotNull(orders, "orders");
        if (n <= 0) {
            throw new IllegalArgumentException("n phải > 0");
        }
        // product → tổng số lượng; cùng id (equals theo id) thì cộng chung nhờ merge Integer::sum
        Map<Product, Integer> quantityByProduct = lines(orders).collect(
                Collectors.toMap(OrderLine::getProduct, OrderLine::getQuantity, Integer::sum));
        return quantityByProduct.entrySet().stream()
                .sorted(Map.Entry.<Product, Integer>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().getId()))   // bằng nhau: id tăng dần (D3)
                .limit(n)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * @return true → đơn có finalAmount() ≥ threshold, false → còn lại; luôn có đủ 2 key, giữ thứ tự đơn
     * @throws IllegalArgumentException khi orders hoặc threshold null
     */
    public Map<Boolean, List<Order>> partitionByFinalAmount(List<Order> orders, BigDecimal threshold) {
        checkNotNull(orders, "orders");
        checkNotNull(threshold, "threshold");
        // partitioningBy luôn có đủ 2 key; so BigDecimal bằng compareTo, không dùng equals (D5)
        return orders.stream().collect(Collectors.partitioningBy(
                order -> order.finalAmount().compareTo(threshold) >= 0));
    }

    private static Stream<OrderLine> lines(List<Order> orders) {
        return orders.stream().flatMap(order -> Arrays.stream(order.getLines()));
    }

    private static void checkNotNull(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " không được null");
        }
    }
}
