package com.training.ecommerce.visualizer.core.view;

import java.util.List;

/**
 * Một node trên bản đồ: object domain thật, hoặc container (collection / area).
 *
 * @param id     node id (vd. "warehouse:WH-HN", "order:a1f3")
 * @param kind   {@link #OBJECT}, {@link #COLLECTION} hoặc {@link #AREA}
 * @param cls    tên class (vd. "Warehouse", "List&lt;Order&gt;") hoặc package với area (vd. "domain.pricing")
 * @param title  tiêu đề hiển thị
 * @param sub    chú thích ngắn, null nếu không có
 * @param parent id node cha, null với node gốc
 * @param items  các ô con; container luôn rỗng (con của nó là node khác)
 */
public record NodeView(String id, String kind, String cls, String title, String sub, String parent,
                       List<ItemView> items) {

    public static final String OBJECT = "object";
    public static final String COLLECTION = "collection";
    public static final String AREA = "area";
}
