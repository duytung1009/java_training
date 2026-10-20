package org.example.session4.practice2.usecase.implement;

import org.example.session4.practice2.domain.Task;
import org.example.session4.practice2.usecase.TaskManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class InMemoryTaskManager implements TaskManager {
    private final List<Task> tasks = new ArrayList<>();

    @Override
    public void addTask(Task task) {
        this.tasks.add(task);
    }

    @Override
    public void removeTask(String id) {
//        this.tasks.removeIf(item -> item.getId().equals(id));
        Iterator<Task> it = this.tasks.iterator();
        while (it.hasNext()) {
            if (it.next().getId().equals(id)) {
                it.remove();
            }
        }
    }

    @Override
    public Task getTask(String id) {
        for (Task t : tasks) {
            if (t.getId().equals(id)) {
                return t;
            }
        }
        return null;
    }

    @Override
    public List<Task> listAll() {
        return this.tasks;
    }
}
