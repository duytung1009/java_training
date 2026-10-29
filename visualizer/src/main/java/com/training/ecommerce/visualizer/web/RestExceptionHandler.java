package com.training.ecommerce.visualizer.web;

import com.training.ecommerce.visualizer.scenario.ScenarioNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Đổi exception thành {@link ProblemDetail}. */
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(ScenarioNotFoundException.class)
    public ProblemDetail handleNotFound(ScenarioNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Không tìm thấy kịch bản");
        return problem;
    }
}
