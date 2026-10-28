package com.training.ecommerce.application;

import java.util.concurrent.ExecutorService;

/**
 * S06: tải chi tiết đơn bằng cách gọi song song 3 nguồn trên executor được inject.
 * Service không tạo và không đóng executor: bên nào tạo thì bên đó đóng.
 */
public class OrderDetailService {

    /**
     * @throws IllegalArgumentException khi một tham số null
     */
    public OrderDetailService(PaymentGateway payments, ShipmentTracker shipments, OrderLineSource lines,
                              ExecutorService executor) {
        throw new UnsupportedOperationException("TODO S06");
    }

    /**
     * Gọi 3 nguồn song song rồi gộp kết quả.
     *
     * @throws IllegalArgumentException  khi orderId null hoặc rỗng
     * @throws OrderDetailLoadException khi một nguồn lỗi (cause = exception gốc), hoặc thread gọi bị interrupt
     *                                   (cờ interrupt vẫn được giữ)
     */
    public OrderDetail loadDetail(String orderId) {
        throw new UnsupportedOperationException("TODO S06");
    }
}
