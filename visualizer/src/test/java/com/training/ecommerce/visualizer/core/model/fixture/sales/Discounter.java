package com.training.ecommerce.visualizer.core.model.fixture.sales;

/** Không định danh, có state đổi được, chỉ tham chiếu Order qua tham số: service + quan hệ param. */
public class Discounter {

    private int calls;

    public Money apply(Order order) {
        calls++;
        return new Money(calls);
    }
}
