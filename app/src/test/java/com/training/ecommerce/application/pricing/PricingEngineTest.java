// SPEC S03 – sinh bởi java-start-backlog 20/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
package com.training.ecommerce.application.pricing;

import com.training.ecommerce.domain.catalog.BundleProduct;
import com.training.ecommerce.domain.catalog.DigitalProduct;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.SubscriptionProduct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PricingEngineTest {

    private final PricingEngine engine = new PricingEngine();

    private final Product tshirt = new PhysicalProduct("P001", "Áo thun nam", new BigDecimal("150000"), 50, new BigDecimal("0.3"));
    private final Product jeans = new PhysicalProduct("P002", "Quần jean", new BigDecimal("450000"), 20, new BigDecimal("0.8"));
    private final Product office = new DigitalProduct("D001", "Office 2024 bản quyền", new BigDecimal("1000000"), 200);
    private final Product claudePro = new SubscriptionProduct("S001", "Gói Claude Pro", new BigDecimal("499000"), 12, 100);

    /** Subtype mới chỉ có trong test: chứng minh thêm loại product không cần sửa PricingEngine (OCP). */
    private static class GiftCardProduct extends Product {
        GiftCardProduct(String id, String name, BigDecimal faceValue) {
            super(id, name, faceValue, 1000);
        }

        @Override
        public BigDecimal calculateFinalPrice() {
            // Thẻ quà tặng bán với giá 95% mệnh giá
            return getBasePrice().multiply(new BigDecimal("0.95")).setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    @Test
    @DisplayName("Doanh thu = Σ calculateFinalPrice() của mọi loại product")
    void sumsFinalPricesOfAllProductTypes() {
        List<Product> products = Arrays.asList(tshirt, jeans, office, claudePro);

        // 150.000 + 450.000 + 1.000.000 + 5.988.000
        assertEquals(new BigDecimal("7588000.00"), engine.calculateRevenue(products));
    }

    @Test
    @DisplayName("Có BundleProduct trong danh sách vẫn tính đúng (OCP)")
    void includesBundles() {
        Product outfit = new BundleProduct("B001", "Combo áo + quần", 10, new BigDecimal("0.10"), tshirt, jeans);

        // 540.000 + 1.000.000
        assertEquals(new BigDecimal("1540000.00"), engine.calculateRevenue(Arrays.asList(outfit, office)));
    }

    @Test
    @DisplayName("Subtype mới định nghĩa ngoài domain vẫn được tính đúng mà không sửa PricingEngine (OCP)")
    void supportsNewSubtypeWithoutChangingEngine() {
        Product giftCard = new GiftCardProduct("G001", "Thẻ quà tặng 500k", new BigDecimal("500000"));

        // 475.000 + 150.000
        assertEquals(new BigDecimal("625000.00"), engine.calculateRevenue(Arrays.asList(giftCard, tshirt)));
    }

    @Test
    @DisplayName("Danh sách rỗng → 0.00")
    void emptyListIsZero() {
        assertEquals(new BigDecimal("0.00"), engine.calculateRevenue(new ArrayList<>()));
    }

    @Test
    @DisplayName("Danh sách null → IllegalArgumentException")
    void rejectsNullList() {
        assertThrows(IllegalArgumentException.class, () -> engine.calculateRevenue(null));
    }

    @Test
    @DisplayName("Danh sách chứa null → IllegalArgumentException")
    void rejectsNullElement() {
        assertThrows(IllegalArgumentException.class, () -> engine.calculateRevenue(Arrays.asList(tshirt, null)));
    }
}
