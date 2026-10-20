package com.training.ecommerce.interfaces.cli;

import com.training.ecommerce.domain.order.Order;

/**
 * Định dạng hóa đơn thành chuỗi (App tự in ra console).
 */
public class ReceiptPrinter {

    /**
     * Mỗi line: {@code "%-20s%5d%12.2f"} (name, quantity, lineTotal), tên dài hơn 20 ký tự giữ nguyên.
     * Sau đó là các dòng bắt đầu bằng Subtotal, Discount, Tax, Total. Luôn dùng dấu chấm thập phân.
     *
     * @throws IllegalArgumentException khi order null
     */
    public String print(Order order) {
        throw new UnsupportedOperationException("TODO S02");
    }
}
