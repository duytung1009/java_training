package org.example.session4.practice1;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Laptop extends Product {
    private String size;

    public Laptop(double price, String name, String size) {
        super(price, name);
        this.size = size;
    }
}
