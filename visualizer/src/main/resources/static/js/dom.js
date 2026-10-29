// Helper tạo phần tử HTML an toàn (dùng textContent, không innerHTML)
export function h(tag, cls, text) {
  const e = document.createElement(tag);
  if (cls) e.className = cls;
  if (text !== undefined && text !== null) e.textContent = text;
  return e;
}
