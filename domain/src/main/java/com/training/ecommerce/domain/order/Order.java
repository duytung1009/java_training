package com.training.ecommerce.domain.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

/**
 * Đơn hàng: subtotal → discount → tax → finalAmount. Mọi số tiền có scale 2, HALF_UP.
 */
public class Order {
    private static final int MONEY_SCALE = 2;

    private final BigDecimal discountRate;
    private final BigDecimal taxRate;
    private OrderLine[] lines = new OrderLine[4];
    private int lineCount = 0;

    /**
     * @param discountRate tỉ lệ giảm giá trong [0, 1]
     * @param taxRate      tỉ lệ thuế &gt;= 0
     */
    public Order(BigDecimal discountRate, BigDecimal taxRate) {
        if (discountRate == null || discountRate.signum() < 0 || discountRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("discountRate phải trong [0, 1]");
        }
        if (taxRate == null || taxRate.signum() < 0) {
            throw new IllegalArgumentException("taxRate phải >= 0");
        }
        this.discountRate = discountRate;
        this.taxRate = taxRate;
    }

    public void addLine(OrderLine line) {
        if (line == null) {
            throw new IllegalArgumentException("line không được null");
        }
        if (lineCount == lines.length) {
            lines = Arrays.copyOf(lines, lines.length * 2);
        }
        lines[lineCount++] = line;
    }

    /** Bản copy các line theo thứ tự add. */
    public OrderLine[] getLines() {
        return Arrays.copyOf(lines, lineCount);
    }

    public BigDecimal subtotal() {
        BigDecimal total = Arrays.stream(lines, 0, lineCount)
                .map(OrderLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return money(total);
    }

    public BigDecimal discountAmount() {
        return money(subtotal().multiply(discountRate));
    }

    public BigDecimal taxAmount() {
        return money(taxableAmount().multiply(taxRate));
    }

    public BigDecimal finalAmount() {
        return taxableAmount().add(taxAmount());
    }

    public BigDecimal getDiscountRate() { return discountRate; }
    public BigDecimal getTaxRate() { return taxRate; }

    private BigDecimal taxableAmount() {
        return subtotal().subtract(discountAmount());
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
