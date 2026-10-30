package com.training.ecommerce.visualizer.core.spi;

import java.util.List;

/**
 * Nguồn domain event của một thế giới: trả các event domain đã ghi từ lần rút trước và xoá chúng đi
 * (kiểu {@code pullDomainEvents}). Lõi gọi sau mỗi lời gọi domain, kể cả khi lời gọi ném exception.
 *
 * @param <W> kiểu "thế giới" mà adapter dùng
 */
@FunctionalInterface
public interface DomainEventSource<W> {

    /**
     * Rút các event domain đã ghi kể từ lần rút trước, theo thứ tự phát sinh, rồi xoá khỏi nguồn.
     *
     * @return danh sách event (không null; rỗng nếu chưa có gì)
     */
    List<?> drain(W world);

    /** Nguồn không bao giờ có event (domain không dùng domain event). */
    static <W> DomainEventSource<W> none() {
        return world -> List.of();
    }
}
