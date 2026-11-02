package com.training.ecommerce.visualizer.core.web;

import com.training.ecommerce.visualizer.core.model.InvalidModelException;
import com.training.ecommerce.visualizer.core.model.ModelNotWritableException;
import com.training.ecommerce.visualizer.core.scenario.ScenarioNotFoundException;
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

    @ExceptionHandler(InvalidModelException.class)
    public ProblemDetail handleInvalidModel(InvalidModelException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setTitle("Mô hình không hợp lệ");
        problem.setProperty("errors", e.errors());
        return problem;
    }

    @ExceptionHandler(ModelNotWritableException.class)
    public ProblemDetail handleNotWritable(ModelNotWritableException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setTitle("Không ghi được mô hình");
        return problem;
    }
}
