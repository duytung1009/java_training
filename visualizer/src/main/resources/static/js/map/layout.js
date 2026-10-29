// Bố cục bản đồ thuộc FE (plan D7): BE chỉ gửi node id + flows [{from, to}].
// Toạ độ theo ô lưới isometric.
// ROOTS = các package (area) cấp 1; child = kích thước khối con xếp lưới động dưới dải tiêu đề (tileW = bề rộng ô trong khối con)
export const ROOTS = {
  inventory: { kind: 'area', x: 0,    y: 0,    w: 9.9, d: 9.2,  color: '--hub',   title: 'domain.inventory', sub: 'package' },
  order:     { kind: 'area', x: 12.5, y: 0,    w: 8.5, d: 12.2, color: '--store', title: 'domain.order',     sub: 'package' },
  catalog:   { kind: 'area', x: 8.8,  y: 12.7, w: 7.5, d: 6.2,  color: '--cat',   title: 'domain.catalog',   sub: 'package', child: { w: 3.4, d: 2.4, tileW: 1.45 } },
  pricing:   { kind: 'area', x: 16.5, y: 12.7, w: 7.5, d: 6.2,  color: '--price', title: 'domain.pricing',   sub: 'package', child: { w: 3.4, d: 2.4, tileW: 1.45 } },
  // child.d 3.6: khối Shipment có 4 ô (status, failed, fee, tracking) xếp 2×2
  shipping:  { kind: 'area', x: 0,    y: 9.7,  w: 8,   d: 4.7,  color: '--zone',  title: 'domain.shipping',  sub: 'package', child: { w: 3.6, d: 3.6, tileW: 1.5 } },
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
  // Nhập hàng kho → hub (InventoryManager.replenish): đi qua khoảng trống x 3.9–6, không cắt khối
  { a: 'warehouse:WH-HN', b: 'hub', pts: [[3.9, 3], [6, 3]] },
  { a: 'warehouse:WH-HCM', b: 'hub', pts: [[3.9, 7], [5, 7], [5, 4.5], [6, 4.5]] },
  { a: 'hub', b: 'cart', pts: [[9.4, 2.5], [13, 2.5]] },
  // cart → orders (toOrder)
  { a: 'cart', b: 'orders', pts: [[14.7, 4.65], [14.7, 5.5]] },
  { a: 'orders', b: 'hub', pts: [[13, 8.5], [11.2, 8.5], [11.2, 4.5], [9.4, 4.5]] },
  // Mã giảm giá (domain.pricing) áp vào đơn lúc Cart.toOrder(code, tax): ra từ cạnh phải orders (mép trên không bị
  // đẩy khi orders giãn), đi ngoài khung order rồi xuống mép trên vùng pricing
  { a: 'orders', b: 'pricing', pts: [[20.5, 6.5], [22, 6.5], [22, 12.7]] },
  // Phí ship: Product (trong hub) gọi ShippingZone qua PhysicalProduct.calculateShipping
  { a: 'hub', b: 'shipping', pts: [[7.7, 5.25], [7.7, 9.7]] },
  // Tạo vận đơn từ đơn (ShippingFeeCalculator.feeFor, new Shipment): ra từ cạnh trái orders, đi ngang qua khoảng trống
  // giữa các vùng tới cạnh phải vùng shipping. Hoàn kho (shipping → hub, InventoryManager.restock) dùng lại đường hub ↔ shipping
  { a: 'orders', b: 'shipping', pts: [[13, 10.5], [8, 10.5]] },
];

const EPS = 1e-6;
const overlap = (a1, a2, b1, b2) => Math.min(Math.max(a1, a2), b2) - Math.max(Math.min(a1, a2), b1) > EPS;
const inside = (v, lo, hi) => v > lo + EPS && v < hi - EPS;
const boxOverlap = (p, q) => overlap(p.x, p.x + p.w, q.x, q.x + q.w) && overlap(p.y, p.y + p.d, q.y, q.y + q.d);

// Kiểm tra bố cục: đường cắt xuyên object/collection (cắt khung area thì được), area chồng nhau,
// thành viên chồng nhau hoặc nằm ngoài area của nó. Trả về danh sách lỗi (rỗng = ổn)
export function checkLayout(areas = ROOTS, placed = PLACED, wires = WIRES) {
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

// ---- Bố cục động: container mọc dần theo chiều +y, đẩy phần bên dưới xuống ----
export const MAX_ROWS = 4;
export const CHILD_MARGIN = 0.2, CHILD_GAP = 0.2;

// Lưới khối con của container r (có r.child) với n con: cols như cũ, hàng = max(hàng vừa trong d gốc, ceil(n/cols) ≤ MAX_ROWS)
export function gridOf(r, n) {
  const cs = r.child;
  const cols = Math.max(1, Math.floor((r.w - 2 * CHILD_MARGIN + CHILD_GAP + EPS) / (cs.w + CHILD_GAP)));
  const baseRows = Math.max(1, Math.floor((r.d - HEADER - 0.1 + CHILD_GAP + EPS) / (cs.d + CHILD_GAP)));
  const rows = Math.max(baseRows, Math.min(MAX_ROWS, Math.ceil(n / cols)));
  return { cols, rows, cap: cols * rows, d: Math.max(r.d, HEADER - 0.1 + rows * (cs.d + CHILD_GAP)) };
}

const xOver = (p, q) => overlap(p.x, p.x + p.w, q.x, q.x + q.w);

// Hàm thuần (không DOM): từ nodes của world → toạ độ hiệu dụng của area, thành viên, đường nối.
// areas/placed: rect tuyệt đối đã dịch; grids: lưới khối con; dy: độ dịch riêng của mỗi khối (placed: chỉ phần trong area, area cha tự dịch);
// local d (độ sâu hiệu dụng) nằm ở areas[id].d / placed[id].d, còn y gốc ở ROOTS/PLACED
export function computeLayout(nodes = []) {
  const count = {};
  for (const n of nodes) if (n.parent) count[n.parent] = (count[n.parent] ?? 0) + 1;
  const grids = {}, pd = {};
  for (const [id, p] of Object.entries(PLACED)) {
    if (p.child) { grids[id] = gridOf(p, count[id] ?? 0); pd[id] = grids[id].d; } else pd[id] = p.d;
  }
  // 1. Trong mỗi area: thành viên bên dưới container đã mọc bị đẩy xuống
  const inner = {}, growth = {}, ad = {};
  for (const [aid, A] of Object.entries(ROOTS)) {
    const ms = Object.entries(PLACED).filter(([, p]) => p.area === aid).sort((x, y) => x[1].y - y[1].y);
    for (const [id, p] of ms) {
      inner[id] = 0;
      for (const [oid, o] of ms)
        if (o.y + o.d <= p.y + EPS && xOver(o, p)) inner[id] = Math.max(inner[id], inner[oid] + pd[oid] - o.d);
    }
    let d = A.d;
    if (ms.length) {
      const pad = A.y + A.d - Math.max(...ms.map(([, p]) => p.y + p.d));
      d = Math.max(d, Math.max(...ms.map(([id, p]) => p.y + inner[id] + pd[id])) + pad - A.y);
    }
    if (A.child) { grids[aid] = gridOf(A, count[aid] ?? 0); d = Math.max(d, grids[aid].d); }
    ad[aid] = d; growth[aid] = d - A.d;
  }
  // 2. Area mọc đẩy các area nằm dưới (chồng x) xuống cùng một đoạn, xử lý tăng dần y
  const shift = {};
  for (const [id, A] of Object.entries(ROOTS).sort((x, y) => x[1].y - y[1].y)) {
    shift[id] = 0;
    for (const [oid, o] of Object.entries(ROOTS))
      if (oid !== id && o.y + o.d <= A.y + EPS && xOver(o, A)) shift[id] = Math.max(shift[id], shift[oid] + growth[oid]);
  }
  const areas = {}, placed = {}, dy = {};
  for (const [id, A] of Object.entries(ROOTS)) { areas[id] = { ...A, y: A.y + shift[id], d: ad[id] }; dy[id] = shift[id]; }
  for (const [id, p] of Object.entries(PLACED)) {
    const s = shift[p.area] + inner[id];
    placed[id] = { ...p, y: p.y + s, d: pd[id] }; dy[id] = inner[id];
  }
  // 3. Đường nối: dịch theo khối đầu mút; hai đầu lệch nhau thì chỉ dịch đầu mút (checkLayout sẽ báo nếu gãy)
  const total = id => placed[id] ? shift[PLACED[id].area] + inner[id] : (shift[id] ?? 0);
  const move = (pts, sa, sb) => pts.map(([x, y], i) => [x, y + (sa === sb ? sa : i === 0 ? sa : i === pts.length - 1 ? sb : y < (pts[0][1] + pts.at(-1)[1]) / 2 ? sa : sb)]);
  const wires = WIRES.map(w => ({ ...w, pts: move(w.pts, total(w.a), total(w.b)) }));
  return { areas, placed, wires, grids, dy };
}

const key = (p, q) => [p, q].sort().join('|');
const ENDPOINTS = new Set(WIRES.flatMap(w => [w.a, w.b]));
const warned = new Set();
const PREFIX = [['order:', 'orders'], ['zone:', 'shipping'], ['shipping:', 'shipping'], ['catalog:', 'catalog'], ['pricing:', 'pricing']];

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
// wires: đường hiệu dụng (đã dịch theo bố cục động), mặc định là đường gốc
export function resolveFlow(flow, parentOf, wires = WIRES) {
  if (!flow || !flow.from || !flow.to) return null;
  const from = rootOf(flow.from, parentOf), to = rootOf(flow.to, parentOf);
  if (from === to) return null;
  const w = wires.find(x => key(x.a, x.b) === key(from, to));
  if (!w) {
    const k = from + '->' + to;
    if (!warned.has(k)) { warned.add(k); console.warn('Visualizer: chưa có đường nối cho flow', k); }
    return null;
  }
  return from === w.a ? w.pts : w.pts.slice().reverse();
}
