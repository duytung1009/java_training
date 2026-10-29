// Bố cục bản đồ thuộc FE (plan D7): BE chỉ gửi node id + flows [{from, to}].
// Toạ độ theo ô lưới isometric.
export const ROOTS = {
  'warehouse:WH-HN':  { kind: 'object', x: 0,   y: 0,    w: 3.4, d: 3.15, color: '--wh',    title: 'Warehouse WH-HN',  sub: 'Map<productId, qty>' },
  'warehouse:WH-HCM': { kind: 'object', x: 0,   y: 4.95, w: 3.4, d: 3.15, color: '--wh',    title: 'Warehouse WH-HCM', sub: 'Map<productId, qty>' },
  hub:                { kind: 'object', x: 5.5, y: 1.5,  w: 3.4, d: 3.75, color: '--hub',   title: 'InventoryManager', sub: 'Product.stock' },
  // Khu vực package: chứa object sinh ra khi chạy (child = kích thước mỗi khối con, tileW = bề rộng ô trong khối con)
  pricing:            { kind: 'area', x: 19.2, y: 0, w: 7.5, d: 11.4, color: '--price', title: 'domain.pricing', sub: 'package', child: { w: 3.4, d: 2.4, tileW: 1.45 } },
  cart:               { kind: 'object', x: 11, y: 6.3, w: 3.4, d: 3.15, color: '--store', title: 'Cart', sub: 'Map<Product, qty>' },
  // Collection do visualizer giữ (domain không có object này): chứa các khối Order
  orders:             { kind: 'collection', x: 11, y: 10.4, w: 7.5, d: 6.2, color: '--store', title: 'List<Order>', sub: 'World', child: { w: 3.4, d: 2.4, tileW: 1.4 } },
  shipping:           { kind: 'area', x: 5.5, y: 6.5, w: 4.3, d: 6.2, color: '--zone', title: 'domain.shipping', sub: 'package', child: { w: 3.8, d: 2.4, tileW: 1.5 } },
};
// Dải tiêu đề trong panel (đơn vị ô lưới): ô con bắt đầu từ y + HEADER
export const HEADER = 1;

// Mỗi đường có hướng chuẩn a → b; pts đi từ a tới b, chỉ gồm đoạn ngang/dọc theo lưới.
// Đường không được cắt qua panel nào: checkLayout() cảnh báo nếu có
export const WIRES = [
  { a: 'warehouse:WH-HN', b: 'warehouse:WH-HCM', pts: [[1.7, 3.15], [1.7, 4.95]] },
  { a: 'hub', b: 'cart', pts: [[8.9, 2], [10.5, 2], [10.5, 7.5], [11, 7.5]] },
  // cart → orders (toOrder)
  { a: 'cart', b: 'orders', pts: [[12.7, 9.45], [12.7, 10.4]] },
  { a: 'orders', b: 'hub', pts: [[11, 11.5], [10, 11.5], [10, 4.5], [8.9, 4.5]] },
  // Phí ship: Product (trong hub) gọi ShippingZone qua PhysicalProduct.calculateShipping
  { a: 'hub', b: 'shipping', pts: [[7, 5.25], [7, 6.5]] },
];
// Chưa có trong domain: Warehouse và InventoryManager là hai model riêng
export const MISSING_WIRE = [[3.4, 2.4], [5.5, 2.4]];

const EPS = 1e-6;
const overlap = (a1, a2, b1, b2) => Math.min(Math.max(a1, a2), b2) - Math.max(Math.min(a1, a2), b1) > EPS;
const inside = (v, lo, hi) => v > lo + EPS && v < hi - EPS;

// Kiểm tra bố cục: đường cắt xuyên panel, panel chồng lên nhau. Trả về danh sách lỗi (rỗng = ổn)
export function checkLayout(panels = ROOTS, wires = [...WIRES, { a: 'missing', b: 'missing', pts: MISSING_WIRE }]) {
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
const PREFIX = [['order:', 'orders'], ['pricing:', 'pricing'], ['zone:', 'shipping']];

// Id (root hoặc con) → root tổ tiên. parentOf(id) trả id cha hoặc undefined; con lạ thì đoán theo tiền tố
export function rootOf(id, parentOf = () => undefined) {
  for (let i = 0; i < 4 && id && !ROOTS[id]; i++) {
    const p = parentOf(id);
    if (p) { id = p; continue; }
    return PREFIX.find(([pre]) => id.startsWith(pre))?.[1] ?? id;
  }
  return id;
}

// Trả về danh sách điểm theo chiều data, hoặc null nếu không có đường (cùng root: không cần đường; cặp lạ: cảnh báo 1 lần)
export function resolveFlow(flow, parentOf) {
  if (!flow || !flow.from || !flow.to) return null;
  const from = rootOf(flow.from, parentOf), to = rootOf(flow.to, parentOf);
  if (from === to) return null;
  const w = byPair.get(key(from, to));
  if (!w) {
    const k = from + '->' + to;
    if (!warned.has(k)) { warned.add(k); console.warn('Visualizer: chưa có đường nối cho flow', k); }
    return null;
  }
  return from === w.a ? w.pts : w.pts.slice().reverse();
}
