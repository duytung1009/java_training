// Phép chiếu isometric + helper SVG
export const SVGNS = 'http://www.w3.org/2000/svg';
export const TW = 64, TH = 32, OX = 360, OY = 56;
export const CELL = Math.hypot(TW / 2, TH / 2); // độ dài 1 ô lưới dọc trục x trên màn hình (px)

export const iso = (x, y) => [OX + (x - y) * TW / 2, OY + (x + y) * TH / 2];
export const diamond = (x, y, w, d) =>
  [iso(x, y), iso(x + w, y), iso(x + w, y + d), iso(x, y + d)].map(p => p.join(',')).join(' ');
// Chữ nằm phẳng trên nền: chạy theo trục x của lưới (along='x') hoặc trục y, đặt tâm tại (cx, cy)
export const flat = (cx, cy, along = 'x') => along === 'x'
  ? `matrix(0.894 0.447 -0.894 0.447 ${cx} ${cy})`
  : `matrix(0.894 -0.447 0.894 0.447 ${cx} ${cy})`;
export const wirePath = pts => pts.map((p, i) => (i ? 'L' : 'M') + iso(...p).join(' ')).join(' ');
export const css = name => getComputedStyle(document.documentElement).getPropertyValue(name).trim();

export function el(tag, attrs = {}, parent) {
  const e = document.createElementNS(SVGNS, tag);
  for (const [k, v] of Object.entries(attrs)) e.setAttribute(k, v);
  if (parent) parent.appendChild(e);
  return e;
}

export const sleep = ms => new Promise(r => setTimeout(r, ms));

// Mũi tên cuối đường flow: colors = { flow: màu, err: màu, ... } → <marker id="{idPrefix}{kind}">
export function arrowMarkers(defs, idPrefix, colors) {
  for (const [kind, color] of Object.entries(colors)) {
    const m = el('marker', { id: `${idPrefix}${kind}`, viewBox: '0 0 10 10', refX: 7, refY: 5, markerWidth: 6, markerHeight: 6, orient: 'auto' }, defs);
    el('path', { d: 'M1 1L8 5L1 9', fill: 'none', stroke: color, 'stroke-width': 1.6 }, m);
  }
}

// Nháy lại từ đầu: gỡ class, ép layout (getBBox) để animation chạy lại, rồi thêm class theo kết quả
export function restartFlash(node, ok) {
  node.classList.remove('flash-ok', 'flash-err'); void node.getBBox();
  node.classList.add(ok ? 'flash-ok' : 'flash-err');
}

// Mờ dần rồi gỡ nút khỏi DOM sau 300ms; bỏ qua nút null
export function fadeOut(...nodes) {
  for (const n of nodes) if (n) { n.style.transition = 'opacity .3s'; n.style.opacity = 0; setTimeout(() => n.remove(), 300); }
}
