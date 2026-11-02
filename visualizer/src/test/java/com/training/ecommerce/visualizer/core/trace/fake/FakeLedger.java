package com.training.ecommerce.visualizer.core.trace.fake;

import java.util.ArrayList;
import java.util.List;

/** Listener giả: ghi sổ khi nhận {@link MoneyDeposited}. */
public class FakeLedger {

    private final List<Object> entries = new ArrayList<>();

    public void on(Object event) {
        entries.add(event);
    }

    public List<Object> entries() {
        return entries;
    }
}
