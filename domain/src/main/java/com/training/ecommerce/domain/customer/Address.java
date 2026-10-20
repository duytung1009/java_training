package com.training.ecommerce.domain.customer;

public class Address {
    public final String address;
    public final double shippingPrice;

    public Address(String address, double shippingPrice) {
        this.address = address;
        this.shippingPrice = shippingPrice;
    }
}
