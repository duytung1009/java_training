package com.training.ecommerce.domain.shipping;

import com.training.ecommerce.domain.customer.Address;

public interface Shippable {
    double calculateShipping(Address dest);
}
