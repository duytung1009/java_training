package com.training.ecommerce.visualizer.world;

import java.util.List;

/**
 * Ảnh chụp thế giới để vẽ: cây node dạng phẳng, pre-order (mỗi node gốc rồi các con ngay sau nó).
 *
 * @param nodes các node theo thứ tự cố định
 */
public record WorldView(List<NodeView> nodes) {
}
