// Dữ liệu bố cục bản đồ của adapter ecommerce (chỉ dữ liệu, không logic): toạ độ theo ô lưới isometric.
// Hàm tính bố cục (computeLayout, checkLayout, resolveWire) nằm ở js/map/layout.js và đọc dữ liệu này qua adapter/index.js.
// ROOTS = các package (area) cấp 1; child = kích thước khối con xếp lưới động dưới dải tiêu đề (tileW = bề rộng ô trong khối con)
export const ROOTS = {
  inventory: { kind: 'area', x: 0,    y: 0,    w: 9.9, d: 9.2,  color: '--hub',   title: 'domain.inventory', sub: 'package' },
  order:     { kind: 'area', x: 12.5, y: 0,    w: 8.5, d: 12.2, color: '--store', title: 'domain.order',     sub: 'package' },
  catalog:   { kind: 'area', x: 8.8,  y: 12.7, w: 7.5, d: 6.2,  color: '--cat',   title: 'domain.catalog',   sub: 'package', child: { w: 3.4, d: 2.4, tileW: 1.45 } },
  pricing:   { kind: 'area', x: 16.5, y: 12.7, w: 7.5, d: 6.2,  color: '--price', title: 'domain.pricing',   sub: 'package', child: { w: 3.4, d: 2.4, tileW: 1.45 } },
    shipping:  { kind: 'area', x: 0,    y: 9.7,  w: 8,   d: 9.0,  color: '--zone',  title: 'domain.shipping',  sub: 'package' },
};
// Thành viên cố định đặt tuyệt đối trong một area (object / collection)
export const PLACED = {
  'warehouse:WH-HN':  { area: 'inventory', kind: 'object', x: 0.5, y: 1.5,  w: 3.4, d: 3.15, color: '--wh',    title: 'Warehouse WH-HN',  sub: 'Map<productId, qty>' },
  'warehouse:WH-HCM': { area: 'inventory', kind: 'object', x: 0.5, y: 5.45, w: 3.4, d: 3.15, color: '--wh',    title: 'Warehouse WH-HCM', sub: 'Map<productId, qty>' },
  hub:                { area: 'inventory', kind: 'object', x: 6,   y: 1.5,  w: 3.4, d: 3.75, color: '--hub',   title: 'InventoryManager', sub: 'Product.stock' },
  cart:               { area: 'order', kind: 'object', x: 13, y: 1.5, w: 3.4, d: 3.15, color: '--store', title: 'Cart', sub: 'Map<Product, qty>' },
  // Collection do visualizer giữ (domain không có object này): chứa các khối Order
  orders:             { area: 'order', kind: 'collection', x: 13, y: 5.5, w: 7.5, d: 6.2, color: '--store', title: 'List<Order>', sub: 'World', child: { w: 3.4, d: 2.4, tileW: 1.4 } },
  // ShippingZone là enum, luôn có hai giá trị: 1 ô baseFee + tối đa 1 ô computed (phí) xếp một hàng
  'zone:INNER_CITY':  { area: 'shipping', kind: 'object', x: 0.3, y: 10.9, w: 3.6, d: 2.4, color: '--zone', title: 'ShippingZone.INNER_CITY', sub: null },
  'zone:OTHER_CITY':  { area: 'shipping', kind: 'object', x: 4.1, y: 10.9, w: 3.6, d: 2.4, color: '--zone', title: 'ShippingZone.OTHER_CITY', sub: null },
  // Collection do visualizer giữ: chứa các khối Shipment (4 ô 2×2: status, failed, fee, tracking), mọc dần xuống
  shipments:          { area: 'shipping', kind: 'collection', x: 0.3, y: 13.5, w: 7.4, d: 5, color: '--zone', title: 'List<Shipment>', sub: 'World', child: { w: 3.4, d: 3.6, tileW: 1.4 } },
};
// Mỗi đường có hướng chuẩn a → b; pts đi từ a tới b, chỉ gồm đoạn ngang/dọc theo lưới.
// Đường nối các object/collection (hoặc area shipping, nơi zone/shipments không phải đầu mút nên flow leo lên vùng); checkLayout() cảnh báo nếu cắt qua khối
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

// Flow dùng id con lạ (không có parent trong world) thì đoán root theo tiền tố id: [tiền tố, id đầu mút đường nối]
export const PREFIXES = [['order:', 'orders'], ['zone:', 'shipping'], ['shipping:', 'shipments'], ['catalog:', 'catalog'], ['pricing:', 'pricing']];
