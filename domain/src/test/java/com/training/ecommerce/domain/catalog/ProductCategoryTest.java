// SPEC S05 – sinh bởi java-start-backlog 21/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
package com.training.ecommerce.domain.catalog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Product.getCategory (S05)")
class ProductCategoryTest {

    private static final BigDecimal WEIGHT = new BigDecimal("0.3");

    @Test
    @DisplayName("Enum Category có đủ 5 giá trị theo thứ tự")
    void categoryValues() {
        assertArrayEquals(
                new Category[]{Category.FASHION, Category.ELECTRONICS, Category.SOFTWARE, Category.SUBSCRIPTION, Category.OTHER},
                Category.values());
    }

    @Nested
    @DisplayName("Constructor cũ (không có category) → OTHER")
    class DefaultCategory {

        @Test
        @DisplayName("Physical / Digital / Subscription / Bundle")
        void oldConstructorsDefaultToOther() {
            Product tshirt = new PhysicalProduct("P001", "Áo thun nam", new BigDecimal("150000"), 50, WEIGHT);
            Product office = new DigitalProduct("D001", "Office 2024", new BigDecimal("1000000"), 200);
            Product pro = new SubscriptionProduct("S001", "Gói Claude Pro", new BigDecimal("499000"), 12, 100);
            Product bundle = new BundleProduct("B001", "Combo", 10, new BigDecimal("0.10"), tshirt, office);

            assertEquals(Category.OTHER, tshirt.getCategory());
            assertEquals(Category.OTHER, office.getCategory());
            assertEquals(Category.OTHER, pro.getCategory());
            assertEquals(Category.OTHER, bundle.getCategory());
        }
    }

    @Nested
    @DisplayName("Constructor mới (category ở cuối, Bundle: trước items)")
    class WithCategory {

        @Test
        @DisplayName("Lưu đúng category, các thuộc tính cũ và giá không đổi")
        void storesCategory() {
            PhysicalProduct tshirt = new PhysicalProduct("P001", "Áo thun nam", new BigDecimal("150000"), 50, WEIGHT, Category.FASHION);
            DigitalProduct office = new DigitalProduct("D001", "Office 2024", new BigDecimal("1000000"), 200, Category.SOFTWARE);
            SubscriptionProduct pro = new SubscriptionProduct("S001", "Gói Claude Pro", new BigDecimal("499000"), 12, 100, Category.SUBSCRIPTION);
            BundleProduct bundle = new BundleProduct("B001", "Combo", 10, new BigDecimal("0.10"), Category.FASHION, tshirt, office);

            assertEquals(Category.FASHION, tshirt.getCategory());
            assertEquals(WEIGHT, tshirt.getWeightKg());
            assertEquals(50, tshirt.getStock());
            assertEquals(new BigDecimal("150000.00"), tshirt.calculateFinalPrice());

            assertEquals(Category.SOFTWARE, office.getCategory());
            assertEquals(Category.SUBSCRIPTION, pro.getCategory());
            assertEquals(new BigDecimal("5988000.00"), pro.calculateFinalPrice());

            assertEquals(Category.FASHION, bundle.getCategory());
            assertEquals(2, bundle.getItems().length);
            // (150.000 + 1.000.000) × 0.9
            assertEquals(new BigDecimal("1035000.00"), bundle.calculateFinalPrice());
        }

        @Test
        @DisplayName("category null → IllegalArgumentException (mọi subtype)")
        void rejectsNullCategory() {
            Product tshirt = new PhysicalProduct("P001", "Áo thun nam", new BigDecimal("150000"), 50, WEIGHT);

            assertThrows(IllegalArgumentException.class,
                    () -> new PhysicalProduct("P001", "Áo", new BigDecimal("150000"), 50, WEIGHT, null));
            assertThrows(IllegalArgumentException.class,
                    () -> new DigitalProduct("D001", "Office", new BigDecimal("1000000"), 200, null));
            assertThrows(IllegalArgumentException.class,
                    () -> new SubscriptionProduct("S001", "Pro", new BigDecimal("499000"), 12, 100, null));
            assertThrows(IllegalArgumentException.class,
                    () -> new BundleProduct("B001", "Combo", 10, new BigDecimal("0.10"), (Category) null, tshirt));
        }

        @Test
        @DisplayName("Validation cũ vẫn chạy ở constructor mới")
        void keepsOldValidation() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PhysicalProduct("P001", "Áo", new BigDecimal("-1"), 50, WEIGHT, Category.FASHION));
            assertThrows(IllegalArgumentException.class,
                    () -> new SubscriptionProduct("S001", "Pro", new BigDecimal("499000"), 0, 100, Category.SUBSCRIPTION));
        }
    }
}
