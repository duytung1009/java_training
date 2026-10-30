// Phép chiếu bản đồ: ma trận affine [a,b,c,d,e,f] (thứ tự SVG matrix()): x' = a·x + c·y + e, y' = b·x + d·y + f
// Không đụng DOM. Mọi thứ nằm trên nền đều ở toạ độ màn hình isometric, nên chuyển phép chiếu = một ma trận áp lên cả lớp nền.
import { TW, TH, OX, OY, CELL } from './iso.js';

export const IDENTITY = [1, 0, 0, 1, 0, 0];

// Áp n trước, rồi m
export const multiply = (m, n) => [
  m[0] * n[0] + m[2] * n[1], m[1] * n[0] + m[3] * n[1],
  m[0] * n[2] + m[2] * n[3], m[1] * n[2] + m[3] * n[3],
  m[0] * n[4] + m[2] * n[5] + m[4], m[1] * n[4] + m[3] * n[5] + m[5],
];

export function invert([a, b, c, d, e, f]) {
  const det = a * d - b * c;
  if (!det) throw new Error('Ma trận suy biến, không nghịch đảo được');
  return [d / det, -b / det, -c / det, a / det, (c * f - d * e) / det, (b * e - a * f) / det];
}

export const apply = (m, [x, y]) => [m[0] * x + m[2] * y + m[4], m[1] * x + m[3] * y + m[5]];

// Hộp bao của các điểm sau khi áp m
export function bounds(m, pts) {
  const q = pts.map(p => apply(m, p)), xs = q.map(p => p[0]), ys = q.map(p => p[1]);
  const x = Math.min(...xs), y = Math.min(...ys);
  return { x, y, w: Math.max(...xs) - x, h: Math.max(...ys) - y };
}

export const lerp = (m, n, t) => m.map((v, i) => v + (n[i] - v) * t);
export const toAttr = m => `matrix(${m.join(' ')})`;

// Lưới → màn hình. iso dựng từ chính hằng số của iso.js nên không lệch nhau
export const GRID = {
  iso: [TW / 2, TH / 2, -TW / 2, TH / 2, OX, OY],
  flat: [CELL, 0, 0, CELL, 0, 0],
};
const ISO_INV = invert(GRID.iso);

// Ma trận áp lên lớp nền (vốn đã ở toạ độ iso) để ra phép chiếu `name`; 'iso' = đơn vị
export const planeMatrix = name => {
  if (!GRID[name]) throw new Error(`Phép chiếu lạ: ${name}`);
  return name === 'iso' ? IDENTITY : multiply(GRID[name], ISO_INV);
};

// ---- Camera: hàm thuần tính khung nhìn ----
// Vừa nội dung (+ lề, cùng đơn vị viewBox) vào hộp {w,h} px, giữ tỉ lệ, nội dung căn giữa; scale ≤ maxScale.
// Trả viewBox có đúng tỉ lệ của hộp (nội dung ở giữa) và kích thước svg = hộp, nên zoom 1 thì không cần cuộn.
export function fitRect(content, box, { margin = 0, maxScale = Infinity } = {}) {
  const c = { x: content.x - margin, y: content.y - margin, w: content.w + 2 * margin, h: content.h + 2 * margin };
  const s = Math.min(box.w / c.w, box.h / c.h, maxScale), w = box.w / s, h = box.h / s;
  return { viewBox: { x: c.x + c.w / 2 - w / 2, y: c.y + c.h / 2 - h / 2, w, h }, width: box.w, height: box.h };
}

// Vùng viewBox đang thấy trong hộp cuộn: svg (px) cuộn tới scroll; svg nhỏ hơn hộp thì chỉ thấy phần svg, scroll bị kẹp
export function visibleRect(viewBox, svg, scroll, box) {
  if (!svg.w || !svg.h) return { ...viewBox };
  const kx = viewBox.w / svg.w, ky = viewBox.h / svg.h, clamp = (v, max) => Math.min(Math.max(v, 0), Math.max(0, max));
  return {
    x: viewBox.x + clamp(scroll.left, svg.w - box.w) * kx, y: viewBox.y + clamp(scroll.top, svg.h - box.h) * ky,
    w: Math.min(box.w, svg.w) * kx, h: Math.min(box.h, svg.h) * ky,
  };
}

export const lerpRect = (a, b, t) => ({ x: a.x + (b.x - a.x) * t, y: a.y + (b.y - a.y) * t, w: a.w + (b.w - a.w) * t, h: a.h + (b.h - a.h) * t });
