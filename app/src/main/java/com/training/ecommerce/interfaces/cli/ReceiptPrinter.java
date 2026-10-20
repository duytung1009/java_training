package com.training.ecommerce.interfaces.cli;

import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;

public class ReceiptPrinter {

    public static String generateReceipt(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("             HÓA ĐƠN BÁN HÀNG           \n");
        sb.append("========================================\n");
        sb.append("Mã đơn hàng: ").append(order.getId()).append("\n");
        sb.append("----------------------------------------\n");

        for (OrderLine line : order.getItems()) {
            sb.append(String.format("%-20s x%-3d %12.2f\n",
                    line.getProduct().getName(),
                    line.getQuantity(),
                    line.getSubtotal()));
        }

        sb.append("----------------------------------------\n");
        sb.append(String.format("Tạm tính        : %15.2f\n", order.getRawTotal()));
        sb.append(String.format("Giảm giá (%.0f%%)   : -%14.2f\n", order.getDiscountRate() * 100, order.getDiscountAmount()));
        sb.append(String.format("Thuế VAT (%.0f%%)   : +%14.2f\n", order.getTaxRate() * 100, order.getTaxAmount()));
        sb.append("----------------------------------------\n");
        sb.append(String.format("TỔNG : %15.2f\n", order.getFinalTotal()));
        sb.append("========================================\n");

        return sb.toString();
    }
}
