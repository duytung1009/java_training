package com.training.ecommerce.visualizer.core.model.fixture.billing;

import com.training.ecommerce.visualizer.core.model.fixture.sales.Money;

/** Định danh nhờ implement Identifiable (không có getId()), state đổi được: aggregate. */
public class Invoice implements Identifiable {

    private final Money total = new Money(0);
    private boolean paid;

    @Override
    public String ident() {
        return "inv";
    }

    public void pay() {
        paid = true;
    }
}
