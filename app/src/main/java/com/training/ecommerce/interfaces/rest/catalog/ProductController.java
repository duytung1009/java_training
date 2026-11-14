package com.training.ecommerce.interfaces.rest.catalog;

import com.training.ecommerce.application.catalog.ProductFactory;
import com.training.ecommerce.application.catalog.ProductService;
import com.training.ecommerce.domain.catalog.Product;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * CRUD product. Lỗi được {@code RestExceptionHandler} chuyển thành ProblemDetail.
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Product", description = "Quản lý catalog sản phẩm")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Danh sách product theo thứ tự thêm")
    public List<ProductResponse> list() {
        return service.findAll().stream().map(ProductMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy product theo id")
    @ApiResponse(responseCode = "404", description = "Không có product với id này")
    public ProductResponse get(@PathVariable String id) {
        return ProductMapper.toResponse(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Tạo product (id trong body); id đã có thì bị ghi đè")
    @ApiResponse(responseCode = "201", description = "Đã tạo, header Location trỏ tới product")
    @ApiResponse(responseCode = "400", description = "Body sai field (errors liệt kê từng field), hoặc JSON không đọc được")
    @ApiResponse(responseCode = "404", description = "Item của bundle không tồn tại")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        Product saved = service.save(ProductFactory.create(request, request.id(), service::findById));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(ProductMapper.toResponse(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật product; id trên path được ưu tiên")
    @ApiResponse(responseCode = "400", description = "Body sai field, hoặc id trong body khác id trên path")
    @ApiResponse(responseCode = "404", description = "Không có product (hoặc item bundle) với id này")
    public ProductResponse update(@PathVariable String id, @Valid @RequestBody ProductRequest request) {
        if (request.id() != null && !request.id().equals(id)) {
            throw new IllegalArgumentException("id trong body (" + request.id() + ") khác id trên path (" + id + ")");
        }
        return ProductMapper.toResponse(service.update(id, ProductFactory.create(request, id, service::findById)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa product")
    @ApiResponse(responseCode = "204", description = "Đã xóa")
    @ApiResponse(responseCode = "404", description = "Không có product với id này")
    @ApiResponse(responseCode = "409", description = "Product đang là item của bundle hoặc nằm trong đơn hàng (ProductInUseException)")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
