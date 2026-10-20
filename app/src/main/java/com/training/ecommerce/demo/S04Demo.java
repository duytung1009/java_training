package com.training.ecommerce.demo;

import com.training.ecommerce.domain.catalog.DigitalProduct;
import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.ProductNotFoundException;
import com.training.ecommerce.domain.inventory.InventoryManager;
import com.training.ecommerce.domain.order.Cart;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.infrastructure.persistence.inmemory.InMemoryRepository;
import com.training.ecommerce.interfaces.cli.ReceiptPrinter;
import com.training.ecommerce.shared.repository.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Demo S04: catalog qua Repository generic, giỏ hàng cộng dồn bằng Map, domain exception.
 */
public class S04Demo {

    public static void main(String[] args) {
        Repository<Product, String> catalog = new InMemoryRepository<>();
        List<PhysicalProduct> fashion = List.of(
                new PhysicalProduct("P001", "Áo thun nam", new BigDecimal("150000"), 50, new BigDecimal("0.3")),
                new PhysicalProduct("P002", "Quần jean", new BigDecimal("450000"), 20, new BigDecimal("0.8")));
        catalog.saveAll(fashion);
        catalog.save(new DigitalProduct("D001", "Office 2024 bản quyền", new BigDecimal("1000000"), 200));
        System.out.println(">>> Catalog (" + catalog.count() + "): " + catalog.findAll());

        InventoryManager inventory = new InventoryManager();
        for (Product product : catalog.findAll()) {
            inventory.addProduct(product);
        }

        Cart cart = new Cart();
        Product tshirt = catalog.findById("P001").orElseThrow();
        cart.add(tshirt, 1);
        cart.add(catalog.findById("P002").orElseThrow(), 1);
        cart.add(tshirt, 1); // cộng dồn: áo = 2
        System.out.println(">>> Giỏ hàng:");
        for (Map.Entry<Product, Integer> item : cart.getItems().entrySet()) {
            System.out.println("    " + item.getKey().getName() + " x" + item.getValue());
        }

        try {
            cart.add(catalog.findById("P002").orElseThrow(), 20);
        } catch (OutOfStockException e) {
            System.out.println(">>> Lỗi: " + e.getMessage());
        }
        try {
            cart.changeQuantity(catalog.findById("D001").orElseThrow(), 1);
        } catch (ProductNotFoundException e) {
            System.out.println(">>> Lỗi (" + e.getProductId() + " chưa có trong giỏ): " + e.getMessage());
        }

        Order order = cart.toOrder(new BigDecimal("0.10"), new BigDecimal("0.08"));
        inventory.decreaseStock(order);
        System.out.println(new ReceiptPrinter().print(order));
        System.out.println(">>> Tồn kho áo sau khi đặt: " + tshirt.getStock());
    }
}
