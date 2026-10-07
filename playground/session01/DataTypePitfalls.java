/** S01 – chạy: java playground/session01/DataTypePitfalls.java — ĐOÁN output trước khi chạy! */
public class DataTypePitfalls {
    public static void main(String[] args) {
        // 1. Integer cache (-128..127)
        Integer a = 127, b = 127;
        Integer c = 128, d = 128;
        System.out.println("1a: " + (a == b));
        System.out.println("1b: " + (c == d));
        System.out.println("1c: " + c.equals(d));

        // 2. Narrowing overflow
        int i = 130;
        byte by = (byte) i;
        System.out.println("2: " + by);

        // 3. Truncate khi ép double -> int
        System.out.println("3: " + (int) 9.99 + " / " + (int) -9.99);

        // 4. char arithmetic
        char ch = 'a';
        System.out.println("4: " + (ch + 1) + " / " + (char) (ch + 1));

        // 5. Compound assignment tự ép kiểu
        byte x = 10;
        // x = x + 1;   // compile error: int -> byte
        x += 1;         // OK: tương đương x = (byte)(x + 1)
        System.out.println("5: " + x);

        // 6. int overflow âm thầm
        int max = Integer.MAX_VALUE;
        System.out.println("6: " + (max + 1));

        // 7. double không chính xác -> đừng dùng cho tiền
        System.out.println("7: " + (0.1 + 0.2));

        // 8. String pool
        String s1 = "hello";
        String s2 = "hello";
        String s3 = new String("hello");
        String s4 = "hel" + "lo";          // compile-time constant
        String part = "hel";
        String s5 = part + "lo";           // runtime concat -> object mới
        System.out.println("8a: " + (s1 == s2));
        System.out.println("8b: " + (s1 == s3));
        System.out.println("8c: " + (s1 == s4));
        System.out.println("8d: " + (s1 == s5));
        System.out.println("8e: " + (s1 == s5.intern()));

        // 9. Unboxing null -> NPE
        try {
            Integer maybe = null;
            int y = maybe;
            System.out.println(y);
        } catch (NullPointerException e) {
            System.out.println("9: NullPointerException");
        }

        // 10. String immutable
        String s = "java";
        s.toUpperCase();
        System.out.println("10: " + s);
    }
}
