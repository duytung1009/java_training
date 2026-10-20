package org.example.session3.shape;

public class Triangle extends Shape {
    private int a;
    private int b;
    private int c;

    public Triangle(int a, int b, int c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    @Override
    public double area() {
        return calculateArea(a, b, c);
    }

    @Override
    public double perimeter() {
        return a + b + c;
    }

    @Override
    public String describe() {
        return "Triangle: a: " + a + ", b: " + b + ", c: " + c;
    }

    // Bên dưới là code AI
    // Hàm kiểm tra 3 cạnh có tạo thành tam giác hợp lệ không
    private boolean isValidTriangle(double a, double b, double c) {
        return (a + b > c) && (a + c > b) && (b + c > a) && (a > 0) && (b > 0) && (c > 0);
    }

    // Hàm tính diện tích tam giác theo công thức Heron
    private double calculateArea(double a, double b, double c) {
        if (!isValidTriangle(a, b, c)) {
            throw new IllegalArgumentException("3 cạnh không tạo thành một tam giác hợp lệ!");
        }

        double p = (a + b + c) / 2.0; // Nửa chu vi
        double area = Math.sqrt(p * (p - a) * (p - b) * (p - c));
        return area;
    }
}
