package org.example;

public class DrawBoxWithArray {
    static void main() {
        int[][] box = {
                {1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1},
        };

        int[][] cross = {
                {1, 0, 0, 0, 0, 0, 0, 0, 1},
                {0, 1, 0, 0, 0, 0, 0, 1, 0},
                {0, 0, 1, 0, 0, 0, 1, 0, 0},
                {0, 0, 0, 1, 0, 1, 0, 0, 0},
                {0, 0, 0, 0, 1, 0, 0, 0, 0},
                {0, 0, 0, 1, 0, 1, 0, 0, 0},
                {0, 0, 1, 0, 0, 0, 1, 0, 0},
                {0, 1, 0, 0, 0, 0, 0, 1, 0},
                {1, 0, 0, 0, 0, 0, 0, 0, 1},
        };

//        IO.println("Vẽ Hộp");
//        drawBox(box);

        IO.println("Vẽ Dấu X - Hard code");
        drawBox(cross);

        IO.println("Vẽ Dấu X - Dynamic size");
        drawCross(5);
    }

    private static void drawBox(int[][] source) {
        for (int[] row : source) {
            StringBuilder line = new StringBuilder();   // Gom hết cell vào 1 string để in ra 1 line riêng
            for (int cell : row) {
                line.append(cell == 1 ? " * " : "   ");
            }
            IO.println(line.toString());
        }
    }

    private static void drawCross(int size) {
        for (int i = 0; i < size; i++) {
            StringBuilder line = new StringBuilder();   // Gom hết cell vào 1 string để in ra 1 line riêng
            for (int j = 0; j < size; j++) {
                if (i == j || i == (size - 1) - j) {
                    line.append(" * ");
                } else {
                    line.append("   ");
                }
            }
            IO.println(line.toString());
        }
    }
}
