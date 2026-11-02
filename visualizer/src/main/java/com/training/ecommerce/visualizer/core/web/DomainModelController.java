package com.training.ecommerce.visualizer.core.web;

import com.training.ecommerce.visualizer.core.model.DomainModel;
import com.training.ecommerce.visualizer.core.model.DomainModelStore;
import com.training.ecommerce.visualizer.core.model.ModelSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST API của mô hình domain: gợi ý, đọc mô hình hiện hành, lưu mô hình. */
@RestController
@RequestMapping("/api/viz/model")
public class DomainModelController {

    private final DomainModelStore store;

    /**
     * @throws IllegalArgumentException nếu store null
     */
    public DomainModelController(DomainModelStore store) {
        if (store == null) {
            throw new IllegalArgumentException("store không được null");
        }
        this.store = store;
    }

    /** Gợi ý từ reflection và quan hệ quan sát, bỏ qua file đã lưu. */
    @GetMapping("/suggestion")
    public DomainModel suggestion() {
        return store.suggestion();
    }

    /** Mô hình hiện hành kèm {@code origin} ({@code file}, {@code classpath} hoặc {@code suggestion}). */
    @GetMapping
    public ModelSnapshot model() {
        return store.current();
    }

    /**
     * Lưu mô hình. Không hợp lệ thì {@code InvalidModelException} thành 400, không ghi được file thì
     * {@code ModelNotWritableException} thành 409 (xem {@link RestExceptionHandler}).
     */
    @PutMapping
    public ModelSnapshot save(@RequestBody DomainModel model) {
        return store.save(model);
    }
}
