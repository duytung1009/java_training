// Phép chiếu isometric + helper SVG
export const SVGNS = 'http://www.w3.org/2000/svg';
const TW = 64, TH = 32, OX = 360, OY = 56;

export const iso = (x, y) => [OX + (x - y) * TW / 2, OY + (x + y) * TH / 2];
export const diamond = (x, y, w, d) =>
  [iso(x, y), iso(x + w, y), iso(x + w, y + d), iso(x, y + d)].map(p => p.join(',')).join(' ');
export const wirePath = pts => pts.map((p, i) => (i ? 'L' : 'M') + iso(...p).join(' ')).join(' ');
export const css = name => getComputedStyle(document.documentElement).getPropertyValue(name).trim();

export function el(tag, attrs = {}, parent) {
  const e = document.createElementNS(SVGNS, tag);
  for (const [k, v] of Object.entries(attrs)) e.setAttribute(k, v);
  if (parent) parent.appendChild(e);
  return e;
}
