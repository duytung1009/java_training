// Bảng nổi mở/đóng bằng một nút (chú thích, bộ lọc): trạng thái mở lưu ở localStorage[key], el.hidden, aria-expanded,
// nút × (.fp-close trong el) đóng và trả focus về nút, Esc đóng (preventDefault).
// defaultOpen: mở lần đầu khi chưa có giá trị đã lưu. escOnlyWhenFocusInside: Esc chỉ đóng khi focus nằm trong bảng (bảng tham chiếu để mở lâu,
// còn lại Esc dành cho lùi tầng bản đồ). closeOnOutside: pointerdown ngoài bảng và nút cũng đóng. onToggle(open): sau mỗi lần đổi trạng thái
import { readJson, writeJson } from '../storage.js';

export function createPopover({ el, button, key, defaultOpen = false, escOnlyWhenFocusInside = false, closeOnOutside = false, onToggle = () => {} }) {
  const saved = readJson(key);
  let open = defaultOpen ? saved !== false : saved === true;

  function render() {
    el.hidden = !open;
    button.setAttribute('aria-expanded', String(open));
  }
  function setOpen(v) {
    if (open === v) return;
    open = v; writeJson(key, open); render(); onToggle(open);
  }
  const close = () => { setOpen(false); button.focus(); };

  button.addEventListener('click', () => setOpen(!open));
  el.querySelector('.fp-close')?.addEventListener('click', close);
  document.addEventListener('keydown', e => {
    if (e.key !== 'Escape' || !open || e.defaultPrevented) return;
    if (escOnlyWhenFocusInside && !el.contains(document.activeElement)) return;
    e.preventDefault(); close();
  });
  if (closeOnOutside) document.addEventListener('pointerdown', e => {
    if (open && e.target instanceof Node && !el.contains(e.target) && !button.contains(e.target)) setOpen(false);
  });
  render();

  return { isOpen: () => open, setOpen };
}
