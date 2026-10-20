package org.example.session4.practice2.usecase.implement;

import org.example.session4.practice2.domain.Log;
import org.example.session4.practice2.domain.Task;
import org.example.session4.practice2.usecase.HistoryLog;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class InMemoryHistoryLog implements HistoryLog {
    private final LinkedList<Log> logs = new LinkedList<>();

    @Override
    public void addLog(String status, Task action) {
        Log newLog = new Log(status, action, LocalDateTime.now());
        this.logs.add(newLog);
    }

    @Override
    public List<Log> getRecent(int n) {
        List<Log> result = new ArrayList<>();
//        int i = 0;
//        while (i < n) {
//            this.logs.getLast()
//            i++;
//        }
        return result;
    }
}
