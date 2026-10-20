package org.example.session4.practice2.usecase;

import org.example.session4.practice2.domain.Task;

import java.util.List;

public interface TaskManager {

    void addTask(Task task);

    void removeTask(String id);

    Task getTask(String id);

    List<Task> listAll();
}
