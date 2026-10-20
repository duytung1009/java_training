package org.example.session4;

import org.example.session4.practice1.Product;
import org.example.session4.practice2.domain.Task;
import org.example.session4.practice2.usecase.HistoryLog;
import org.example.session4.practice2.usecase.TaskManager;
import org.example.session4.practice2.usecase.implement.InMemoryHistoryLog;
import org.example.session4.practice2.usecase.implement.InMemoryTaskManager;

import java.util.List;

public class Session4Main {

    static void main(String[] args) {
//        List<Product> productList = new ArrayList<>();
//        Laptop laptop1 = new Laptop(25000000, "Macbook Air", "13 inch");
//        productList.add(laptop1);   // Hàm tiện ích có sẵn của List
//
//        Phone phone1 = new Phone(15000000, "Iphone 15", "");
//        addItemToList(productList, phone1);    // Write <? super Product>
//
//        Laptop laptop2 = new Laptop(30000000, "Macbook Pro", "16 inch");
//        addItemToList(productList, laptop2);   // Write <? super Product>
//
//        double total = calculatePrice(productList);    // Read <? extends Product>
//        IO.println("[calculatePrice] total: " + total);

        TaskManager taskManager = new InMemoryTaskManager();
        HistoryLog historyLog = new InMemoryHistoryLog();
        for (int i = 0; i < 10000; i++) {
            Task newTask = new Task(String.valueOf(i), "Task " + i);
            taskManager.addTask(newTask);
            historyLog.addLog("OPEN", newTask);
        }

        Task task100 = taskManager.getTask("100");
        IO.println("[listAll] Tasks size: " + taskManager.listAll().size());
        IO.println("[getTask] task id 100: " + task100.getName());
    }

    // Write
    static <T> void addItemToList(List<? super T> list, T newItem) {
        list.add(newItem);
    }

    // Read
    static double calculatePrice(List<? extends Product> products) {
        double total = 0;
        for (Product p : products) {
            total += p.getPrice();
        }
        return total;
    }

}
