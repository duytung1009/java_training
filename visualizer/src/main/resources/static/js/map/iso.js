// Phép chiếu isometric + helper SVG
export const SVGNS = 'http://www.w3.org/2000/svg';
const TW = 64, TH = 32, OX = 360, OY = 56;

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
