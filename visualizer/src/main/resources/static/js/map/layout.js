// Bố cục bản đồ thuộc FE (plan D7): BE chỉ gửi panel id + flow {from, to}.
// Toạ độ theo ô lưới isometric.
export const PANELS = {
  'warehouse:WH-HN':  { x: 0,   y: 0,    w: 3.4, d: 3.15, color: '--wh',    title: 'Warehouse WH-HN',  sub: 'Map<productId, qty>' },
  'warehouse:WH-HCM': { x: 0,   y: 4.95, w: 3.4, d: 3.15, color: '--wh',    title: 'Warehouse WH-HCM', sub: 'Map<productId, qty>' },
  hub:                { x: 5.5, y: 1.5,  w: 3.4, d: 3.75, color: '--hub',   title: 'InventoryManager', sub: 'Product.stock' },
  cart:               { x: 10,  y: 0,    w: 3,   d: 3.15, color: '--store', title: 'Cart',             sub: 'Map<Product, qty>' },
  pricing:            { x: 10,  y: 4.15, w: 5.1, d: 4.35, color: '--price', title: 'Pricing',          sub: 'PricingRule · DiscountCode', tileW: 1.4 },
  order:              { x: 10,  y: 9,    w: 3,   d: 2.55, color: '--store', title: 'Order',            sub: 'snapshot giá', tileW: 2.4 },
  'zone:INNER_CITY':  { x: 10,  y: 12.65, w: 3,  d: 2.15, color: '--zone',  title: 'INNER_CITY',       sub: 'Hà Nội', tileW: 2.4 },
  'zone:OTHER_CITY':  { x: 5.5, y: 12.65, w: 3,  d: 2.15, color: '--zone',  title: 'OTHER_CITY',       sub: 'tỉnh khác', tileW: 2.4 },
};
// Dải tiêu đề trong panel (đơn vị ô lưới): ô con bắt đầu từ y + HEADER
export const HEADER = 1;

// Mỗi đường có hướng chuẩn a → b; pts đi từ a tới b, chỉ gồm đoạn ngang/dọc theo lưới.
// Đường không được cắt qua panel nào: checkLayout() cảnh báo nếu có
export const WIRES = [
  { a: 'warehouse:WH-HN', b: 'warehouse:WH-HCM', pts: [[1.7, 3.15], [1.7, 4.95]] },
  { a: 'hub', b: 'cart', pts: [[8.9, 2.5], [9.4, 2.5], [9.4, 1.1], [10, 1.1]] },
  // cart → order (checkout) vòng bên phải pricing
  { a: 'cart', b: 'order', pts: [[13, 1.8], [15.6, 1.8], [15.6, 10.3], [13, 10.3]] },
  { a: 'pricing', b: 'cart', pts: [[12.4, 4.15], [12.4, 3.15]] },
  { a: 'order', b: 'hub', pts: [[10, 10], [9.4, 10], [9.4, 4], [8.9, 4]] },
  { a: 'order', b: 'zone:INNER_CITY', pts: [[11.5, 11.55], [11.5, 12.65]] },
  { a: 'order', b: 'zone:OTHER_CITY', pts: [[10.6, 11.55], [10.6, 12.1], [7, 12.1], [7, 12.65]] },
];
// Chưa có trong domain: Warehouse và InventoryManager là hai model riêng
export const MISSING_WIRE = [[3.4, 2.4], [5.5, 2.4]];

const EPS = 1e-6;
const overlap = (a1, a2, b1, b2) => Math.min(Math.max(a1, a2), b2) - Math.max(Math.min(a1, a2), b1) > EPS;
const inside = (v, lo, hi) => v > lo + EPS && v < hi - EPS;

// Kiểm tra bố cục: đường cắt xuyên panel, panel chồng lên nhau. Trả về danh sách lỗi (rỗng = ổn)
export function checkLayout(panels = PANELS, wires = [...WIRES, { a: 'missing', b: 'missing', pts: MISSING_WIRE }]) {
  const errors = [];
  const list = Object.entries(panels);
  for (const w of wires) {
    for (let i = 1; i < w.pts.length; i++) {
      const [[x1, y1], [x2, y2]] = [w.pts[i - 1], w.pts[i]];
      if (x1 !== x2 && y1 !== y2) { errors.push(`đường ${w.a} → ${w.b} có đoạn chéo`); continue; }
      for (const [id, p] of list) {
        const hit = y1 === y2
          ? inside(y1, p.y, p.y + p.d) && overlap(x1, x2, p.x, p.x + p.w)
          : inside(x1, p.x, p.x + p.w) && overlap(y1, y2, p.y, p.y + p.d);
        if (hit) errors.push(`đường ${w.a} → ${w.b} cắt qua panel ${id}`);
      }
    }
  }
  list.forEach(([a, p], i) => list.slice(i + 1).forEach(([b, q]) => {
    if (overlap(p.x, p.x + p.w, q.x, q.x + q.w) && overlap(p.y, p.y + p.d, q.y, q.y + q.d))
      errors.push(`panel ${a} chồng lên ${b}`);
  }));
  return errors;
}

const key = (p, q) => [p, q].sort().join('|');
const byPair = new Map(WIRES.map(w => [key(w.a, w.b), w]));
const warned = new Set();

// Trả về danh sách điểm theo chiều data, hoặc null nếu cặp không hỗ trợ
export function resolveFlow(flow) {
  if (!flow || !flow.from || !flow.to) return null;
  const w = byPair.get(key(flow.from, flow.to));
  if (!w) {
    const k = flow.from + '->' + flow.to;
    if (!warned.has(k)) { warned.add(k); console.warn('Visualizer: chưa có đường nối cho flow', k); }
    return null;
  }
  return flow.from === w.a ? w.pts : w.pts.slice().reverse();
}
