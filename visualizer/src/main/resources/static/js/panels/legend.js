// Chú thích nổi trên bản đồ: nút "Chú thích" ở tiêu đề mở/đóng.
// Trạng thái mở lưu ở viz.legend.open (mặc định mở lần đầu); đây là bảng tham chiếu nên click ra ngoài không đóng.
// setMode('map' | 'node'): tầng bản đồ (mục .lg-iso) hay tầng instance (mục .lg-node).
import { createPopover } from './popover.js';

export function createLegend({ el, button }) {
  createPopover({ el, button, key: 'viz.legend.open', defaultOpen: true, escOnlyWhenFocusInside: true });

  return { setMode(mode) { el.querySelector('.lg-iso').hidden = mode === 'node'; el.querySelector('.lg-node').hidden = mode !== 'node'; } };
}
