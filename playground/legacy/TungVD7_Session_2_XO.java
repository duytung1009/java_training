package org.example;

import java.util.Scanner;

public class TungVD7_Session_2_XO {
    static void main() {
        IO.println("Hello and welcome!");

        Scanner input = new Scanner(System.in);
        IO.println("Nhập chiều dài");
        int width = input.nextInt();
        IO.println("Nhập chiều rộng");
        int height = input.nextInt();

        int[][] matrix = new int[width][height];

        IO.println("Start");
        drawBoard(matrix);

        boolean isFirstTurn = true;
        boolean isPlaying = true;
        int turn = 0;
        while (isPlaying) {
            boolean isSuccess = false;
            while (!isSuccess) {
                IO.println(isFirstTurn ? "[Player 1] Nhập toạ độ x" : "[Player 2] Nhập toạ độ x");
                int inputX = input.nextInt();
                IO.println(isFirstTurn ? "[Player 1] Nhập toạ độ y" : "[Player 2] Nhập toạ độ y");
                int inputY = input.nextInt();
                if (isFirstTurn) {
                    isSuccess = setX(matrix, inputX, inputY);
                    IO.println(isSuccess
                            ? "[Player 1] Đánh " + inputX + "," + inputY
                            : "[Player 1] Đánh lại");
                } else {
                    isSuccess = setO(matrix, inputX, inputY);
                    IO.println(isSuccess
                            ? "[Player 2] Đánh " + inputX + "," + inputY
                            : "[Player 2] Đánh lại");
                }

                int winCheck = isWin(matrix);
                if (winCheck == 1) {
                    IO.println("[Player 1] Win");
                    isPlaying = false;
                } else if (winCheck == -1) {
                    IO.println("[Player 2] Win");
                    isPlaying = false;
                }
            }
            drawBoard(matrix);
            isFirstTurn = !isFirstTurn;

            if (turn == 10) {
                IO.println("Hết 10 lượt - terminated");
                isPlaying = false;
            }
            turn++;
        }
    }

    static void drawBoard(int[][] matrix) {
        IO.println("[Practice] draw XO:");

        // Draw first row
        StringBuilder firstRow = new StringBuilder();
        firstRow.append("#");
        for (int i = 0; i < matrix.length; i++) {
            firstRow.append('|');
            firstRow.append(i);
        }
        firstRow.append('|');
        IO.println(firstRow.toString());

        // Draw board
        int rowNum = 0;
        for (int[] row : matrix) {
            StringBuilder line = new StringBuilder();
            line.append(rowNum);
            for (int cell : row) {
                line.append('|');
                if (cell == 1) {
                    line.append('X');
                } else if (cell == -1) {
                    line.append('O');
                } else {
                    line.append('_');
                }
            }
            line.append('|');
            IO.println(line.toString());
            rowNum++;
        }
    }

    static boolean setX(int[][] matrix, int x, int y) {
        if (matrix[x][y] == 0) {
            matrix[x][y] = 1;
            return true;
        }
        return false;
    }

    static boolean setO(int[][] matrix, int x, int y) {
        if (matrix[x][y] == 0) {
            matrix[x][y] = -1;
            return true;
        }
        return false;
    }

    static int isWin(int[][] matrix) {
        int winningLength = 3;
        int result = 0; // 1: X win; -1: O win; 0: chưa ai win
        // Count
        int xCount = 0;
        int oCount = 0;
        // Check hang ngang
        for (int[] row : matrix) { 
            for (int cell : row) {
                if (cell == 1) {
                    xCount++;
                    oCount = 0;
                } else if (cell == -1) {
                    xCount = 0;
                    oCount++;
                } else {
                    xCount = 0;
                    oCount = 0;
                }
                IO.println("cell " + cell);
                IO.println("xCount " + xCount);
                IO.println("oCount " + oCount);
            }
            if (xCount >= winningLength) {
                return 1;
            } else if (oCount >= winningLength) {
                return -1;
            }
        }
        // TODO Check hang doc
        // TODO Rotate matrix: https://stackoverflow.com/questions/2799755/rotate-array-clockwise
        // TODO Check hang cheo 1
        // TODO Check hang cheo 2
        return result;   
    }
}
