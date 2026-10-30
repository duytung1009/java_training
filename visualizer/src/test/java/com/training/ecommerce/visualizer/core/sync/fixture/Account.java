package com.training.ecommerce.visualizer.core.sync.fixture;

/** Domain giả cho test scanner: class công khai có ctor, method, field. */
public class Account {

    public static final int LIMIT = 10;

    public Account(String owner) {
    }

    public int balance() {
        return 0;
    }

    public static Account open(String owner) {
        return new Account(owner);
    }

    String hidden() {
        return "";
    }

    /** Lớp ẩn danh sinh ra Account$1: scanner phải bỏ qua. */
    Runnable anonymous() {
        return new Runnable() {
            @Override
            public void run() {
            }
        };
    }
}
