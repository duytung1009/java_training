// Nút chuyển phép chiếu bản đồ isometric: 2.5D (iso) | Phẳng (flat). Lựa chọn lưu ở viz.proj.
import { h } from '../dom.js';
import { readJson, writeJson } from '../storage.js';

const KEY = 'viz.proj';
const OPTIONS = [['iso', '2.5D'], ['flat', 'Phẳng']];

export function createProjectionToggle({ el, onChange = () => {} }) {
  const saved = readJson(KEY);
  let value = OPTIONS.some(([v]) => v === saved) ? saved : 'iso';
  const btns = OPTIONS.map(([v, text]) => {
    const b = h('button', 'lv-btn', text);
    b.type = 'button';
    b.addEventListener('click', () => {
      if (v === value) return;
      value = v; writeJson(KEY, v); render(); onChange(v);
    });
    el.append(b);
    return [v, b];
  });
  function render() {
    for (const [v, b] of btns) { b.classList.toggle('on', v === value); b.setAttribute('aria-pressed', String(v === value)); }
  }
  render();

  return { get: () => value, setVisible(v) { el.hidden = !v; } };
}
