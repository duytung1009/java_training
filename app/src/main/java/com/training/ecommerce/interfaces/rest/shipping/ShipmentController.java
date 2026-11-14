package com.training.ecommerce.interfaces.rest.shipping;

import com.training.ecommerce.application.shipping.ShipmentService;
import com.training.ecommerce.domain.shipping.Shipment;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Tạo vận đơn, tra cứu và đẩy vận đơn qua vòng đời giao hàng.
 */
@RestController
@RequestMapping("/api/shipments")
@Tag(name = "Shipment", description = "Vận đơn và giao hàng")
public class ShipmentController {
    private final ShipmentService service;

    public ShipmentController(ShipmentService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Tạo vận đơn cho đơn: phí tính theo cả đơn và địa chỉ giao")
    @ApiResponse(responseCode = "201", description = "Đã tạo, header Location trỏ tới vận đơn")
    @ApiResponse(responseCode = "400", description = "Body không hợp lệ, hoặc đơn không có hàng vật lý")
    @ApiResponse(responseCode = "404", description = "Không có đơn với orderId này")
    @ApiResponse(responseCode = "409", description = "Đơn đã có vận đơn còn hiệu lực (khác CANCELLED / RETURNED)")
    public ResponseEntity<ShipmentResponse> create(@Valid @RequestBody CreateShipmentRequest request) {
        Shipment shipment = service.create(request.orderId(), ShipmentMapper.toAddress(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(shipment.getId()).toUri();
        return ResponseEntity.created(location).body(ShipmentMapper.toResponse(shipment));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy vận đơn theo id")
    @ApiResponse(responseCode = "404", description = "Không có vận đơn với id này")
    public ShipmentResponse get(@PathVariable String id) {
        return ShipmentMapper.toResponse(service.findById(id));
    }

    @GetMapping
    @Operation(summary = "Danh sách vận đơn của một đơn theo thứ tự tạo")
    public List<ShipmentResponse> listByOrder(@RequestParam String orderId) {
        return service.findByOrderId(orderId).stream().map(ShipmentMapper::toResponse).toList();
    }

    @PostMapping("/{id}/pick-up")
    @Operation(summary = "Lấy hàng: CREATED → IN_TRANSIT")
    @ApiResponse(responseCode = "404", description = "Không có vận đơn với id này")
    @ApiResponse(responseCode = "409", description = "Vận đơn không ở CREATED")
    public ShipmentResponse pickUp(@PathVariable String id) {
        return ShipmentMapper.toResponse(service.pickUp(id));
    }

    @PostMapping("/{id}/deliver")
    @Operation(summary = "Giao thành công: IN_TRANSIT → DELIVERED (không hoàn kho)")
    @ApiResponse(responseCode = "404", description = "Không có vận đơn với id này")
    @ApiResponse(responseCode = "409", description = "Vận đơn không ở IN_TRANSIT")
    public ShipmentResponse deliver(@PathVariable String id) {
        return ShipmentMapper.toResponse(service.deliver(id));
    }

    @PostMapping("/{id}/fail")
    @Operation(summary = "Giao thất bại một lần; lần thứ 3 tự chuyển RETURNING")
    @ApiResponse(responseCode = "400", description = "Lý do rỗng")
    @ApiResponse(responseCode = "404", description = "Không có vận đơn với id này")
    @ApiResponse(responseCode = "409", description = "Vận đơn không ở IN_TRANSIT")
    public ShipmentResponse fail(@PathVariable String id, @Valid @RequestBody ReasonRequest request) {
        return ShipmentMapper.toResponse(service.failAttempt(id, request.reason()));
    }

    @PostMapping("/{id}/refuse")
    @Operation(summary = "Người nhận từ chối: IN_TRANSIT → RETURNING")
    @ApiResponse(responseCode = "400", description = "Lý do rỗng")
    @ApiResponse(responseCode = "404", description = "Không có vận đơn với id này")
    @ApiResponse(responseCode = "409", description = "Vận đơn không ở IN_TRANSIT")
    public ShipmentResponse refuse(@PathVariable String id, @Valid @RequestBody ReasonRequest request) {
        return ShipmentMapper.toResponse(service.refuse(id, request.reason()));
    }

    @PostMapping("/{id}/return")
    @Operation(summary = "Hàng đã về kho: RETURNING → RETURNED, hoàn kho trong cùng transaction")
    @ApiResponse(responseCode = "404", description = "Không có vận đơn với id này")
    @ApiResponse(responseCode = "409", description = "Vận đơn không ở RETURNING; kho không đổi")
    public ShipmentResponse markReturned(@PathVariable String id) {
        return ShipmentMapper.toResponse(service.markReturned(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Huỷ vận đơn (chỉ ở CREATED), hoàn kho trong cùng transaction")
    @ApiResponse(responseCode = "404", description = "Không có vận đơn với id này")
    @ApiResponse(responseCode = "409", description = "Vận đơn không ở CREATED; kho không đổi")
    public ShipmentResponse cancel(@PathVariable String id) {
        return ShipmentMapper.toResponse(service.cancel(id));
    }
}
