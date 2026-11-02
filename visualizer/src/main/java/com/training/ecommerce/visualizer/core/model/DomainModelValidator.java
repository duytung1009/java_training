package com.training.ecommerce.visualizer.core.model;

import com.training.ecommerce.visualizer.core.model.DomainModel.ClassDef;
import com.training.ecommerce.visualizer.core.model.DomainModel.ContextDef;
import com.training.ecommerce.visualizer.core.model.DomainModel.RelationDef;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Kiểm tra một {@link DomainModel} theo tập class thật của domain. */
public class DomainModelValidator {

    private final Set<String> domainClasses;

    /**
     * @param domainClasses tên đơn giản các class của domain
     * @throws IllegalArgumentException nếu domainClasses null
     */
    public DomainModelValidator(Set<String> domainClasses) {
        if (domainClasses == null) {
            throw new IllegalArgumentException("domainClasses không được null");
        }
        this.domainClasses = Set.copyOf(domainClasses);
    }

    /** Trả về mọi lỗi tìm thấy; rỗng nghĩa là hợp lệ. */
    public List<ModelError> validate(DomainModel model) {
        List<ModelError> errors = new ArrayList<>();
        if (model == null) {
            errors.add(new ModelError(null, "Mô hình không được null"));
            return errors;
        }
        if (model.version() == null) {
            errors.add(new ModelError(null, "Thiếu version"));
        } else if (model.version() != DomainModel.CURRENT_VERSION) {
            errors.add(new ModelError(null, "version phải là " + DomainModel.CURRENT_VERSION
                    + " (đang là " + model.version() + ")"));
        }

        Set<String> contextIds = new HashSet<>();
        for (ContextDef context : model.contexts()) {
            if (context == null || context.id() == null || context.id().isBlank()) {
                errors.add(new ModelError(null, "Context phải có id"));
            } else if (!contextIds.add(context.id())) {
                errors.add(new ModelError(null, "Trùng id context: " + context.id()));
            }
        }

        Set<String> listed = new HashSet<>();
        for (ClassDef def : model.classes()) {
            if (def == null || def.cls() == null || def.cls().isBlank()) {
                errors.add(new ModelError(null, "Class phải có tên"));
                continue;
            }
            String name = def.cls();
            if (!domainClasses.contains(name)) {
                errors.add(new ModelError(name, "Class không có trong domain: " + name));
            }
            if (!listed.add(name)) {
                errors.add(new ModelError(name, "Class được liệt kê nhiều lần (một class chỉ thuộc một context): "
                        + name));
            }
            if (def.context() != null && !contextIds.contains(def.context())) {
                errors.add(new ModelError(name, "Context chưa khai báo: " + def.context()));
            }
            if (def.role() == null || !DomainModel.ROLES.contains(def.role())) {
                errors.add(new ModelError(name, "role không hợp lệ: " + def.role()
                        + " (chọn một trong " + new java.util.TreeSet<>(DomainModel.ROLES) + ")"));
            }
        }

        for (RelationDef relation : model.relations()) {
            if (relation == null) {
                errors.add(new ModelError(null, "Quan hệ không được null"));
                continue;
            }
            for (String end : new String[]{relation.from(), relation.to()}) {
                if (end == null || !listed.contains(end)) {
                    errors.add(new ModelError(end, "Quan hệ " + relation.from() + " → " + relation.to()
                            + " trỏ tới class không có trong mô hình: " + end));
                }
            }
        }
        return errors;
    }
}
