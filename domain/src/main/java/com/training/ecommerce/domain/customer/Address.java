package com.training.ecommerce.domain.customer;

/**
 * Địa chỉ giao hàng (value object).
 */
public final class Address {
    private final String street;
    private final String city;

    public Address(String street, String city) {
        if (street == null || street.isBlank()) {
            throw new IllegalArgumentException("street không được rỗng");
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("city không được rỗng");
        }
        this.street = street;
        this.city = city;
    }

    public String getStreet() { return street; }
    public String getCity() { return city; }
}
