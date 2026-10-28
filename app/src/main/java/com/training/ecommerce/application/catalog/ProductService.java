package com.training.ecommerce.application.catalog;

import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.ProductNotFoundException;
import com.training.ecommerce.shared.repository.Repository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD mỏng cho catalog, đi qua {@link Repository} nên chạy được với bất kỳ implementation nào.
 * Ghi ({@code save}, {@code delete}) chạy trong transaction: lưu bundle gồm nhiều câu lệnh (upsert, xóa và ghi lại item)
 * nên phải all-or-nothing ở profile dev.
 */
@Service
public class ProductService {
    private final Repository<Product, String> products;

    /**
     * @throws IllegalArgumentException khi products null
     */
    public ProductService(Repository<Product, String> products) {
        if (products == null) {
            throw new IllegalArgumentException("products không được null");
        }
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return products.findAll();
    }

    /**
     * @throws ProductNotFoundException khi không có product với id này (kể cả id null)
     */
    @Transactional(readOnly = true)
    public Product findById(String id) {
        return products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    /**
     * Thêm mới hoặc ghi đè product cùng id.
     *
     * @throws IllegalArgumentException khi product null
     */
    @Transactional
    public Product save(Product product) {
        return products.save(product);
    }

    /**
     * Cập nhật product đã có. Id trên path phải trùng id của product.
     *
     * @throws IllegalArgumentException khi product null hoặc id khác nhau
     * @throws ProductNotFoundException khi chưa có product với id này
     */
    @Transactional
    public Product update(String id, Product product) {
        if (product == null) {
            throw new IllegalArgumentException("product không được null");
        }
        if (!product.getId().equals(id)) {
            throw new IllegalArgumentException("id trên path (" + id + ") khác id của product (" + product.getId() + ")");
        }
        findById(id);
        return products.save(product);
    }

    /**
     * @throws ProductNotFoundException khi không có product với id này
     * @throws ProductInUseException    khi product đang được bundle hoặc đơn hàng tham chiếu
     */
    @Transactional
    public void delete(String id) {
        boolean deleted;
        try {
            deleted = products.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new ProductInUseException(id, e);
        }
        if (!deleted) {
            throw new ProductNotFoundException(id);
        }
    }
}
