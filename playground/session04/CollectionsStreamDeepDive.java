import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * S04 (pretest, ngoài lớp) – chạy: java playground/session04/CollectionsStreamDeepDive.java
 * ĐOÁN output từng dòng trước khi chạy! Gap tương ứng: docs/review/gaps.md (2026-10-01).
 * Viết tương thích Java 11+ (không dùng Stream.toList() / record).
 */
public class CollectionsStreamDeepDive {

    /** Key có field MUTABLE tham gia vào equals/hashCode – anti-pattern. */
    static class CartItem {
        String sku;
        CartItem(String sku) { this.sku = sku; }
        @Override public boolean equals(Object o) {
            return o instanceof CartItem && ((CartItem) o).sku.equals(sku);
        }
        @Override public int hashCode() { return Objects.hash(sku); }
        @Override public String toString() { return "CartItem(" + sku + ")"; }
    }

    /** Không implement Comparable. */
    static class Product {
        final String sku; final long price;
        Product(String sku, long price) { this.sku = sku; this.price = price; }
        @Override public String toString() { return sku; }
    }

    static int counter;

    static int finallyReturnValue() {
        int x = 1;
        try {
            return x;          // giá trị 1 được TÍNH & CẤT trước khi finally chạy
        } finally {
            x = 99;            // sửa biến local -> không ảnh hưởng giá trị đã cất
            counter++;         // nhưng finally vẫn CHẠY
        }
    }

    @SuppressWarnings("finally")
    static String finallySwallows() {
        try {
            throw new IllegalStateException("payment failed");
        } finally {
            return "OK";       // return trong finally GHI ĐÈ và NUỐT exception
        }
    }

    static String loadDescription(String sku) throws IOException {
        if (sku.isEmpty()) throw new IOException("empty sku");
        return "desc-" + sku;
    }

    public static void main(String[] args) {
        // ===== A. HashSet + mutable key (L3 · mình hiểu sai) =====
        Set<CartItem> cart = new HashSet<>();
        CartItem item = new CartItem("SKU-1");
        cart.add(item);
        item.sku = "SKU-2";                            // sửa field nằm trong hashCode SAU khi add
        System.out.println("1: contains(item) = " + cart.contains(item));                  // hash mới -> bucket khác
        System.out.println("2: contains(SKU-1) = " + cart.contains(new CartItem("SKU-1"))); // đúng bucket cũ, nhưng equals fail
        System.out.println("3: remove(item) = " + cart.remove(item) + ", size = " + cart.size());
        System.out.println("4: iterate -> " + cart);   // vẫn nằm đó: phần tử "mồ côi"

        // ===== B. TreeSet với class không Comparable (L3 · lớp chưa dạy) =====
        Set<Product> sorted = new TreeSet<>();         // COMPILE được (generic không ràng buộc Comparable)
        try {
            sorted.add(new Product("P1", 100));        // ngay phần tử ĐẦU TIÊN: TreeMap.put gọi compare(key, key)
            System.out.println("5: add OK");
        } catch (ClassCastException e) {
            System.out.println("5: ClassCastException ở lần add đầu tiên");
        }

        // ===== C. finally + return (L3 · mình hiểu sai) =====
        System.out.println("6: finallyReturnValue() = " + finallyReturnValue() + ", counter = " + counter);
        System.out.println("7: finallySwallows() = " + finallySwallows() + " (exception biến mất)");

        // ===== D. Stream lazy / vertical / short-circuit / single-use (L2–L3 · mình hiểu sai) =====
        List<String> skus = Arrays.asList("A1", "B2", "B3", "C4");
        Stream<String> pipeline = skus.stream()
                .peek(s -> System.out.println("   peek " + s))
                .filter(s -> s.startsWith("B"));
        System.out.println("8: đã tạo pipeline, chưa có terminal -> chưa in gì ở trên");
        System.out.println("9: findFirst (xem thứ tự peek/filter/map):");
        String first = skus.stream()
                .peek(s -> System.out.println("   peek   " + s))
                .filter(s -> s.startsWith("B"))
                .map(s -> { System.out.println("   map    " + s); return s.toLowerCase(); })
                .findFirst()                            // dừng ngay khi có kết quả -> C4, B3 không được xử lý
                .orElse("none");
        System.out.println("   => " + first);
        System.out.println("10: count lần 1 = " + pipeline.count()); // lúc này peek mới chạy
        try {
            pipeline.count();                          // Stream chỉ dùng MỘT lần
        } catch (IllegalStateException e) {
            System.out.println("11: IllegalStateException: " + e.getMessage());
        }

        // ===== E. Fail-fast / modCount (L3 · biết bề mặt) =====
        List<String> lines = new ArrayList<>(Arrays.asList("A", "B", "C", "D"));
        try {
            for (String s : lines) {
                if (s.equals("B")) {
                    lines.remove(s);                   // KHÔNG nổ ở đây
                    System.out.println("12: remove(B) xong, chưa có exception");
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("13: CME ở next() kế tiếp; lines = " + lines);
        }

        // ===== F. Lỗ hổng fail-fast: xóa phần tử ÁP CHÓT (L3 · lớp chưa dạy) =====
        List<String> promo = new ArrayList<>(Arrays.asList("A", "B", "C", "D"));
        List<String> visited = new ArrayList<>();
        for (String s : promo) {
            visited.add(s);
            if (s.equals("C")) promo.remove(s);        // cursor=3 == size mới 3 -> hasNext() false
        }
        System.out.println("14: không CME! visited = " + visited + ", promo = " + promo + " (D bị BỎ QUA)");

        List<String> safe = new ArrayList<>(Arrays.asList("A", "B", "C", "D"));
        safe.removeIf(s -> s.equals("B") || s.equals("C"));
        Iterator<String> it = safe.iterator();
        while (it.hasNext()) if (it.next().equals("D")) it.remove();
        System.out.println("15: removeIf / it.remove() an toàn -> " + safe);

        // ===== G. Collectors.toMap trùng key / null (L2 · mình hiểu sai) =====
        List<String[]> orderLines = Arrays.asList(
                new String[]{"SKU-1", "2"}, new String[]{"SKU-2", "1"}, new String[]{"SKU-1", "3"});
        try {
            Map<String, Integer> qty = orderLines.stream()
                    .collect(Collectors.toMap(l -> l[0], l -> Integer.parseInt(l[1])));
            System.out.println("16: " + qty);
        } catch (IllegalStateException e) {
            System.out.println("16: IllegalStateException: " + e.getMessage());
        }
        Map<String, Integer> merged = orderLines.stream()
                .collect(Collectors.toMap(l -> l[0], l -> Integer.parseInt(l[1]), Integer::sum));
        System.out.println("17: có merge fn -> SKU-1=" + merged.get("SKU-1") + ", SKU-2=" + merged.get("SKU-2"));
        try {
            Stream.of("SKU-9").collect(Collectors.toMap(s -> s, s -> (String) null));
        } catch (NullPointerException e) {
            System.out.println("18: value null -> NullPointerException (toMap dùng Map.merge)");
        }

        // ===== H. Lambda capture: effectively final (L2 · lớp chưa dạy) =====
        List<Product> products = Arrays.asList(new Product("P1", 100), new Product("P2", 250));
        long total = 0;
        // products.forEach(p -> total += p.price);   // COMPILE ERROR: local variables referenced
        //                                            // from a lambda expression must be final or effectively final
        long sum = products.stream().mapToLong(p -> p.price).sum();
        System.out.println("19: total (vẫn " + total + ") / mapToLong().sum() = " + sum);

        // ===== I. Checked exception trong lambda (L3 · biết bề mặt) =====
        // List<String> d = skus.stream().map(s -> loadDescription(s)).collect(...);
        //   -> COMPILE ERROR: unreported exception IOException, kể cả khi main có "throws IOException",
        //      vì lambda là thân của Function.apply() – method này không khai báo throws.
        try {
            List<String> descs = Stream.of("P1", "").map(s -> {
                try {
                    return loadDescription(s);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);   // bọc lại thành unchecked
                }
            }).collect(Collectors.toList());
            System.out.println("20: " + descs);
        } catch (UncheckedIOException e) {
            System.out.println("20: UncheckedIOException, cause = " + e.getCause().getMessage());
        }
    }
}
