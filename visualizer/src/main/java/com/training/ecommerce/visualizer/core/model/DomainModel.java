package com.training.ecommerce.visualizer.core.model;

import java.util.List;
import java.util.Set;

/**
 * Mô hình domain để xem theo tầng: context, class (kèm vai trò) và quan hệ. Đây cũng là định dạng file
 * {@code domain-model.json}.
 *
 * @param version   phiên bản schema (hiện chỉ có 1); {@code Integer} để phát hiện file thiếu
 * @param contexts  các context, mỗi id một lần
 * @param classes   các class domain đã xếp context và vai trò
 * @param relations quan hệ có hướng giữa các class
 */
public record DomainModel(Integer version, List<ContextDef> contexts, List<ClassDef> classes,
                          List<RelationDef> relations) {

    /** Phiên bản schema hiện hành. */
    public static final int CURRENT_VERSION = 1;

    /** Các giá trị hợp lệ của {@link ClassDef#role()}. */
    public static final Set<String> ROLES = Set.of("aggregate", "entity", "value", "service", "other");

    /** Chuẩn hoá: danh sách null thành rỗng và sao chép bất biến. */
    public DomainModel {
        contexts = contexts == null ? List.of() : List.copyOf(contexts);
        classes = classes == null ? List.of() : List.copyOf(classes);
        relations = relations == null ? List.of() : List.copyOf(relations);
    }

    /**
     * @param id    id context (vd. "inventory")
     * @param title tiêu đề hiển thị
     * @param color tên biến màu CSS, null nếu chưa chọn
     */
    public record ContextDef(String id, String title, String color) {
    }

    /**
     * @param cls     tên đơn giản của class (khớp {@code NodeView.cls}), vd. "Warehouse"
     * @param context id context chứa class, null nếu chưa xếp
     * @param role    {@code aggregate | entity | value | service | other}
     * @param hidden  true nếu ẩn khỏi bản đồ
     * @param source  lý do gợi ý (vd. {@code package}, {@code identity}, {@code field}, {@code manual}), có thể null
     */
    public record ClassDef(String cls, String context, String role, boolean hidden, String source) {
    }

    /**
     * @param from   class nguồn
     * @param to     class đích
     * @param source {@code field}, {@code param}, {@code observed:<scenarioId>} hoặc {@code manual}
     */
    public record RelationDef(String from, String to, String source) {
    }
}
