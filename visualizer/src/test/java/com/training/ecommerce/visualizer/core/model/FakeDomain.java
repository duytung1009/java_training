package com.training.ecommerce.visualizer.core.model;

import com.training.ecommerce.visualizer.core.model.fixture.RootThing;
import com.training.ecommerce.visualizer.core.sync.DomainApiSpec;

/** Domain giả dùng chung cho test của {@code core.model}. */
final class FakeDomain {

    static final DomainApiSpec SPEC = new DomainApiSpec(
            "com.training.ecommerce.visualizer.core.model.fixture", RootThing.class, "/khong-can.txt");

    private FakeDomain() {
    }

    static DomainModel.ClassDef classDef(DomainModel model, String cls) {
        return model.classes().stream().filter(c -> c.cls().equals(cls)).findFirst()
                .orElseThrow(() -> new AssertionError("Không có class " + cls));
    }

    static DomainModel.RelationDef relation(DomainModel model, String from, String to) {
        return model.relations().stream().filter(r -> r.from().equals(from) && r.to().equals(to)).findFirst()
                .orElse(null);
    }
}
