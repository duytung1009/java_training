import java.util.Arrays;

/** S02 – chạy: java playground/session02/ControlFlowPitfalls.java — ĐOÁN output trước! */
public class ControlFlowPitfalls {
    static void modify(int[] arr, int n) {
        arr[0] = 99;          // sửa object mà reference trỏ tới -> caller thấy
        arr = new int[]{-1};  // gán lại bản copy của reference -> caller KHÔNG thấy
        n = 100;              // primitive: bản copy
    }

    public static void main(String[] args) {
        // 1. if không có {}
        int x = 5;
        if (x > 10)
            System.out.println("1: A");
            System.out.println("1: B");   // luôn chạy – thụt lề đánh lừa

        // 2. switch fall-through
        int v = 10;
        switch (v) {
            case 10: System.out.println("2: Ten");
            case 20: System.out.println("2: Twenty");
            default: System.out.println("2: Other");
        }

        // 3. switch expression (Java 14+) – không fall-through, trả về giá trị
        String day = "SAT";
        int hours = switch (day) {
            case "SAT", "SUN" -> 0;
            case "WED" -> {
                int review = 2;
                yield review;
            }
            default -> 8;
        };
        System.out.println("3: " + hours);

        // 4. Labeled break
        outer:
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (j == 1) continue outer;
                if (i == 2) break outer;
                System.out.println("4: " + i + "," + j);
            }
        }

        // 5. do-while chạy ít nhất 1 lần
        int k = 10;
        do { System.out.println("5: do-while chạy k=" + k); } while (k < 5);

        // 6. Java luôn pass-by-value
        int[] arr = {1, 2, 3};
        int n = 1;
        modify(arr, n);
        System.out.println("6: " + Arrays.toString(arr) + " n=" + n);

        // 7. Giá trị mặc định & in mảng
        int[][] m = new int[2][3];
        String[] names = new String[2];
        System.out.println("7: " + Arrays.deepToString(m) + " " + Arrays.toString(names));
        System.out.println("7b: " + arr);   // in ra [I@hash – không phải nội dung
    }
}
