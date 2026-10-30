// Chú thích nổi trên bản đồ: nút "Chú thích" ở tiêu đề mở/đóng. Bản đồ isometric có bộ chú thích riêng (.lg-iso),
// các bản đồ 2D (context, aggregate, instance thu gọn) có phần ký hiệu nét/khung (.lg-2d); setFlat chọn phần nào hiện.
// Trạng thái mở lưu ở viz.legend.open (mặc định mở lần đầu); đây là bảng tham chiếu nên click ra ngoài không đóng.
import { readJson, writeJson } from '../storage.js';

const KEY = 'viz.legend.open';

export function createLegend({ el, button }) {
  let open = readJson(KEY) !== false, flat = false;
  const iso = el.querySelector('.lg-iso'), notation = el.querySelector('.lg-2d');

  function render() {
    el.hidden = !open;
    iso.hidden = flat;
    notation.hidden = !flat;
    button.setAttribute('aria-expanded', String(open));
  }
  function setOpen(v) { open = v; writeJson(KEY, open); render(); }

  button.addEventListener('click', () => setOpen(!open));
  el.querySelector('.lg-close').addEventListener('click', () => { setOpen(false); button.focus(); });
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape' && open && !e.defaultPrevented) { setOpen(false); button.focus(); }
  });
  render();

  return {
    isShown: () => open,
    setFlat(v) { if (v !== flat) { flat = v; render(); } },
  };
}
