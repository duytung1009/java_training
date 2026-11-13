package com.training.ecommerce.interfaces.rest;

import com.training.ecommerce.application.catalog.ProductInUseException;
import com.training.ecommerce.application.order.OrderNotFoundException;
import com.training.ecommerce.application.shipping.ActiveShipmentExistsException;
import com.training.ecommerce.application.shipping.ShipmentNotFoundException;
import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.catalog.ProductNotFoundException;
import com.training.ecommerce.domain.shipping.IllegalShipmentTransitionException;
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
 * <p>{@code ProductNotFoundException} kế thừa {@code IllegalArgumentException} và {@code OutOfStockException} kế thừa
 * {@code IllegalStateException}; Spring chọn handler gần exception nhất nên 404 / 409 thắng 400.
 * Vận đơn: chuyển trạng thái sai / đơn đã có vận đơn còn hiệu lực cũng là 409. ISE chung (dữ liệu hỏng) không map ở
 * đây, vẫn là 500.
 * Bean Validation (@Valid) lỗi → 400 kèm danh sách {@code errors} theo field; JSON sai kiểu / enum lạ vẫn là
 * "Body JSON không hợp lệ".
 */
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler({ProductNotFoundException.class, OrderNotFoundException.class, ShipmentNotFoundException.class})
    ProblemDetail notFound(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({OutOfStockException.class, ProductInUseException.class,
            IllegalShipmentTransitionException.class, ActiveShipmentExistsException.class})
    ProblemDetail conflict(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
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
