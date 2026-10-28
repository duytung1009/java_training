package com.training.ecommerce.application.order;

import com.training.ecommerce.application.payment.PaymentGateway;
import com.training.ecommerce.application.payment.PaymentInfo;
import com.training.ecommerce.application.shipping.ShipmentInfo;
import com.training.ecommerce.application.shipping.ShipmentTracker;
import com.training.ecommerce.domain.order.OrderLine;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * S06: tải chi tiết đơn bằng cách gọi song song 3 nguồn trên executor được inject.
 * Service không tạo và không đóng executor: bên nào tạo thì bên đó đóng.
 *
 * <p>Inject executor nên cùng một service chạy được trên {@code newFixedThreadPool} hay
 * {@code newVirtualThreadPerTaskExecutor()} mà không sửa code; test cũng tự chọn executor được.
 *
 * <p>Giới hạn: {@code get()} chờ theo thứ tự payment → shipment → lines. Nếu lines lỗi ngay mà shipment chậm thì vẫn
 * phải chờ shipment xong mới biết lỗi. Muốn "lỗi đâu báo đó" thì dùng {@code ExecutorCompletionService}
 * hoặc structured concurrency (JEP 505, còn preview ở Java 25).
 */
public class OrderDetailService {
    private final PaymentGateway payments;
    private final ShipmentTracker shipments;
    private final OrderLineSource lines;
    private final ExecutorService executor;

    /**
     * @throws IllegalArgumentException khi một tham số null
     */
    public OrderDetailService(PaymentGateway payments, ShipmentTracker shipments, OrderLineSource lines,
                              ExecutorService executor) {
        if (payments == null || shipments == null || lines == null || executor == null) {
            throw new IllegalArgumentException("payments, shipments, lines, executor không được null");
        }
        this.payments = payments;
        this.shipments = shipments;
        this.lines = lines;
        this.executor = executor;
    }

    /**
     * Gọi 3 nguồn song song rồi gộp kết quả. Tổng thời gian ≈ nguồn chậm nhất.
     *
     * @throws IllegalArgumentException  khi orderId null hoặc rỗng
     * @throws OrderDetailLoadException khi một nguồn lỗi (cause = exception gốc), hoặc thread gọi bị interrupt
     *                                   (cờ interrupt vẫn được giữ)
     */
    public OrderDetail loadDetail(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId không được rỗng");
        }
        // submit cả 3 trước rồi mới get(): 3 task chạy song song ngay từ lúc submit
        Future<PaymentInfo> payment = executor.submit(() -> payments.findPayment(orderId));
        Future<ShipmentInfo> shipment = executor.submit(() -> shipments.track(orderId));
        Future<List<OrderLine>> orderLines = executor.submit(() -> lines.findLines(orderId));
        List<Future<?>> all = List.of(payment, shipment, orderLines);

        try {
            return new OrderDetail(orderId, payment.get(), shipment.get(), orderLines.get());
        } catch (ExecutionException e) {
            cancelAll(all);                 // một nguồn hỏng thì kết quả các nguồn khác cũng vô dụng
            throw new OrderDetailLoadException("Không tải được chi tiết đơn " + orderId, e.getCause());
        } catch (InterruptedException e) {
            cancelAll(all);
            Thread.currentThread().interrupt();   // không nuốt tín hiệu dừng: bật lại cờ cho caller
            throw new OrderDetailLoadException("Bị dừng khi đang tải chi tiết đơn " + orderId, e);
        }
    }

    /** cancel(true): task chưa chạy thì không chạy nữa, task đang chạy thì bị interrupt. */
    private static void cancelAll(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            future.cancel(true);
        }
    }
}
