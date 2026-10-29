package com.training.ecommerce.visualizer.sync;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Quét API công khai của module {@code domain} bằng reflection và trả về mỗi chữ ký một dòng.
 * Java thuần (không dùng Spring) để chạy được bằng {@code java -cp} trần.
 *
 * <p>Tái tạo snapshot:
 * {@code java -cp visualizer/target/classes:domain/target/classes
 * com.training.ecommerce.visualizer.sync.DomainApiScanner visualizer/src/main/resources/domain-api.txt}
 */
public final class DomainApiScanner {

    static final String DOMAIN_PACKAGE = "com.training.ecommerce.domain.";
    private static final String DEFAULT_OUTPUT = "visualizer/src/main/resources/domain-api.txt";

    private DomainApiScanner() {
    }

    /** Quét domain hiện tại; kết quả đã sắp xếp và không trùng. */
    public static List<String> scan() {
        ClassLoader loader = DomainApiScanner.class.getClassLoader();
        TreeSet<String> lines = new TreeSet<>();
        for (String className : classNames(loader)) {
            Class<?> type;
            try {
                type = Class.forName(className, false, loader);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Không nạp được " + className, e);
            }
            if (isPublic(type)) {
                describe(type, lines);
            }
        }
        return new ArrayList<>(lines);
    }

    private static List<String> classNames(ClassLoader loader) {
        try {
            URI location = com.training.ecommerce.domain.catalog.Product.class
                    .getProtectionDomain().getCodeSource().getLocation().toURI();
            Path root = Path.of(location);
            if (Files.isDirectory(root)) {
                return namesUnder(root);
            }
            try (FileSystem zip = FileSystems.newFileSystem(root)) {
                return namesUnder(zip.getPath("/"));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<String> namesUnder(Path root) throws IOException {
        Path base = root.resolve(DOMAIN_PACKAGE.replace('.', '/'));
        try (Stream<Path> files = Files.walk(base)) {
            return files.map(p -> root.relativize(p).toString().replace('\\', '/'))
                    .filter(n -> n.endsWith(".class"))
                    .map(n -> n.substring(0, n.length() - ".class".length()).replace('/', '.'))
                    .filter(DomainApiScanner::isCandidate)
                    .toList();
        }
    }

    private static boolean isCandidate(String name) {
        String simple = name.substring(name.lastIndexOf('.') + 1);
        if (simple.equals("module-info") || simple.equals("package-info")) {
            return false;
        }
        // lớp ẩn danh/cục bộ có phần sau '$' bắt đầu bằng chữ số: Foo$1, Foo$1Local
        for (String part : simple.split("\\$")) {
            if (!part.isEmpty() && Character.isDigit(part.charAt(0))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPublic(Class<?> type) {
        for (Class<?> c = type; c != null; c = c.getDeclaringClass()) {
            if (!Modifier.isPublic(c.getModifiers())) {
                return false;
            }
        }
        return true;
    }

    private static void describe(Class<?> type, TreeSet<String> out) {
        String name = typeName(type);
        out.add(kind(type) + " " + name + (isAbstractClass(type) ? " abstract" : ""));
        for (Constructor<?> c : type.getDeclaredConstructors()) {
            if (Modifier.isPublic(c.getModifiers()) && !c.isSynthetic()) {
                out.add("ctor " + name + "(" + params(c.getGenericParameterTypes()) + ")");
            }
        }
        for (Method m : type.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers()) && !m.isSynthetic() && !m.isBridge()) {
                out.add("method " + name + "#" + m.getName() + "(" + params(m.getGenericParameterTypes()) + "): "
                        + simple(m.getGenericReturnType()) + staticMark(m.getModifiers()));
            }
        }
        for (Field f : type.getDeclaredFields()) {
            if (Modifier.isPublic(f.getModifiers()) && !f.isSynthetic()) {
                out.add("field " + name + "." + f.getName() + ": " + simple(f.getGenericType())
                        + staticMark(f.getModifiers()));
            }
        }
    }

    private static String kind(Class<?> t) {
        if (t.isAnnotation()) return "annotation";
        if (t.isInterface()) return "interface";
        if (t.isEnum()) return "enum";
        if (t.isRecord()) return "record";
        return "class";
    }

    private static boolean isAbstractClass(Class<?> t) {
        return !t.isInterface() && Modifier.isAbstract(t.getModifiers());
    }

    private static String staticMark(int modifiers) {
        return Modifier.isStatic(modifiers) ? " static" : "";
    }

    private static String typeName(Class<?> t) {
        return t.getName().substring(DOMAIN_PACKAGE.length()).replace('$', '.');
    }

    private static String params(Type[] types) {
        return Arrays.stream(types).map(DomainApiScanner::simple).collect(Collectors.joining(", "));
    }

    /** Bỏ tiền tố package: {@code java.util.List<com.x.Product>} thành {@code List<Product>}. */
    private static String simple(Type t) {
        return t.getTypeName().replaceAll("\\b(?:[a-z_][\\w]*\\.)+", "").replace('$', '.');
    }

    /**
     * Ghi kết quả quét ra file. {@code args[0]} là đường dẫn đích, mặc định {@value #DEFAULT_OUTPUT}.
     */
    public static void main(String[] args) throws IOException {
        Path target = Path.of(args.length > 0 ? args[0] : DEFAULT_OUTPUT);
        List<String> signatures = scan();
        List<String> content = new ArrayList<>(List.of(
                "# Snapshot API công khai của module domain (mỗi dòng một chữ ký).",
                "# Visualizer so sánh file này với domain hiện tại để hiện banner cảnh báo khi lệch.",
                "# Không sửa tay. Tạo lại: ./mvnw -q -pl visualizer -am compile && java -cp "
                        + "visualizer/target/classes:domain/target/classes "
                        + "com.training.ecommerce.visualizer.sync.DomainApiScanner "
                        + "visualizer/src/main/resources/domain-api.txt"));
        content.addAll(signatures);
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        Files.write(target, content, StandardCharsets.UTF_8);
        System.out.println("Đã ghi " + signatures.size() + " chữ ký vào " + target);
    }
}
