package com.training.ecommerce.visualizer.core.trace.fake;

import java.util.ArrayList;
import java.util.List;

/** Domain giả tự ghi event nội bộ và cho rút bằng {@code pullDomainEvents()}. */
public class FakeAccount {

    public static final long LARGE = 1000;

    private final String name;
    private long balance;
    private final List<Object> pending = new ArrayList<>();

    public FakeAccount(String name) {
        this.name = name;
    }

    /** Gửi tiền: ghi {@link MoneyDeposited}, và thêm {@link LargeDeposit} nếu từ {@link #LARGE} trở lên. */
    public synchronized void deposit(long amount) {
        balance += amount;
        pending.add(new MoneyDeposited(name, amount));
        if (amount >= LARGE) {
            pending.add(new LargeDeposit(name, amount));
        }
    }

    /** Ghi event rồi ném exception (để test rút event khi lời gọi lỗi). */
    public synchronized void depositThenFail(long amount) {
        pending.add(new MoneyDeposited(name, amount));
        throw new IllegalStateException("tài khoản bị khoá");
    }

    public synchronized long balance() {
        return balance;
    }

    /** Trả các event đã ghi kể từ lần rút trước rồi xoá. */
    public synchronized List<Object> pullDomainEvents() {
        List<Object> out = List.copyOf(pending);
        pending.clear();
        return out;
    }
}
