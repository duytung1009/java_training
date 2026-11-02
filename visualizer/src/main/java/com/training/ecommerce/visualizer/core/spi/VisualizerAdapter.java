package com.training.ecommerce.visualizer.core.spi;

import com.training.ecommerce.visualizer.core.scenario.Scenario;
import com.training.ecommerce.visualizer.core.sync.DomainApiSpec;
import com.training.ecommerce.visualizer.core.view.WorldSnapshot;

import java.util.List;

/**
 * Điểm cắm của một domain cụ thể vào lõi visualizer: dựng thế giới, chụp trạng thái, khai báo kịch bản
 * và nơi quét API công khai. Lõi chỉ biết domain qua interface này.
 *
 * @param <W> kiểu "thế giới" chứa các object domain mà kịch bản thao tác
 */
public interface VisualizerAdapter<W> {

    /** Dựng thế giới mới từ dữ liệu mẫu; mỗi lượt chạy dùng một thế giới riêng. */
    W createWorld();

    /** Chụp trạng thái hiện tại của {@code world}: cây node để vẽ và giá trị field để so sánh diff. */
    WorldSnapshot snapshot(W world);

    /** Các kịch bản theo thứ tự hiển thị trên UI. */
    List<Scenario<W>> scenarios();

    /** Package domain và file snapshot API để cảnh báo khi domain lệch. */
    DomainApiSpec domainApi();

    /**
     * Nguồn domain event của thế giới. Mặc định rỗng; domain có event thì override cùng {@link #eventMapper()}.
     */
    default DomainEventSource<W> eventSource() {
        return DomainEventSource.none();
    }

    /** Cách biến domain event thành flow trên bản đồ. Mặc định không ánh xạ event nào. */
    default DomainEventMapper eventMapper() {
        return DomainEventMapper.none();
    }
}
