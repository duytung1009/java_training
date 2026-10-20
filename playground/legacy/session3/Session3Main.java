package org.example.session3;

import org.example.session3.shape.Circle;
import org.example.session3.shape.Rectangle;
import org.example.session3.shape.Shape;
import org.example.session3.shape.Triangle;

public class Session3Main {

    public static void main(String[] args) {
        Shape[] shapes = new Shape[]{
                new Circle(3),
                new Rectangle(2,4),
                new Triangle(3,4,5)
        };
        try {
            for (Shape s : shapes) {
                IO.println(s.describe());
                IO.println("Area: " + s.area() + " - Perimeter: " + s.perimeter());
            }
        } catch (IllegalArgumentException e) {
            IO.println("Lỗi: " + e.getMessage());
        }
    }
}
