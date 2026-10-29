// Bố cục bản đồ thuộc FE (plan D7): BE chỉ gửi node id + flows [{from, to}].
// Toạ độ theo ô lưới isometric.
// ROOTS = các package (area) cấp 1; child = kích thước khối con xếp lưới động dưới dải tiêu đề (tileW = bề rộng ô trong khối con)
export const ROOTS = {
  inventory: { kind: 'area', x: 0,    y: 0,    w: 9.9, d: 9.2,  color: '--hub',   title: 'domain.inventory', sub: 'package' },
  order:     { kind: 'area', x: 12.5, y: 0,    w: 8.5, d: 12.2, color: '--store', title: 'domain.order',     sub: 'package' },
  catalog:   { kind: 'area', x: 8.8,  y: 12.7, w: 7.5, d: 6.2,  color: '--cat',   title: 'domain.catalog',   sub: 'package', child: { w: 3.4, d: 2.4, tileW: 1.45 } },
  pricing:   { kind: 'area', x: 16.5, y: 12.7, w: 7.5, d: 6.2,  color: '--price', title: 'domain.pricing',   sub: 'package', child: { w: 3.4, d: 2.4, tileW: 1.45 } },
  shipping:  { kind: 'area', x: 0,    y: 9.7,  w: 8,   d: 3.8,  color: '--zone',  title: 'domain.shipping',  sub: 'package', child: { w: 3.6, d: 2.4, tileW: 1.5 } },
};
// Thành viên cố định đặt tuyệt đối trong một area (object / collection)
export const PLACED = {
  'warehouse:WH-HN':  { area: 'inventory', kind: 'object', x: 0.5, y: 1.5,  w: 3.4, d: 3.15, color: '--wh',    title: 'Warehouse WH-HN',  sub: 'Map<productId, qty>' },
  'warehouse:WH-HCM': { area: 'inventory', kind: 'object', x: 0.5, y: 5.45, w: 3.4, d: 3.15, color: '--wh',    title: 'Warehouse WH-HCM', sub: 'Map<productId, qty>' },
  hub:                { area: 'inventory', kind: 'object', x: 6,   y: 1.5,  w: 3.4, d: 3.75, color: '--hub',   title: 'InventoryManager', sub: 'Product.stock' },
  cart:               { area: 'order', kind: 'object', x: 13, y: 1.5, w: 3.4, d: 3.15, color: '--store', title: 'Cart', sub: 'Map<Product, qty>' },
  // Collection do visualizer giữ (domain không có object này): chứa các khối Order
  orders:             { area: 'order', kind: 'collection', x: 13, y: 5.5, w: 7.5, d: 6.2, color: '--store', title: 'List<Order>', sub: 'World', child: { w: 3.4, d: 2.4, tileW: 1.4 } },
};
// Dải tiêu đề trong khối (đơn vị ô lưới): ô con bắt đầu từ y + HEADER
export const HEADER = 1;

// Mỗi đường có hướng chuẩn a → b; pts đi từ a tới b, chỉ gồm đoạn ngang/dọc theo lưới.
// Đường nối các object/collection (hoặc area shipping); checkLayout() cảnh báo nếu cắt qua khối
export const WIRES = [
  { a: 'warehouse:WH-HN', b: 'warehouse:WH-HCM', pts: [[2.2, 4.65], [2.2, 5.45]] },
  { a: 'hub', b: 'cart', pts: [[9.4, 2.5], [13, 2.5]] },
  // cart → orders (toOrder)
  { a: 'cart', b: 'orders', pts: [[14.7, 4.65], [14.7, 5.5]] },
  { a: 'orders', b: 'hub', pts: [[13, 8.5], [11.2, 8.5], [11.2, 4.5], [9.4, 4.5]] },
  // Phí ship: Product (trong hub) gọi ShippingZone qua PhysicalProduct.calculateShipping
  { a: 'hub', b: 'shipping', pts: [[7.7, 5.25], [7.7, 9.7]] },
];
// Chưa có trong domain: Warehouse và InventoryManager là hai model riêng
export const MISSING_WIRE = [[3.9, 3], [6, 3]];

const EPS = 1e-6;
const overlap = (a1, a2, b1, b2) => Math.min(Math.max(a1, a2), b2) - Math.max(Math.min(a1, a2), b1) > EPS;
const inside = (v, lo, hi) => v > lo + EPS && v < hi - EPS;
const boxOverlap = (p, q) => overlap(p.x, p.x + p.w, q.x, q.x + q.w) && overlap(p.y, p.y + p.d, q.y, q.y + q.d);

// Kiểm tra bố cục: đường cắt xuyên object/collection (cắt khung area thì được), area chồng nhau,
// thành viên chồng nhau hoặc nằm ngoài area của nó. Trả về danh sách lỗi (rỗng = ổn)
export function checkLayout(areas = ROOTS, placed = PLACED, wires = [...WIRES, { a: 'missing', b: 'missing', pts: MISSING_WIRE }]) {
  const errors = [];
  const list = Object.entries(placed), alist = Object.entries(areas);
  for (const w of wires) {
    for (let i = 1; i < w.pts.length; i++) {
      const [[x1, y1], [x2, y2]] = [w.pts[i - 1], w.pts[i]];
      if (x1 !== x2 && y1 !== y2) { errors.push(`đường ${w.a} → ${w.b} có đoạn chéo`); continue; }
      for (const [id, p] of list) {
        const hit = y1 === y2
          ? inside(y1, p.y, p.y + p.d) && overlap(x1, x2, p.x, p.x + p.w)
          : inside(x1, p.x, p.x + p.w) && overlap(y1, y2, p.y, p.y + p.d);
        if (hit) errors.push(`đường ${w.a} → ${w.b} cắt qua khối ${id}`);
      }
    }
  }
  alist.forEach(([a, p], i) => alist.slice(i + 1).forEach(([b, q]) => {
    if (boxOverlap(p, q)) errors.push(`area ${a} chồng lên ${b}`);
  }));
  list.forEach(([a, p], i) => list.slice(i + 1).forEach(([b, q]) => {
    if (boxOverlap(p, q)) errors.push(`khối ${a} chồng lên ${b}`);
  }));
  for (const [id, p] of list) {
    const A = areas[p.area];
    if (!A) { errors.push(`khối ${id} thuộc area lạ ${p.area}`); continue; }
    if (p.x < A.x - EPS || p.x + p.w > A.x + A.w + EPS || p.y < A.y + HEADER - EPS || p.y + p.d > A.y + A.d + EPS)
      errors.push(`khối ${id} nằm ngoài area ${p.area} (hoặc đè dải tiêu đề)`);
  }
  return errors;
}

const key = (p, q) => [p, q].sort().join('|');
const byPair = new Map(WIRES.map(w => [key(w.a, w.b), w]));
const ENDPOINTS = new Set(WIRES.flatMap(w => [w.a, w.b]));
const warned = new Set();
const PREFIX = [['order:', 'orders'], ['zone:', 'shipping'], ['catalog:', 'catalog'], ['pricing:', 'pricing']];

// Id → id đầu tiên trên chuỗi cha là đầu mút của một đường (không có thì lên tới root).
// parentOf(id) trả id cha hoặc undefined; con lạ thì đoán theo tiền tố
export function rootOf(id, parentOf = () => undefined) {
  for (let i = 0; i < 5 && id && !ENDPOINTS.has(id); i++) {
    const p = parentOf(id);
    if (p) { id = p; continue; }
    return PREFIX.find(([pre]) => id.startsWith(pre))?.[1] ?? id;
  }
  return id;
}

// Trả về danh sách điểm theo chiều data, hoặc null nếu không có đường (cùng đầu mút: không cần đường; cặp lạ: cảnh báo 1 lần)
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
