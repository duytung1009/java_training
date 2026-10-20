// SPEC S04 – sinh bởi java-start-backlog 21/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
// SPEC S05 – nâng cấp 21/10: findById trả Optional (Spec S05 D2).
package com.training.ecommerce.infrastructure.persistence.inmemory;

import com.training.ecommerce.domain.catalog.DigitalProduct;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.shared.repository.Identifiable;
import com.training.ecommerce.shared.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRepositoryTest {

    private Repository<Product, String> products;
    private Product tshirt;
    private Product jeans;
    private Product office;

    @BeforeEach
    void setUp() {
        products = new InMemoryRepository<>();
        tshirt = new PhysicalProduct("P001", "Áo thun nam", new BigDecimal("150000"), 50, new BigDecimal("0.3"));
        jeans = new PhysicalProduct("P002", "Quần jean", new BigDecimal("450000"), 20, new BigDecimal("0.8"));
        office = new DigitalProduct("D001", "Office 2024 bản quyền", new BigDecimal("1000000"), 200);
    }

    @Nested
    @DisplayName("save / findById")
    class SaveAndFind {

        @Test
        @DisplayName("save trả về chính entity; findById trả đúng instance")
        void savesAndFindsSameInstance() {
            assertSame(tshirt, products.save(tshirt));
            products.save(office);

            assertSame(tshirt, products.findById("P001").orElseThrow());
            assertSame(office, products.findById("D001").orElseThrow());
        }

        @Test
        @DisplayName("Tìm id theo giá trị (new String), không theo reference")
        void findsByIdValue() {
            products.save(tshirt);

            assertSame(tshirt, products.findById(new String("P001")).orElseThrow());
        }

        @Test
        @DisplayName("id không tồn tại / null → Optional.empty()")
        void returnsEmptyWhenMissing() {
            products.save(tshirt);

            assertTrue(products.findById("P999").isEmpty());
            assertTrue(products.findById(null).isEmpty());
        }

        @Test
        @DisplayName("save cùng id → ghi đè (upsert), count không đổi, giữ vị trí cũ")
        void upsertsSameId() {
            products.save(tshirt);
            products.save(jeans);
            Product tshirtUpdated = new PhysicalProduct("P001", "Áo thun nam (giá mới)", new BigDecimal("129000"), 60, new BigDecimal("0.3"));

            products.save(tshirtUpdated);

            assertEquals(2, products.count());
            assertSame(tshirtUpdated, products.findById("P001").orElseThrow());
            assertSame(tshirtUpdated, products.findAll().get(0));
            assertSame(jeans, products.findAll().get(1));
        }

        @Test
        @DisplayName("save(null) → IllegalArgumentException")
        void rejectsNullEntity() {
            assertThrows(IllegalArgumentException.class, () -> products.save(null));
        }
    }

    @Nested
    @DisplayName("findAll / count / deleteById")
    class FindAllAndDelete {

        @Test
        @DisplayName("findAll theo thứ tự thêm")
        void findAllKeepsInsertionOrder() {
            products.save(office);
            products.save(tshirt);
            products.save(jeans);

            List<Product> all = products.findAll();

            assertEquals(3, all.size());
            assertSame(office, all.get(0));
            assertSame(tshirt, all.get(1));
            assertSame(jeans, all.get(2));
        }

        @Test
        @DisplayName("Repository rỗng → findAll rỗng, count 0")
        void emptyRepository() {
            assertTrue(products.findAll().isEmpty());
            assertEquals(0, products.count());
        }

        @Test
        @DisplayName("findAll không sửa được (UnsupportedOperationException), repository không bị ảnh hưởng")
        void findAllIsUnmodifiable() {
            products.save(tshirt);
            List<Product> all = products.findAll();

            assertThrows(UnsupportedOperationException.class, () -> all.add(jeans));
            assertEquals(1, products.count());
        }

        @Test
        @DisplayName("deleteById: có → true và xóa; không có → false")
        void deletesById() {
            products.save(tshirt);
            products.save(jeans);

            assertTrue(products.deleteById("P001"));
            assertFalse(products.deleteById("P001"));
            assertFalse(products.deleteById("P999"));
            assertTrue(products.findById("P001").isEmpty());
            assertEquals(1, products.count());
            assertSame(jeans, products.findAll().get(0));
        }
    }

    @Nested
    @DisplayName("Generic")
    class Generic {

        /** Entity khác Product, id kiểu Long: chứng minh Repository dùng chung được. */
        private static class Customer implements Identifiable<Long> {
            private final Long id;
            private final String name;

            Customer(Long id, String name) {
                this.id = id;
                this.name = name;
            }

            @Override
            public Long getId() {
                return id;
            }
        }

        @Test
        @DisplayName("Dùng được với entity khác, id kiểu Long")
        void worksWithOtherEntityType() {
            Repository<Customer, Long> customers = new InMemoryRepository<>();
            Customer tung = new Customer(1L, "Tùng");
            customers.save(tung);

            assertSame(tung, customers.findById(1L).orElseThrow());
            assertEquals(1, customers.count());
        }

        @Test
        @Tag("bonus")
        @DisplayName("saveAll nhận List<PhysicalProduct> cho Repository<Product, String> (? extends T)")
        void saveAllAcceptsSubtypeCollection() {
            List<PhysicalProduct> physicalProducts = new ArrayList<>();
            physicalProducts.add((PhysicalProduct) tshirt);
            physicalProducts.add((PhysicalProduct) jeans);

            products.saveAll(physicalProducts);

            assertEquals(2, products.count());
            assertSame(jeans, products.findById("P002").orElseThrow());
        }
    }
}
