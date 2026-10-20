package org.example.session4.practice2.usecase;

import org.example.session4.practice2.domain.Log;
import org.example.session4.practice2.domain.Task;

import java.util.List;

public interface HistoryLog {

    void addLog(String status, Task action);

    List<Log> getRecent(int n);
}
