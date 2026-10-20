package com.training.ecommerce.domain.inventory;

import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;

public class InventoryManager {
    private final String[] productIds;
    private final int[] quantities;
    private int count = 0;

    public InventoryManager(int capacity) {
        productIds = new String[capacity];
        quantities = new int[capacity];
    }

    public void setStock(String productId, int quantity) throws Exception {
        if (quantity < 0) {
            throw new Exception("Invalid value");
        }
        for (int i = 0; i < count; i++) {
            if (productIds[i].equals(productId)) {
                quantities[i] = quantity;
                return;
            }
        }
        productIds[count] = productId;
        quantities[count] = quantity;
        count++;
    }

    public boolean canFulfill(Order order) {
        for (OrderLine line : order.getItems()) {
            if (line.getProduct().getStock() < line.getQuantity()) {
                return false;
            }
        }
        return true;
    }

    public void deductStock(Order order) throws Exception {
        if (!canFulfill(order)) {
            throw new Exception("Not enough stock");
        }
        for (OrderLine line : order.getItems()) {
            String pId = line.getProduct().getId();
            for (int i = 0; i < count; i++) {
                if (productIds[i].equals(pId)) {
                    quantities[i] -= line.getQuantity();
                }
            }
        }
    }
}
