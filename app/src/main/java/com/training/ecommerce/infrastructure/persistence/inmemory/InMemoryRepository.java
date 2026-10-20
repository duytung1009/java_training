package com.training.ecommerce.infrastructure.persistence.inmemory;

import com.training.ecommerce.shared.repository.Identifiable;
import com.training.ecommerce.shared.repository.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Repository lưu trong bộ nhớ, giữ thứ tự thêm.
 */
public class InMemoryRepository<T extends Identifiable<ID>, ID> implements Repository<T, ID> {

    @Override
    public T save(T entity) {
        throw new UnsupportedOperationException("TODO S04");
    }

    @Override
    public void saveAll(Collection<? extends T> entities) {
        throw new UnsupportedOperationException("TODO S04");
    }

    @Override
    public T findById(ID id) {
        throw new UnsupportedOperationException("TODO S04");
    }

    @Override
    public List<T> findAll() {
        throw new UnsupportedOperationException("TODO S04");
    }

    @Override
    public boolean deleteById(ID id) {
        throw new UnsupportedOperationException("TODO S04");
    }

    @Override
    public int count() {
        throw new UnsupportedOperationException("TODO S04");
    }
}
