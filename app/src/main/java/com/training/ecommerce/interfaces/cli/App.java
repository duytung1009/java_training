package com.training.ecommerce.interfaces.cli;

import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.inventory.InventoryManager;

import java.math.BigDecimal;

public class App {
    private static Product[] catalog;
    private static InventoryManager inventoryManager;

    public static void main(String[] args) throws Exception {
        // 1. Khởi tạo Catalog sản phẩm (Mảng Product[])
        catalog = new Product[]{
                new Product("P01", "Bánh mì lúa mạch", new BigDecimal("35000")),
                new Product("P02", "Sữa chua không đường", new BigDecimal("12000")),
                new Product("P03", "Ức gà phi lê", new BigDecimal("85000"))
        };

        // 2. Khởi tạo kho hàng và đặt số lượng tồn kho
        inventoryManager = new InventoryManager(10);
        inventoryManager.setStock("P01", 50);
        inventoryManager.setStock("P02", 100);
        inventoryManager.setStock("P03", 20);

        // 4. Tạo đơn hàng mới (Giảm 10%, Thuế VAT 8%)
        Order order = new Order("ORD-001", 0.10, 0.08);
        order.addItem(findProductById("P01"), 2);
        order.addItem(findProductById("P02"), 5);

        // 5. Điều phối xử lý đơn hàng
        processOrder(order);
    }

    public static Product findProductById(String id) {
        if (catalog == null) return null;
        for (Product p : catalog) {
            if (p != null && p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    public static void processOrder(Order order) throws Exception {
        System.out.println(">>> Đang xử lý đơn hàng: " + order.getId());

        // Bước 1: Kiểm tra kho
        if (!inventoryManager.canFulfill(order)) {
            System.out.println("Xử lý thất bại: Kho không đủ số lượng tồn!");
            return;
        }

        // Bước 2: Khấu trừ tồn kho
        inventoryManager.deductStock(order);

        // Bước 3: In hóa đơn ra màn hình
        String receiptText = ReceiptPrinter.generateReceipt(order);
        System.out.println(receiptText);
    }
}
