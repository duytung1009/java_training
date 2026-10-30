package com.training.ecommerce.visualizer.core.model;

import com.training.ecommerce.visualizer.core.model.DomainModel.ClassDef;
import com.training.ecommerce.visualizer.core.model.DomainModel.ContextDef;
import com.training.ecommerce.visualizer.core.model.DomainModel.RelationDef;
import com.training.ecommerce.visualizer.core.sync.DomainApiScanner;
import com.training.ecommerce.visualizer.core.sync.DomainApiSpec;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Gợi ý {@link DomainModel} bằng reflection trên các class dưới package gốc của domain. Java thuần, không biết
 * domain cụ thể nào; người dùng chỉnh lại kết quả trên giao diện.
 *
 * <p>Vai trò xét theo thứ tự: Throwable (ẩn) / enum, record, interface (không có field) / định danh + state đổi
 * được + có bị class cùng context sở hữu hay không. Heuristic cho phép sai; mỗi class mang {@code source} ghi lý do.
 */
public final class DomainModelSuggester {

    private DomainModelSuggester() {
    }

    /**
     * Các class domain xét đến (top-level, public, không ẩn danh/synthetic), khoá theo tên đơn giản. Hai class
     * trùng tên đơn giản thì giữ class đầu theo tên đầy đủ.
     *
     * @throws IllegalArgumentException nếu spec null
     */
    public static Map<String, Class<?>> domainClasses(DomainApiSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("spec không được null");
        }
        Map<String, Class<?>> result = new TreeMap<>();
        for (Class<?> type : DomainApiScanner.classes(spec)) {
            if (type.getDeclaringClass() == null && Modifier.isPublic(type.getModifiers())
                    && !type.isAnonymousClass() && !type.isSynthetic() && !type.isLocalClass()) {
                result.putIfAbsent(type.getSimpleName(), type);
            }
        }
        return result;
    }

    /**
     * Gợi ý chỉ từ cấu trúc class.
     *
     * @throws IllegalArgumentException nếu spec null
     */
    public static DomainModel suggest(DomainApiSpec spec) {
        return suggest(spec, List.of());
    }

    /**
     * Gợi ý từ cấu trúc class, gộp thêm quan hệ quan sát được. Cặp đã có thì giữ nguồn tĩnh; quan hệ tự trỏ hoặc
     * trỏ ra ngoài tập class bị bỏ.
     *
     * @throws IllegalArgumentException nếu spec hoặc observed null
     */
    public static DomainModel suggest(DomainApiSpec spec, List<RelationDef> observed) {
        if (spec == null || observed == null) {
            throw new IllegalArgumentException("spec và observed không được null");
        }
        Map<String, Class<?>> classes = domainClasses(spec);
        Map<Class<?>, String> names = new LinkedHashMap<>();
        classes.forEach((name, type) -> names.put(type, name));
        String prefix = spec.basePackage() + ".";

        Map<String, String> contextOf = new TreeMap<>();
        classes.forEach((name, type) -> contextOf.put(name, contextOf(type, prefix)));
        Map<String, List<Field>> fieldsOf = new TreeMap<>();
        classes.forEach((name, type) -> fieldsOf.put(name, instanceFields(type, prefix)));

        // owned: class khác cùng context có field kiểu class này; lớp con chứa lớp cha (composite) không tính
        Set<String> owned = new TreeSet<>();
        fieldsOf.forEach((owner, fields) -> {
            for (Field f : fields) {
                for (Class<?> ref : referenced(f.getGenericType())) {
                    String target = names.get(ref);
                    if (target != null && !target.equals(owner) && !ref.isAssignableFrom(classes.get(owner))
                            && Objects.equals(contextOf.get(owner), contextOf.get(target))) {
                        owned.add(target);
                    }
                }
            }
        });

        List<ClassDef> defs = new ArrayList<>();
        classes.forEach((name, type) -> defs.add(classDef(name, type, contextOf.get(name), fieldsOf.get(name),
                owned.contains(name))));

        Map<String, RelationDef> relations = new TreeMap<>();
        classes.forEach((name, type) -> collectRelations(name, type, names, relations));
        for (RelationDef r : observed) {
            if (r == null || r.from() == null || r.to() == null || r.from().equals(r.to())
                    || !classes.containsKey(r.from()) || !classes.containsKey(r.to())) {
                continue;
            }
            relations.putIfAbsent(key(r.from(), r.to()), r);
        }

        TreeSet<String> contextIds = new TreeSet<>();
        contextOf.values().stream().filter(Objects::nonNull).forEach(contextIds::add);
        List<ContextDef> contexts = contextIds.stream().map(id -> new ContextDef(id, id, null)).toList();
        defs.sort(Comparator.comparing(ClassDef::cls));
        List<RelationDef> sortedRelations = relations.values().stream()
                .sorted(Comparator.comparing(RelationDef::from).thenComparing(RelationDef::to)).toList();
        return new DomainModel(DomainModel.CURRENT_VERSION, contexts, defs, sortedRelations);
    }

    private static String key(String from, String to) {
        return from + "\u0000" + to;
    }

    /** Context là phân đoạn package đầu tiên dưới gốc; class nằm thẳng ở gốc thì null. */
    private static String contextOf(Class<?> type, String prefix) {
        String rest = type.getPackageName().isEmpty() ? "" : type.getPackageName() + ".";
        if (!rest.startsWith(prefix)) {
            return null;
        }
        String tail = rest.substring(prefix.length());
        return tail.isEmpty() ? null : tail.substring(0, tail.indexOf('.'));
    }

    /** Field instance (không synthetic) của class và các lớp cha nằm trong domain. */
    private static List<Field> instanceFields(Class<?> type, String prefix) {
        List<Field> result = new ArrayList<>();
        for (Class<?> c = type; c != null && c.getName().startsWith(prefix); c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()) && !f.isSynthetic()) {
                    result.add(f);
                }
            }
        }
        return result;
    }

    private static ClassDef classDef(String name, Class<?> type, String context, List<Field> fields,
                                     boolean owned) {
        if (Throwable.class.isAssignableFrom(type)) {
            return new ClassDef(name, context, "other", true, "type");
        }
        if (type.isEnum() || type.isRecord()) {
            return new ClassDef(name, context, "value", false, "type");
        }
        boolean noFields = fields.isEmpty();
        if (type.isInterface()) {
            return new ClassDef(name, context, "other", false, "type");
        }
        if (Modifier.isAbstract(type.getModifiers()) && noFields) {
            return new ClassDef(name, context, "other", false, "type");
        }
        boolean identity = hasIdentity(type);
        if (!identity && noFields) {
            // không định danh, không state: chỉ có hành vi (vd. class tiện ích / domain service)
            return new ClassDef(name, context, "service", false, "type");
        }
        boolean mutable = fields.stream().anyMatch(f -> !Modifier.isFinal(f.getModifiers())
                || Collection.class.isAssignableFrom(f.getType()) || Map.class.isAssignableFrom(f.getType()));
        if (identity) {
            return owned ? new ClassDef(name, context, "entity", false, "field")
                    : mutable ? new ClassDef(name, context, "aggregate", false, "identity")
                    : new ClassDef(name, context, "entity", false, "identity");
        }
        return mutable ? new ClassDef(name, context, "service", false, "field")
                : new ClassDef(name, context, "value", false, "field");
    }

    private static boolean hasIdentity(Class<?> type) {
        for (Method m : type.getMethods()) {
            if (m.getParameterCount() == 0 && !Modifier.isStatic(m.getModifiers())
                    && (m.getName().equals("getId") || m.getName().equals("id"))) {
                return true;
            }
        }
        return implementsIdentifiable(type);
    }

    private static boolean implementsIdentifiable(Class<?> type) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (Class<?> i : c.getInterfaces()) {
                if (i.getSimpleName().equals("Identifiable") || implementsIdentifiable(i)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void collectRelations(String from, Class<?> type, Map<Class<?>, String> names,
                                         Map<String, RelationDef> out) {
        // field trước: field thắng param khi cùng một cặp
        for (Field f : type.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && !f.isSynthetic()) {
                add(from, referenced(f.getGenericType()), names, "field", out);
            }
        }
        for (Constructor<?> c : type.getDeclaredConstructors()) {
            if (Modifier.isPublic(c.getModifiers()) && !c.isSynthetic()) {
                for (Type p : c.getGenericParameterTypes()) {
                    add(from, referenced(p), names, "param", out);
                }
            }
        }
        for (Method m : type.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers()) && !m.isSynthetic() && !m.isBridge()) {
                for (Type p : m.getGenericParameterTypes()) {
                    add(from, referenced(p), names, "param", out);
                }
            }
        }
    }

    private static void add(String from, List<Class<?>> targets, Map<Class<?>, String> names, String source,
                            Map<String, RelationDef> out) {
        for (Class<?> target : targets) {
            String to = names.get(target);
            if (to != null && !to.equals(from)) {
                String k = key(from, to);
                RelationDef existing = out.get(k);
                if (existing == null || ("param".equals(existing.source()) && "field".equals(source))) {
                    out.put(k, new RelationDef(from, to, source));
                }
            }
        }
    }

    /** Các class được nhắc tới trong một kiểu, bóc mảng và tham số generic. */
    private static List<Class<?>> referenced(Type type) {
        List<Class<?>> out = new ArrayList<>();
        collect(type, out);
        return out;
    }

    private static void collect(Type type, List<Class<?>> out) {
        switch (type) {
            case Class<?> c -> {
                if (c.isArray()) {
                    collect(c.getComponentType(), out);
                } else {
                    out.add(c);
                }
            }
            case ParameterizedType p -> {
                collect(p.getRawType(), out);
                for (Type arg : p.getActualTypeArguments()) {
                    collect(arg, out);
                }
            }
            case GenericArrayType g -> collect(g.getGenericComponentType(), out);
            case WildcardType w -> {
                for (Type bound : w.getUpperBounds()) {
                    collect(bound, out);
                }
                for (Type bound : w.getLowerBounds()) {
                    collect(bound, out);
                }
            }
            default -> {
                // TypeVariable: không biết class cụ thể
            }
        }
    }
}
