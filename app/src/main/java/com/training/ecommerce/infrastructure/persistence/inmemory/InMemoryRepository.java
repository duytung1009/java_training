package com.training.ecommerce.infrastructure.persistence.inmemory;

import com.training.ecommerce.shared.repository.Identifiable;
import com.training.ecommerce.shared.repository.Repository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository lưu trong bộ nhớ bằng {@code LinkedHashMap}, giữ thứ tự thêm.
 */
public class InMemoryRepository<T extends Identifiable<ID>, ID> implements Repository<T, ID> {
    private final Map<ID, T> store = new LinkedHashMap<>();

    @Override
    public T save(T entity) {
        if (entity == null || entity.getId() == null) {
            throw new IllegalArgumentException("entity và id không được null");
        }
        store.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public void saveAll(Collection<? extends T> entities) {
        if (entities == null) {
            throw new IllegalArgumentException("entities không được null");
        }
        for (T entity : entities) {
            save(entity);
        }
    }

    @Override
    public T findById(ID id) {
        if (id == null) {
            return null;
        }
        return store.get(id);
    }

    @Override
    public List<T> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public boolean deleteById(ID id) {
        return store.remove(id) != null;
    }

    @Override
    public int count() {
        return store.size();
    }
}
