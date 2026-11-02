package com.training.ecommerce.visualizer.core.model.fixture.sales;

import com.training.ecommerce.visualizer.core.model.fixture.billing.Invoice;

import java.util.ArrayList;
import java.util.List;

/** Aggregate: có getId(), chứa List<Line>, tham chiếu sang context billing qua field. */
public class Order {

    private final String id = "o1";
    private final List<Line> lines = new ArrayList<>();
    private Status status = Status.NEW;
    private Invoice invoice;

    public String getId() {
        return id;
    }

    /** Line vừa là field vừa là param: quan hệ phải mang nguồn field. */
    public void addLine(Line line) {
        lines.add(line);
    }

    public Status status() {
        return status;
    }
}
