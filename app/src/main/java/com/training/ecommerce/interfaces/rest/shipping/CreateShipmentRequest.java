package com.training.ecommerce.interfaces.rest.shipping;

/**
 * Body tạo vận đơn.
 *
 * @param orderId id đơn cần giao
 * @param street  địa chỉ giao
 * @param city    thành phố giao (quyết định vùng phí ship)
 */
public record CreateShipmentRequest(String orderId, String street, String city) {
}
