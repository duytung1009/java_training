package com.training.ecommerce.visualizer.core.model.fixture;

/** Class nằm thẳng trong package gốc của domain giả: context null. Cũng là anchor của {@code DomainApiSpec}. */
public final class RootThing {

    private final String label;

    public RootThing(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
