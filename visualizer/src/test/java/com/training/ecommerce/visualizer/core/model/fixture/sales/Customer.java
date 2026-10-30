package com.training.ecommerce.visualizer.core.model.fixture.sales;

/** Kế thừa định danh từ BaseEntity, có field đổi được: aggregate. */
public class Customer extends BaseEntity {

    private String name;

    public void rename(String newName) {
        this.name = newName;
    }
}
