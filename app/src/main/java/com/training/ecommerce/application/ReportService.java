package com.training.ecommerce.application;

import com.training.ecommerce.domain.catalog.Category;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.order.Order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Báo cáo bán hàng. Pure: không sửa list hay Order đầu vào.
 */
public class ReportService {

    /**
     * @return category → Σ lineTotal (trước giảm giá/thuế của đơn), scale 2; category không có doanh thu thì không có key
     * @throws IllegalArgumentException khi orders null
     */
    public Map<Category, BigDecimal> revenueByCategory(List<Order> orders) {
        throw new UnsupportedOperationException("TODO S05");
    }

    /**
     * @return tối đa n product bán nhiều nhất theo tổng số lượng (giảm dần); bằng nhau thì id tăng dần
     * @throws IllegalArgumentException khi orders null hoặc n &lt;= 0
     */
    public List<Product> topSellingProducts(List<Order> orders, int n) {
        throw new UnsupportedOperationException("TODO S05");
    }

    /**
     * @return true → đơn có finalAmount() ≥ threshold, false → còn lại; luôn có đủ 2 key, giữ thứ tự đơn
     * @throws IllegalArgumentException khi orders hoặc threshold null
     */
    public Map<Boolean, List<Order>> partitionByFinalAmount(List<Order> orders, BigDecimal threshold) {
        throw new UnsupportedOperationException("TODO S05");
    }
}
