package com.training.ecommerce.interfaces.rest.order;

import com.training.ecommerce.application.order.PlaceOrderCommand;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Chuyển đổi giữa DTO của order và command / domain.
 */
final class OrderMapper {

    private OrderMapper() {
    }

    /**
     * Không tự kiểm tra: lines null / rỗng / chứa null được {@code PlaceOrderCommand} từ chối bằng IAE.
     * Rate bỏ trống được hiểu là 0.
     */
    static PlaceOrderCommand toCommand(PlaceOrderRequest request) {
        List<PlaceOrderCommand.Line> lines = request.lines() == null ? null : request.lines().stream()
                .map(line -> line == null ? null : new PlaceOrderCommand.Line(line.productId(), line.quantity()))
                .toList();      // Stream.toList() cho phép phần tử null (khác List.copyOf), để command quyết định
        return new PlaceOrderCommand(
                request.discountRate() == null ? BigDecimal.ZERO : request.discountRate(),
                request.taxRate() == null ? BigDecimal.ZERO : request.taxRate(),
                lines);
    }

    static OrderResponse toResponse(Order order) {
        List<OrderResponse.Line> lines = Arrays.stream(order.getLines())
                .map(OrderMapper::toLine)
                .toList();
        return new OrderResponse(order.getId(), order.getDiscountRate(), order.getTaxRate(), lines, order.subtotal(),
                order.discountAmount(), order.taxAmount(), order.finalAmount());
    }

    private static OrderResponse.Line toLine(OrderLine line) {
        return new OrderResponse.Line(line.getProduct().getId(), line.getProduct().getName(), line.getQuantity(),
                line.getUnitPrice(), line.lineTotal());
    }
}
