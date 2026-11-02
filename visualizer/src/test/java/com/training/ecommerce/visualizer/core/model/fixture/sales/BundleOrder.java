package com.training.ecommerce.visualizer.core.model.fixture.sales;

import java.util.List;

/** Lớp con chứa danh sách lớp cha (composite): không làm {@link Order} thành "bị sở hữu". */
public class BundleOrder extends Order {

    private final List<Order> parts;

    public BundleOrder(List<Order> parts) {
        this.parts = List.copyOf(parts);
    }

    public List<Order> parts() {
        return parts;
    }
}
