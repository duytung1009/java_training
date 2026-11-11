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
// Đường gấp khúc "M x y L x y …" từ các điểm đã ở toạ độ màn hình; wirePath nhận điểm lưới rồi chiếu iso
export const pathD = pts => pts.map((p, i) => (i ? 'L' : 'M') + p[0] + ' ' + p[1]).join(' ');
export const wirePath = pts => pathD(pts.map(p => iso(...p)));
// 4 góc của hình chữ nhật { x, y, w, d | h }
export const corners = r => { const d = r.d ?? r.h; return [[r.x, r.y], [r.x + r.w, r.y], [r.x + r.w, r.y + d], [r.x, r.y + d]]; };
export const css = name => getComputedStyle(document.documentElement).getPropertyValue(name).trim();

export function el(tag, attrs = {}, parent) {
  const e = document.createElementNS(SVGNS, tag);
  for (const [k, v] of Object.entries(attrs)) e.setAttribute(k, v);
  if (parent) parent.appendChild(e);
  return e;
}

export const sleep = ms => new Promise(r => setTimeout(r, ms));

// Mũi tên cuối đường flow: colors = { flow: màu, err: màu, ... } → <marker id="{idPrefix}{kind}">; orient mặc định 'auto'
export function arrowMarkers(defs, idPrefix, colors, { orient = 'auto' } = {}) {
  for (const [kind, color] of Object.entries(colors)) {
    const m = el('marker', { id: `${idPrefix}${kind}`, viewBox: '0 0 10 10', refX: 7, refY: 5, markerWidth: 6, markerHeight: 6, orient }, defs);
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

// Hình chữ nhật màn hình (px viewport) của phần tử SVG; null nếu không có hoặc đang ẩn (rộng 0)
export function screenRect(node) {
  const r = node?.getBoundingClientRect();
  return r && r.width ? { left: r.left, top: r.top, width: r.width, height: r.height } : null;
}

// Đường flow ở overlay: event → nét chấm --event kèm nhãn tên event (luôn giữ màu event kể cả khi lời gọi THROWN, vì event đã
// thật sự xảy ra trước lúc ném); lời gọi → --flow, hoặc --err khi lỗi, nhãn (nếu có) là dữ liệu chảy theo lời gọi, vd. orderId.
// kind = f.kind ('EVENT' | 'CALL'); trả { p, label }
export function drawFlow(layer, { kind, ok, label, markerPrefix, colors }) {
  const isEvent = kind === 'EVENT', k = isEvent ? 'event' : ok ? 'flow' : 'err';
  const p = el('path', { class: isEvent ? 'flow event' : 'flow', stroke: colors[k], 'marker-end': `url(#${markerPrefix}${k})` }, layer);
  let text = null;
  if (label) {
    text = el('text', { class: isEvent ? 'flow-label' : 'flow-label call', 'text-anchor': 'middle' }, layer);
    text.textContent = label;
  }
  return { p, label: text };
}

// Đặt đường flow theo điểm màn hình; nhãn nằm trên giữa đường 5px
export function placeFlowPath({ p, label }, screenPts) {
  p.setAttribute('d', pathD(screenPts));
  if (!label) return;
  const mid = p.getPointAtLength(p.getTotalLength() / 2);
  label.setAttribute('x', mid.x); label.setAttribute('y', mid.y - 5);
}

// Phần tử bấm được bằng chuột + bàn phím (tabindex 0): click và Enter (không lặp) gọi onActivate(e), đều chặn nổi bọt.
// click: hàm riêng cho click (false = không gắn → click nổi lên phần tử cha); dbl: true = chỉ chặn dblclick nổi bọt, hàm = chặn + gọi
export function activatable(g, onActivate, { click = onActivate, dbl = null } = {}) {
  g.setAttribute('tabindex', 0);
  if (click) g.addEventListener('click', e => { e.stopPropagation(); click(e); });
  if (dbl) g.addEventListener('dblclick', e => { e.stopPropagation(); if (dbl !== true) dbl(e); });
  g.addEventListener('keydown', e => { if (e.key === 'Enter' && !e.repeat) { e.stopPropagation(); e.preventDefault(); onActivate(e); } });
}
