package com.training.ecommerce.interfaces.cli;

import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;

import java.util.Locale;

/**
 * Định dạng hóa đơn thành chuỗi. Luôn dùng dấu chấm thập phân (Locale.ROOT).
 */
public class ReceiptPrinter {
    private static final String SEPARATOR = "-------------------------------------";
    private static final String LINE_FORMAT = "%-20s%5d%12.2f%n";
    private static final String SUMMARY_FORMAT = "%-25s%12.2f%n";

    public String print(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("order không được null");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("HÓA ĐƠN BÁN HÀNG").append(System.lineSeparator());
        sb.append(SEPARATOR).append(System.lineSeparator());
        for (OrderLine line : order.getLines()) {
            sb.append(String.format(Locale.ROOT, LINE_FORMAT,
                    line.getProduct().getName(), line.getQuantity(), line.lineTotal()));
        }
        sb.append(SEPARATOR).append(System.lineSeparator());
        sb.append(String.format(Locale.ROOT, SUMMARY_FORMAT, "Subtotal", order.subtotal()));
        sb.append(String.format(Locale.ROOT, SUMMARY_FORMAT, "Discount", order.discountAmount().negate()));
        sb.append(String.format(Locale.ROOT, SUMMARY_FORMAT, "Tax", order.taxAmount()));
        sb.append(String.format(Locale.ROOT, SUMMARY_FORMAT, "Total", order.finalAmount()));
        return sb.toString();
    }
}
