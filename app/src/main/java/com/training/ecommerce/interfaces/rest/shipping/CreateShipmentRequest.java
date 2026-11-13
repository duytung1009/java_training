package com.training.ecommerce.interfaces.rest.shipping;

import jakarta.validation.constraints.NotBlank;

/**
 * Body tạo vận đơn.
 *
 * @param orderId id đơn cần giao
 * @param street  địa chỉ giao
 * @param city    thành phố giao (quyết định vùng phí ship)
 */
public record CreateShipmentRequest(
        @NotBlank(message = "orderId không được rỗng") String orderId,
        @NotBlank(message = "street không được rỗng") String street,
        @NotBlank(message = "city không được rỗng") String city) {
}
