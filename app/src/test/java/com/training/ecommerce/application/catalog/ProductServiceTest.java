package com.training.ecommerce.application.catalog;

import com.training.ecommerce.domain.catalog.DigitalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.ProductNotFoundException;
import com.training.ecommerce.infrastructure.persistence.inmemory.InMemoryRepository;
import com.training.ecommerce.shared.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductServiceTest {
    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(new InMemoryRepository<>());
    }

    private static Product ebook(String id) {
        return new DigitalProduct(id, "Ebook " + id, new BigDecimal("99000"), 5);
    }

    @Test
    void saveThenFind() {
        Product saved = service.save(ebook("P1"));

        assertSame(saved, service.findById("P1"));
        assertEquals(1, service.findAll().size());
    }

    @Test
    void saveSameIdOverwrites() {
        service.save(ebook("P1"));
        service.save(new DigitalProduct("P1", "Mới", BigDecimal.TEN, 1));

        assertEquals(1, service.findAll().size());
        assertEquals("Mới", service.findById("P1").getName());
    }

    @Test
    void findMissingThrows() {
        ProductNotFoundException e = assertThrows(ProductNotFoundException.class, () -> service.findById("nope"));
        assertEquals("nope", e.getProductId());
        assertThrows(ProductNotFoundException.class, () -> service.findById(null));
    }

    @Test
    void deleteRemovesAndMissingThrows() {
        service.save(ebook("P1"));

        service.delete("P1");

        assertEquals(0, service.findAll().size());
        assertThrows(ProductNotFoundException.class, () -> service.delete("P1"));
    }

    @Test
    void updateReplacesExistingProduct() {
        service.save(ebook("P1"));

        service.update("P1", new DigitalProduct("P1", "Mới", BigDecimal.TEN, 1));

        assertEquals("Mới", service.findById("P1").getName());
    }

    @Test
    void updateMissingThrowsAndIdMismatchIsRejected() {
        service.save(ebook("P1"));

        assertThrows(ProductNotFoundException.class, () -> service.update("P9", ebook("P9")));
        assertThrows(IllegalArgumentException.class, () -> service.update("P1", ebook("P2")));
        assertThrows(IllegalArgumentException.class, () -> service.update("P1", null));
        assertEquals(1, service.findAll().size());
    }

    @Test
    void deleteTranslatesIntegrityViolationToProductInUse() {
        Repository<Product, String> failing = new InMemoryRepository<>() {
            @Override
            public boolean deleteById(String id) {
                throw new DataIntegrityViolationException("fk");
            }
        };

        ProductInUseException e = assertThrows(ProductInUseException.class, () -> new ProductService(failing).delete("P1"));
        assertEquals("P1", e.getProductId());
    }

    @Test
    void rejectsNullRepository() {
        assertThrows(IllegalArgumentException.class, () -> new ProductService(null));
    }
}
