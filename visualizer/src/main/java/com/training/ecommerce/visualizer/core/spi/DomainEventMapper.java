package com.training.ecommerce.visualizer.core.spi;

import java.util.Optional;

/** Chuyển một domain event thành flow trên bản đồ; event không có flow thì trả {@link Optional#empty()}. */
@FunctionalInterface
public interface DomainEventMapper {

    /**
     * @param event event domain (không null)
     * @return flow tương ứng, rỗng nếu event chỉ hiện trong log
     */
    Optional<EventFlow> map(Object event);

    /** Mapper không ánh xạ event nào. */
    static DomainEventMapper none() {
        return event -> Optional.empty();
    }
}
