import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * S05 – Functional Programming, Lambda & Stream API.
 * Chạy: java playground/session05/FunctionalStreamPitfalls.java   (cần JDK 16+ vì Stream.toList())
 *
 * ĐOÁN output từng dòng trước khi chạy! Số đầu dòng khớp với số câu quiz trong docs/sessions/session-05.md.
 * Câu 3 và câu 15 không có output (lỗi compile / câu thiết kế), xem phần comment.
 */
public class FunctionalStreamPitfalls {

    /** Câu 1: có hợp lệ với @FunctionalInterface không? */
    @FunctionalInterface
    interface PriceRule {
        BigDecimal apply(BigDecimal price);

        boolean equals(Object o);           // method public của Object → không tính

        default PriceRule then(PriceRule next) {
            return p -> next.apply(apply(p));
        }

        static PriceRule none() {
            return p -> p;
        }
    }

    /** Câu 4: this trong lambda vs anonymous class. */
    static class Cart {
        @Override
        public String toString() {
            return "Cart";
        }

        String[] run() {
            String[] out = new String[2];
            Runnable a = () -> out[0] = String.valueOf(this);
            Runnable b = new Runnable() {
                public void run() {
                    out[1] = String.valueOf(this);
                }
            };
            a.run();
            b.run();
            return out;
        }
    }

    static int loadCount = 0;

    static String loadDefaultName() {
        loadCount++;
        return "Sản phẩm mặc định";
    }

    public static void main(String[] args) {
        // 1
        PriceRule tenPercentOff = p -> p.multiply(new BigDecimal("0.9"));
        System.out.println("1: " + tenPercentOff.then(PriceRule.none()).apply(new BigDecimal("100000")));

        // 2
        Function<Integer, Integer> plus1 = x -> x + 1;
        Function<Integer, Integer> times2 = x -> x * 2;
        System.out.println("2: " + plus1.andThen(times2).apply(3) + " " + plus1.compose(times2).apply(3));

        // 3: KHÔNG compile – "local variables referenced from a lambda expression must be final or effectively final"
        // int count = 0;
        // List.of(1, 2, 3).forEach(x -> count++);

        // 4
        String[] carts = new Cart().run();
        String anonymous = carts[1].substring(0, carts[1].indexOf('@'));
        System.out.println("4: " + carts[0] + " / " + anonymous.substring(anonymous.lastIndexOf('$') - 4));

        // 5
        Stream<String> lazy = Stream.of("a", "b", "c").filter(x -> {
            System.out.println("filter " + x);
            return true;
        });
        System.out.println("5: done");

        // 6
        System.out.print("6: ");
        Stream.of("one", "two", "three", "four")
                .peek(x -> System.out.print(x + " "))
                .filter(x -> x.length() > 3)
                .findFirst()
                .ifPresent(System.out::println);

        // 7
        Stream<String> inStock = Stream.of("Áo thun", "Quần jean").filter(name -> !name.isBlank());
        long n = inStock.count();
        try {
            inStock.toList();
            System.out.println("7: chạy bình thường, n = " + n);
        } catch (IllegalStateException e) {
            System.out.println("7: IllegalStateException – " + e.getMessage());
        }

        // 8
        try {
            Map<String, Integer> qtyBySku = Stream.of("A:1", "B:2", "A:3")
                    .collect(Collectors.toMap(s -> s.split(":")[0], s -> Integer.parseInt(s.split(":")[1])));
            System.out.println("8: " + qtyBySku);
        } catch (IllegalStateException e) {
            System.out.println("8: IllegalStateException – " + e.getMessage());
        }

        // 9
        Optional<String> found = Optional.of("Áo thun nam");
        found.orElse(loadDefaultName());
        int afterOrElse = loadCount;
        found.orElseGet(FunctionalStreamPitfalls::loadDefaultName);
        System.out.println("9: orElse gọi load " + afterOrElse + " lần, orElseGet gọi thêm " + (loadCount - afterOrElse) + " lần");

        // 10
        List<String> names = Stream.of("Áo thun", "Quần jean").toList();
        try {
            names.add("Tất");
            System.out.println("10: " + names);
        } catch (UnsupportedOperationException e) {
            System.out.println("10: UnsupportedOperationException");
        }

        // 11
        System.out.print("11: ");
        String first = Stream.of("b", "a", "c")
                .peek(x -> System.out.print(x + " "))
                .sorted()
                .findFirst()
                .orElse("?");
        System.out.println("-> " + first);

        // 12
        Function<String, Integer> length = String::length;
        BiFunction<String, String, Boolean> sameIgnoreCase = String::equalsIgnoreCase;
        System.out.println("12: " + length.apply("Claude") + " " + sameIgnoreCase.apply("SALE10", "sale10"));

        // 13
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal price : List.of(new BigDecimal("150000"), new BigDecimal("450000"))) {
            total.add(price);
        }
        System.out.println("13: " + total);

        // 14
        Map<Boolean, List<Integer>> bigOrders = Stream.of(100_000, 250_000)
                .collect(Collectors.partitioningBy(total1 -> total1 >= 1_000_000));
        System.out.println("14: " + bigOrders);

        // 15: câu thiết kế, không có output – xem quiz.
    }
}
