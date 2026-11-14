package com.training.ecommerce.interfaces.rest;

import com.training.ecommerce.shared.error.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Comparator;
import java.util.List;

/**
 * Chuyển exception của domain / application thành {@link ProblemDetail} (RFC 9457).
 *
 * <p>Mọi {@link BusinessException} được map một chỗ theo {@code Category} (NOT_FOUND → 404, CONFLICT → 409) và
 * kèm property {@code code} ổn định để client xử lý bằng máy. Exception kỹ thuật / ISE chung (dữ liệu hỏng) không
 * map ở đây, vẫn là 500; {@code IllegalArgumentException} thuần là 400.
 * Bean Validation (@Valid) lỗi → 400 kèm danh sách {@code errors} theo field; JSON sai kiểu / enum lạ vẫn là
 * "Body JSON không hợp lệ".
 */
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    ProblemDetail business(BusinessException e) {
        HttpStatus status = switch (e.getCategory()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        problem.setProperty("code", e.getCode());
        return problem;
    }

    /** Vi phạm ràng buộc DB không có exception riêng (ví dụ ghi bundle trỏ tới item không tồn tại). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail dataIntegrity(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Dữ liệu vi phạm ràng buộc");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail malformedBody(HttpMessageNotReadableException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Body JSON không hợp lệ");
    }

    /** Một lỗi của request: {@code field} là đường dẫn như {@code weightKg}, {@code lines[1].quantity}. */
    public record FieldViolation(String field, String message) {
    }

    /** Gom mọi lỗi field (và lỗi mức object) vào một response, sắp theo field để kết quả ổn định. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidRequest(MethodArgumentNotValidException e) {
        List<FieldViolation> errors = e.getBindingResult().getAllErrors().stream()
                .map(error -> new FieldViolation(
                        error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName(),
                        error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldViolation::field).thenComparing(FieldViolation::message))
                .toList();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request không hợp lệ");
        problem.setProperty("errors", errors);
        return problem;
    }
}
