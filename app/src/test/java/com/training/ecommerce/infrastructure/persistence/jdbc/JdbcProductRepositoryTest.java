package com.training.ecommerce.infrastructure.persistence.jdbc;

import com.training.ecommerce.domain.catalog.BundleProduct;
import com.training.ecommerce.domain.catalog.Category;
import com.training.ecommerce.domain.catalog.DigitalProduct;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.SubscriptionProduct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcProductRepositoryTest extends PostgresJdbcTestSupport {

    @Autowired
    private DataSource dataSource;

    private JdbcProductRepository repo;

    @BeforeEach
    void setUpRepo() {
        repo = new JdbcProductRepository(jdbc);
    }

    private static PhysicalProduct tshirt() {
        return new PhysicalProduct("P1", "Áo", new BigDecimal("150000"), 5, new BigDecimal("0.5"), Category.FASHION);
    }

    private static DigitalProduct ebook() {
        return new DigitalProduct("P2", "Ebook", new BigDecimal("99000"), 7, Category.SOFTWARE);
    }

    @Test
    void roundTripPhysical() {
        repo.save(tshirt());

        PhysicalProduct found = (PhysicalProduct) repo.findById("P1").orElseThrow();
        assertThat(found.getName()).isEqualTo("Áo");
        assertThat(found.getBasePrice()).isEqualByComparingTo("150000");
        assertThat(found.getStock()).isEqualTo(5);
        assertThat(found.getCategory()).isEqualTo(Category.FASHION);
        assertThat(found.getWeightKg()).isEqualByComparingTo("0.5");
    }

    @Test
    void roundTripDigital() {
        repo.save(ebook());

        Product found = repo.findById("P2").orElseThrow();
        assertThat(found).isInstanceOf(DigitalProduct.class);
        assertThat(found.getCategory()).isEqualTo(Category.SOFTWARE);
        assertThat(found.getStock()).isEqualTo(7);
    }

    @Test
    void roundTripSubscription() {
        repo.save(new SubscriptionProduct("P3", "Tin", new BigDecimal("30000"), 12, 3, Category.SUBSCRIPTION));

        SubscriptionProduct found = (SubscriptionProduct) repo.findById("P3").orElseThrow();
        assertThat(found.getMonths()).isEqualTo(12);
        assertThat(found.getBasePrice()).isEqualByComparingTo("30000");
        assertThat(found.calculateFinalPrice()).isEqualByComparingTo("360000");
    }

    @Test
    void roundTripBundleKeepsItemsInOrderAndDerivedPrice() {
        repo.saveAll(List.of(tshirt(), ebook()));
        Product t = repo.findById("P1").orElseThrow();
        Product e = repo.findById("P2").orElseThrow();
        repo.save(new BundleProduct("B1", "Combo", 4, new BigDecimal("0.1"), Category.OTHER, e, t));

        BundleProduct found = (BundleProduct) repo.findById("B1").orElseThrow();
        assertThat(found.getItems()).extracting(Product::getId).containsExactly("P2", "P1");
        assertThat(found.getDiscountRate()).isEqualByComparingTo("0.1");
        assertThat(found.getBasePrice()).isEqualByComparingTo("249000");
        assertThat(found.calculateFinalPrice()).isEqualByComparingTo("224100");
    }

    @Test
    void nestedBundleIsRestored() {
        repo.save(ebook());
        Product e = repo.findById("P2").orElseThrow();
        repo.save(new BundleProduct("B1", "Inner", 1, new BigDecimal("0.5"), e));
        Product inner = repo.findById("B1").orElseThrow();
        repo.save(new BundleProduct("B2", "Outer", 1, BigDecimal.ZERO, inner));

        BundleProduct outer = (BundleProduct) repo.findById("B2").orElseThrow();
        assertThat(outer.getItems()[0]).isInstanceOf(BundleProduct.class);
    }

    @Test
    void saveTwiceUpdatesFieldsAndKeepsPosition() {
        repo.save(tshirt());
        repo.save(ebook());

        repo.save(new PhysicalProduct("P1", "Áo mới", new BigDecimal("200000"), 1, new BigDecimal("0.6")));

        assertThat(repo.count()).isEqualTo(2);
        assertThat(repo.findAll()).extracting(Product::getId).containsExactly("P1", "P2");
        Product updated = repo.findById("P1").orElseThrow();
        assertThat(updated.getName()).isEqualTo("Áo mới");
        assertThat(updated.getBasePrice()).isEqualByComparingTo("200000");
        assertThat(updated.getCategory()).isEqualTo(Category.OTHER);
    }

    @Test
    void saveCanReplaceBundleItemsAndChangeSubtype() {
        repo.saveAll(List.of(tshirt(), ebook()));
        Product t = repo.findById("P1").orElseThrow();
        Product e = repo.findById("P2").orElseThrow();
        repo.save(new BundleProduct("B1", "Combo", 1, BigDecimal.ZERO, t, e));

        repo.save(new BundleProduct("B1", "Combo", 1, BigDecimal.ZERO, e));
        assertThat(((BundleProduct) repo.findById("B1").orElseThrow()).getItems()).hasSize(1);

        repo.save(new DigitalProduct("B1", "Đổi loại", BigDecimal.TEN, 1));
        assertThat(repo.findById("B1").orElseThrow()).isInstanceOf(DigitalProduct.class);
        assertThat(jdbc.sql("SELECT count(*) FROM bundle_items").query(Integer.class).single()).isZero();
    }

    @Test
    void findAllKeepsInsertionOrderAndIsUnmodifiable() {
        repo.save(ebook());
        repo.save(tshirt());

        List<Product> all = repo.findAll();

        assertThat(all).extracting(Product::getId).containsExactly("P2", "P1");
        assertThatThrownBy(() -> all.add(tshirt())).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void findByIdHandlesMissingAndNull() {
        assertThat(repo.findById("nope")).isEmpty();
        assertThat(repo.findById(null)).isEmpty();
    }

    @Test
    void deleteAndCount() {
        repo.save(tshirt());
        assertThat(repo.count()).isEqualTo(1);

        assertThat(repo.deleteById("P1")).isTrue();
        assertThat(repo.deleteById("P1")).isFalse();
        assertThat(repo.deleteById(null)).isFalse();
        assertThat(repo.count()).isZero();
    }

    @Test
    void itemInUseCannotBeDeletedButDeletingBundleKeepsItems() {
        repo.saveAll(List.of(tshirt(), ebook()));
        Product t = repo.findById("P1").orElseThrow();
        repo.save(new BundleProduct("B1", "Combo", 1, BigDecimal.ZERO, t));

        assertThatThrownBy(() -> repo.deleteById("P1")).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(repo.deleteById("B1")).isTrue();
        assertThat(repo.findById("P1")).isPresent();
    }

    @Test
    void saveRejectsNull() {
        assertThatThrownBy(() -> repo.save(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repo.saveAll(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void schemaRejectsNegativeStock() {
        repo.save(tshirt());

        assertThatThrownBy(() -> jdbc.sql("UPDATE products SET stock = -1").update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void seedDataLoadsAndIsIdempotent() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("data.sql"));
        populator.execute(dataSource);
        populator.execute(dataSource);      // lần 2: ON CONFLICT DO NOTHING

        assertThat(repo.count()).isEqualTo(4);
        BundleProduct combo = (BundleProduct) repo.findById("P-COMBO").orElseThrow();
        assertThat(combo.getItems()).extracting(Product::getId).containsExactly("P-TSHIRT", "P-EBOOK");
        assertThat(combo.calculateFinalPrice()).isEqualByComparingTo("224100");
        assertThat(repo.findById("P-NEWS").orElseThrow()).isInstanceOf(SubscriptionProduct.class);
    }
}
