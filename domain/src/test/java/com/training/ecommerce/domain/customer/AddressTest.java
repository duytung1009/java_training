// SPEC S03 – sinh bởi java-start-backlog 20/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
package com.training.ecommerce.domain.customer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AddressTest {

    @Test
    @DisplayName("Getter trả đúng street và city")
    void exposesValues() {
        Address address = new Address("17 Duy Tân, Cầu Giấy", "Hà Nội");

        assertEquals("17 Duy Tân, Cầu Giấy", address.getStreet());
        assertEquals("Hà Nội", address.getCity());
    }

    @ParameterizedTest(name = "street = \"{0}\"")
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("street null/rỗng → IllegalArgumentException")
    void rejectsBlankStreet(String street) {
        assertThrows(IllegalArgumentException.class, () -> new Address(street, "Hà Nội"));
    }

    @ParameterizedTest(name = "city = \"{0}\"")
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("city null/rỗng → IllegalArgumentException")
    void rejectsBlankCity(String city) {
        assertThrows(IllegalArgumentException.class, () -> new Address("17 Duy Tân", city));
    }
}
