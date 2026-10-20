import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

/**
 * S04 – Generics, Collections (List / Set / Map / Queue / Iterator) & Exception.
 * Chạy: java playground/session04/GenericsCollectionsPitfalls.java   (JDK 11+)
 *
 * ĐOÁN output từng dòng trước khi chạy! Số đầu dòng khớp với số câu quiz trong docs/sessions/session-04.md.
 * Câu 11 và câu 15 không có output (lỗi compile / câu thiết kế), xem phần comment.
 */
public class GenericsCollectionsPitfalls {

    /** Sản phẩm có id dùng trong equals/hashCode, giá thì không. */
    static class Item {
        final String sku;
        final BigDecimal price;

        Item(String sku, String price) {
            this.sku = sku;
            this.price = new BigDecimal(price);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Item && ((Item) o).sku.equals(sku);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sku);
        }

        @Override
        public String toString() {
            return sku;
        }
    }

    /** Key có field mutable tham gia vào hashCode – anti-pattern. */
    static class CartKey {
        String sku;

        CartKey(String sku) {
            this.sku = sku;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof CartKey && ((CartKey) o).sku.equals(sku);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sku);
        }
    }

    static class Resource implements AutoCloseable {
        private final String name;

        Resource(String name) {
            this.name = name;
        }

        @Override
        public void close() {
            System.out.print("close " + name + " ");
        }
    }

    static double sumAll(List<? extends Number> numbers) {
        double sum = 0;
        for (Number n : numbers) {
            sum += n.doubleValue();
        }
        return sum;
    }

    @SuppressWarnings("finally")
    static int finallyDoesNotChangeReturnedValue() {
        int x = 1;
        try {
            return x;
        } finally {
            x = 99;
        }
    }

    public static void main(String[] args) {
        // 1
        List<Integer> quantities = new ArrayList<>(List.of(1, 2, 3));
        quantities.remove(1);
        System.out.println("1: " + quantities);

        // 2
        List<String> fixed = Arrays.asList("Áo", "Quần");
        fixed.set(0, "Tất");
        try {
            fixed.add("Mũ");
            System.out.println("2: " + fixed);
        } catch (UnsupportedOperationException e) {
            System.out.println("2: set được → " + fixed + ", add → UnsupportedOperationException");
        }

        // 3
        try {
            List<String> immutable = List.of("Áo", null);
            System.out.println("3: " + immutable);
        } catch (NullPointerException e) {
            System.out.println("3: NullPointerException");
        }

        // 4
        List<String> cart = new ArrayList<>(List.of("Áo", "Quần", "Tất"));
        try {
            for (String item : cart) {
                if (item.equals("Áo")) {
                    cart.remove(item);
                }
            }
            System.out.print("4: không lỗi " + cart);
        } catch (ConcurrentModificationException e) {
            System.out.print("4: ConcurrentModificationException");
        }
        List<String> cart2 = new ArrayList<>(List.of("Áo", "Quần", "Tất"));
        for (String item : cart2) {
            if (item.equals("Quần")) {
                cart2.remove(item);   // xóa phần tử áp chót
            }
        }
        System.out.println(" | xóa áp chót: " + cart2);

        // 5
        Set<CartKey> keys = new HashSet<>();
        CartKey key = new CartKey("P001");
        keys.add(key);
        key.sku = "P002";
        System.out.println("5: contains(P002)=" + keys.contains(new CartKey("P002"))
                + ", contains(key)=" + keys.contains(key) + ", size=" + keys.size());

        // 6
        Set<Item> byPrice = new TreeSet<>(Comparator.comparing((Item i) -> i.price));
        byPrice.add(new Item("P001", "150000"));
        byPrice.add(new Item("P002", "150000"));
        byPrice.add(new Item("P003", "99000"));
        System.out.println("6: " + byPrice);

        // 7
        Map<String, Integer> qty = new HashMap<>();
        qty.merge("P001", 2, Integer::sum);
        qty.merge("P001", 3, Integer::sum);
        qty.merge("P002", 1, Integer::sum);
        qty.merge("P002", 1, (oldQty, add) -> null);
        System.out.println("7: " + new java.util.TreeMap<>(qty));

        // 8
        Map<String, List<String>> productsByCategory = new LinkedHashMap<>();
        productsByCategory.computeIfAbsent("FASHION", k -> new ArrayList<>()).add("Áo thun");
        productsByCategory.computeIfAbsent("FASHION", k -> new ArrayList<>()).add("Quần jean");
        productsByCategory.computeIfAbsent("SOFTWARE", k -> new ArrayList<>()).add("Office");
        System.out.println("8: " + productsByCategory);

        // 9
        try {
            Map<String, Integer> stock = Map.of("P001", 50, "P001", 20);
            System.out.println("9: " + stock);
        } catch (IllegalArgumentException e) {
            System.out.println("9: IllegalArgumentException");
        }

        // 10
        PriorityQueue<Integer> pq = new PriorityQueue<>(List.of(30, 10, 20, 5));
        StringBuilder polled = new StringBuilder();
        while (!pq.isEmpty()) {
            polled.append(pq.poll()).append(' ');
        }
        Deque<String> stack = new ArrayDeque<>();
        stack.push("Trang chủ");
        stack.push("Danh mục");
        stack.push("Sản phẩm");
        System.out.println("10: poll = " + polled.toString().trim() + " | pop = " + stack.pop());

        // 11: KHÔNG compile
        // List<Object> objects = new ArrayList<String>();          // incompatible types: generics invariant
        // List<? extends Number> numbers = new ArrayList<Integer>();
        // numbers.add(1);                                           // không add được vào ? extends
        // new T[10] / T.class trong class generic                   // type erasure
        System.out.println("11: sumAll(List<Integer>) = " + sumAll(List.of(1, 2, 3)));

        // 12
        boolean sameClass = new ArrayList<String>().getClass() == new ArrayList<Integer>().getClass();
        System.out.println("12: " + sameClass);

        // 13
        System.out.println("13: " + finallyDoesNotChangeReturnedValue());

        // 14
        System.out.print("14: ");
        try (Resource first = new Resource("A"); Resource second = new Resource("B")) {
            throw new IllegalStateException("body");
        } catch (IllegalStateException e) {
            System.out.println("| catch " + e.getMessage() + ", suppressed=" + e.getSuppressed().length);
        }

        // 15: câu thiết kế, không có output – xem quiz.
    }
}
