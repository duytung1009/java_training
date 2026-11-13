package com.training.ecommerce.domain.order;

import com.training.ecommerce.shared.repository.Identifiable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Đơn hàng: subtotal → discount → tax → finalAmount. Mọi số tiền có scale 2, HALF_UP.
 */
public class Order implements Identifiable<String> {
    private static final int MONEY_SCALE = 2;

    private final String id;
    private final BigDecimal discountRate;
    private final BigDecimal taxRate;
    private OrderLine[] lines = new OrderLine[4];
    private int lineCount = 0;

    /**
     * Tạo đơn mới với id ngẫu nhiên ({@link UUID#randomUUID()}).
     *
     * @param discountRate tỉ lệ giảm giá trong [0, 1]
     * @param taxRate      tỉ lệ thuế &gt;= 0
     * @throws IllegalArgumentException khi discountRate hoặc taxRate không hợp lệ
     */
    public Order(BigDecimal discountRate, BigDecimal taxRate) {
        this(UUID.randomUUID().toString(), discountRate, taxRate);
    }

    /**
     * Tạo đơn với id cho trước (dùng khi khôi phục đơn đã lưu).
     *
     * @param id           id đơn, không rỗng
     * @param discountRate tỉ lệ giảm giá trong [0, 1]
     * @param taxRate      tỉ lệ thuế &gt;= 0
     * @throws IllegalArgumentException khi id null/rỗng, hoặc discountRate / taxRate không hợp lệ
     */
    public Order(String id, BigDecimal discountRate, BigDecimal taxRate) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id không được rỗng");
        }
        if (discountRate == null || discountRate.signum() < 0 || discountRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("discountRate phải trong [0, 1]");
        }
        if (taxRate == null || taxRate.signum() < 0) {
            throw new IllegalArgumentException("taxRate phải >= 0");
        }
        this.id = id;
        this.discountRate = discountRate;
        this.taxRate = taxRate;
    }

    @Override
    public String getId() { return id; }

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

    /**
     * Tổng số lượng theo product id (cộng dồn các line cùng product), giữ thứ tự xuất hiện đầu tiên.
     *
     * @return map không sửa được
     */
    public Map<String, Integer> quantitiesByProductId() {
        // toMap có merge Integer::sum để cộng dồn line trùng product, LinkedHashMap để giữ thứ tự xuất hiện
        Map<String, Integer> quantities = Arrays.stream(lines, 0, lineCount)
                .collect(Collectors.toMap(
                        line -> line.getProduct().getId(),
                        OrderLine::getQuantity,
                        Integer::sum,
                        LinkedHashMap::new));
        return Collections.unmodifiableMap(quantities);
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
