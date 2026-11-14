package com.training.ecommerce.interfaces.rest.order;

import com.training.ecommerce.application.order.OrderService;
import com.training.ecommerce.domain.order.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Đặt đơn và tra cứu đơn.
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order", description = "Đặt và tra cứu đơn hàng")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Đặt đơn: trừ kho và lưu đơn trong một transaction")
    @ApiResponse(responseCode = "201", description = "Đã tạo, header Location trỏ tới đơn")
    @ApiResponse(responseCode = "400", description = "Body không hợp lệ (rate, lines rỗng, quantity <= 0...)")
    @ApiResponse(responseCode = "404", description = "Có productId không tồn tại")
    @ApiResponse(responseCode = "409", description = "Không đủ hàng; kho và đơn không đổi")
    public ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest request) {
        Order order = service.placeOrder(OrderMapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(order.getId()).toUri();
        return ResponseEntity.created(location).body(OrderMapper.toResponse(order));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy đơn theo id")
    @ApiResponse(responseCode = "404", description = "Không có đơn với id này")
    public OrderResponse get(@PathVariable String id) {
        return OrderMapper.toResponse(service.findById(id));
    }

    @GetMapping
    @Operation(summary = "Danh sách đơn theo thứ tự đặt")
    public List<OrderResponse> list() {
        return service.findAll().stream().map(OrderMapper::toResponse).toList();
    }
}
