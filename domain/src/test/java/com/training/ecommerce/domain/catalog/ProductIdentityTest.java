// SPEC S03 – sinh bởi java-start-backlog 20/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
package com.training.ecommerce.domain.catalog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Product: equals / hashCode / toString")
class ProductIdentityTest {

    private final Product tshirt = new PhysicalProduct("P001", "Áo thun nam", new BigDecimal("150000"), 50, new BigDecimal("0.3"));

    @Test
    @DisplayName("2 product cùng id → HashSet.size() == 1")
    void sameIdCollapsesInHashSet() {
        Product sameIdOtherData = new PhysicalProduct(new String("P001"), "Áo thun nam (bản cập nhật)", new BigDecimal("129000"), 5, new BigDecimal("0.4"));
        Set<Product> catalog = new HashSet<>();
        catalog.add(tshirt);
        catalog.add(sameIdOtherData);

        assertEquals(1, catalog.size());
        assertEquals(tshirt, sameIdOtherData);
        assertEquals(tshirt.hashCode(), sameIdOtherData.hashCode());
    }

    @Test
    @DisplayName("Cùng id nhưng khác subtype vẫn bằng nhau (equals chỉ theo id), và đối xứng")
    void equalityIgnoresSubtype() {
        Product digitalP001 = new DigitalProduct("P001", "Áo thun nam (bản số)", new BigDecimal("1"), 1);

        assertEquals(tshirt, digitalP001);
        assertEquals(digitalP001, tshirt);
        assertEquals(tshirt.hashCode(), digitalP001.hashCode());
    }

    @Test
    @DisplayName("Khác id → không bằng nhau, HashSet giữ cả hai")
    void differentIdsAreDifferent() {
        Product jeans = new PhysicalProduct("P002", "Quần jean", new BigDecimal("450000"), 20, new BigDecimal("0.8"));
        Set<Product> catalog = new HashSet<>();
        catalog.add(tshirt);
        catalog.add(jeans);

        assertNotEquals(tshirt, jeans);
        assertEquals(2, catalog.size());
    }

    @Test
    @DisplayName("equals: reflexive, null → false, khác kiểu → false")
    void equalsContractEdges() {
        assertEquals(tshirt, tshirt);
        assertFalse(tshirt.equals(null));
        assertFalse(tshirt.equals("P001"));
    }

    @Test
    @DisplayName("toString có id và tên (không phải Class@hash)")
    void toStringContainsIdAndName() {
        String text = tshirt.toString();

        assertTrue(text.contains("P001"), text);
        assertTrue(text.contains("Áo thun nam"), text);
    }
}
