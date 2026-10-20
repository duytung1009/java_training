package com.training.ecommerce.shared.repository;

import java.util.Collection;
import java.util.List;

/**
 * Kho lưu trữ generic theo id.
 *
 * @param <T>  kiểu entity, phải có id
 * @param <ID> kiểu id
 */
public interface Repository<T extends Identifiable<ID>, ID> {

    /**
     * Thêm mới, hoặc ghi đè entity cùng id (upsert, giữ vị trí lần thêm đầu tiên).
     *
     * @return chính entity truyền vào
     * @throws IllegalArgumentException khi entity hoặc id của nó null
     */
    T save(T entity);

    /** Lưu lần lượt từng entity (wildcard: nhận cả collection của subtype). */
    void saveAll(Collection<? extends T> entities);

    /** @return entity có id tương ứng, hoặc {@code null} nếu không có (kể cả khi id null) */
    T findById(ID id);

    /** @return mọi entity theo thứ tự thêm; list không sửa được */
    List<T> findAll();

    /** @return {@code true} nếu có entity bị xóa */
    boolean deleteById(ID id);

    int count();
}
