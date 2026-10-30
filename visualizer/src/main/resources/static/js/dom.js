// Helper tạo phần tử HTML an toàn (dùng textContent, không innerHTML)
export function h(tag, cls, text) {
  const e = document.createElement(tag);
  if (cls) e.className = cls;
  if (text !== undefined && text !== null) e.textContent = text;
  return e;
}

// Cắt chuỗi dài hơn max thành (max-1) ký tự + "…"
export const ellipsize = (s, max) => (s.length > max ? s.slice(0, max - 1) + '…' : s);

// <select> từ [[value, text], ...]; chọn xong gọi onPick(value)
export function selectEl(options, value, onPick, ariaLabel) {
  const s = h('select');
  if (ariaLabel) s.setAttribute('aria-label', ariaLabel);
  for (const [v, text] of options) {
    const o = h('option', '', text);
    o.value = v;
    s.appendChild(o);
  }
  s.value = value;
  if (onPick) s.addEventListener('change', () => onPick(s.value));
  return s;
}
