// Chú thích nổi trên bản đồ: nút "Chú thích" ở tiêu đề mở/đóng.
// Trạng thái mở lưu ở viz.legend.open (mặc định mở lần đầu); đây là bảng tham chiếu nên click ra ngoài không đóng.
import { readJson, writeJson } from '../storage.js';

const KEY = 'viz.legend.open';

export function createLegend({ el, button }) {
  let open = readJson(KEY) !== false;

  function render() {
    el.hidden = !open;
    button.setAttribute('aria-expanded', String(open));
  }
  function setOpen(v) { open = v; writeJson(KEY, open); render(); }

  button.addEventListener('click', () => setOpen(!open));
  el.querySelector('.lg-close').addEventListener('click', () => { setOpen(false); button.focus(); });
  document.addEventListener('keydown', e => {
    // Bảng tham chiếu để mở lâu: Esc chỉ đóng khi focus nằm trong bảng, còn lại Esc dành cho lùi tầng bản đồ
    if (e.key === 'Escape' && open && !e.defaultPrevented && el.contains(document.activeElement)) { e.preventDefault(); setOpen(false); button.focus(); }
  });
  render();
}
