package com.training.ecommerce.interfaces.rest.shipping;

/**
 * Body kèm lý do cho giao thất bại / từ chối nhận.
 *
 * @param reason lý do, không rỗng
 */
public record ReasonRequest(String reason) {
}
