package com.training.ecommerce.interfaces.rest.shipping;

import jakarta.validation.constraints.NotBlank;

/**
 * Body kèm lý do cho giao thất bại / từ chối nhận.
 *
 * @param reason lý do, không rỗng
 */
public record ReasonRequest(@NotBlank(message = "reason không được rỗng") String reason) {
}
