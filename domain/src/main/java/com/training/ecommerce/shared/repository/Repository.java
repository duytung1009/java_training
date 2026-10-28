package com.training.ecommerce.shared.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    /**
     * Lưu lần lượt từng entity (wildcard: nhận cả collection của subtype).
     *
     * @throws IllegalArgumentException khi entities null, hoặc một entity không hợp lệ (xem {@link #save})
     */
    default void saveAll(Collection<? extends T> entities) {
        if (entities == null) {
            throw new IllegalArgumentException("entities không được null");
        }
        for (T entity : entities) {
            save(entity);
        }
    }

    /** S05: entity có id tương ứng; không có (kể cả id null) → {@code Optional.empty()}. */
    Optional<T> findById(ID id);

    /** @return mọi entity theo thứ tự thêm; list không sửa được */
    List<T> findAll();

    /** @return {@code true} nếu có entity bị xóa */
    boolean deleteById(ID id);

    int count();
}
