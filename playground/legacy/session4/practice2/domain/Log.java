package org.example.session4.practice2.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class Log {
    private String status;
    private Task task;
    private LocalDateTime timestamp;
}