package com.training.ecommerce.visualizer.core.model.fixture.sales;

/** Lớp trừu tượng có field và getId() để thử việc kế thừa định danh. */
public abstract class BaseEntity {

    protected String id;

    public String getId() {
        return id;
    }
}
