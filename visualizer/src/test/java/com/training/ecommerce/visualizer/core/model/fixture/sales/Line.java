package com.training.ecommerce.visualizer.core.model.fixture.sales;

/** Có id nhưng bị Order sở hữu: entity, không phải aggregate. */
public class Line {

    private final String id;
    private final int quantity;

    public Line(String id, int quantity) {
        this.id = id;
        this.quantity = quantity;
    }

    public String id() {
        return id;
    }
}
