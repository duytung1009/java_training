package org.example.session4.practice1;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Product {
    private double price;
    private String name;

    Product(double price, String name) {
        this.price = price;
        this.name = name;
    }
}
