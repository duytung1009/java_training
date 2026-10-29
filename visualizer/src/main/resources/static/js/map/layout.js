// Bố cục bản đồ thuộc FE (plan D7): BE chỉ gửi panel id + flow {from, to}.
// Toạ độ theo ô lưới isometric.
export const PANELS = {
  'warehouse:WH-HN':  { x: 0,   y: 0,   w: 3.4, d: 2.6, color: '--wh',    title: 'Warehouse WH-HN',  sub: 'Map<productId, qty>' },
  'warehouse:WH-HCM': { x: 0,   y: 4.4, w: 3.4, d: 2.6, color: '--wh',    title: 'Warehouse WH-HCM', sub: 'Map<productId, qty>' },
  hub:                { x: 5.5, y: 1.5, w: 3.4, d: 3.2, color: '--hub',   title: 'InventoryManager', sub: 'Product.stock' },
  cart:               { x: 10,  y: 0,   w: 3,   d: 2.6, color: '--store', title: 'Cart',             sub: 'Map<Product, qty>' },
  pricing:            { x: 10,  y: 3.6, w: 5.1, d: 3.7, color: '--price', title: 'Pricing',          sub: 'PricingRule · DiscountCode', tileW: 1.4 },
  order:              { x: 10,  y: 7.9, w: 3,   d: 2,   color: '--store', title: 'Order',            sub: 'snapshot giá', tileW: 2.4 },
  'zone:INNER_CITY':  { x: 10,  y: 11,  w: 3,   d: 1.6, color: '--zone',  title: 'INNER_CITY',       sub: 'Hà Nội', tileW: 2.4 },
  'zone:OTHER_CITY':  { x: 5.5, y: 11,  w: 3,   d: 1.6, color: '--zone',  title: 'OTHER_CITY',       sub: 'tỉnh khác', tileW: 2.4 },
};

// Mỗi đường có hướng chuẩn a → b; pts đi từ a tới b
export const WIRES = [
  { a: 'warehouse:WH-HN', b: 'warehouse:WH-HCM', pts: [[1.7, 2.6], [1.7, 4.4]] },
  { a: 'hub', b: 'cart', pts: [[8.9, 2.5], [9.4, 2.5], [9.4, 1.1], [10, 1.1]] },
  // cart → order đi xuyên qua pricing (checkout)
  { a: 'cart', b: 'order', pts: [[11.5, 2.6], [11.5, 7.9]] },
  { a: 'pricing', b: 'cart', pts: [[12.4, 3.6], [12.4, 2.6]] },
  { a: 'order', b: 'hub', pts: [[10, 8.9], [9.4, 8.9], [9.4, 4], [8.9, 4]] },
  { a: 'order', b: 'zone:INNER_CITY', pts: [[11.5, 9.9], [11.5, 11]] },
  { a: 'order', b: 'zone:OTHER_CITY', pts: [[10.6, 9.9], [10.6, 10.5], [7, 10.5], [7, 11]] },
];
// Chưa có trong domain: Warehouse và InventoryManager là hai model riêng
export const MISSING_WIRE = [[3.4, 2], [5.5, 2]];

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
