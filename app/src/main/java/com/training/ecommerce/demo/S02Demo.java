package com.training.ecommerce.demo;

import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.inventory.InventoryManager;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;
import com.training.ecommerce.interfaces.cli.ReceiptPrinter;

import java.math.BigDecimal;

/**
 * Demo console S02: tạo catalog, đặt hàng, trừ kho, in hóa đơn, và các trường hợp lỗi.
 */
public class S02Demo {

    public static void main(String[] args) {
        InventoryManager inventory = new InventoryManager();
        inventory.addProduct(new PhysicalProduct("P01", "Bánh mì lúa mạch", new BigDecimal("35000"), 50, new BigDecimal("0.5")));
        inventory.addProduct(new PhysicalProduct("P02", "Sữa chua không đường", new BigDecimal("12000"), 100, new BigDecimal("0.5")));
        inventory.addProduct(new PhysicalProduct("P03", "Ức gà phi lê", new BigDecimal("85000"), 20, new BigDecimal("0.5")));

        ReceiptPrinter printer = new ReceiptPrinter();

        // Giảm 10%, VAT 8%
        Order order = new Order(new BigDecimal("0.10"), new BigDecimal("0.08"));
        order.addLine(new OrderLine(inventory.findById("P01"), 2));
        order.addLine(new OrderLine(inventory.findById("P02"), 5));
        processOrder(inventory, printer, order);

        // Lỗi: product không tồn tại
        Product unknown = inventory.findById("P99");
        System.out.println(">>> Tìm P99: " + (unknown == null ? "không tồn tại" : unknown.getName()));

        // Lỗi: thiếu hàng (ức gà chỉ còn 20)
        try {
            new OrderLine(inventory.findById("P03"), 21);
        } catch (IllegalStateException e) {
            System.out.println(">>> Lỗi: " + e.getMessage());
        }

        // Lỗi: quantity <= 0
        try {
            new OrderLine(inventory.findById("P01"), 0);
        } catch (IllegalArgumentException e) {
            System.out.println(">>> Lỗi: " + e.getMessage());
        }
    }

    private static void processOrder(InventoryManager inventory, ReceiptPrinter printer, Order order) {
        try {
            inventory.decreaseStock(order);
            System.out.println(printer.print(order));
        } catch (IllegalStateException | IllegalArgumentException e) {
            System.out.println(">>> Xử lý đơn thất bại: " + e.getMessage());
        }
    }
}
