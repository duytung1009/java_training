package org.example.session4.practice1;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Phone extends Product {
    private String desc;

    public Phone(double price, String name, String desc) {
        super(price, name);
        this.desc = desc;
    }
}
