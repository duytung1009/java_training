// Bảng chú thích khi click một khối trên bản đồ: class domain, vai trò, API chính, trạng thái hiện tại
import { ROOTS, PLACED } from '../map/layout.js';

const WAREHOUSE = {
  cls: 'Warehouse', pkg: 'domain.inventory', session: 'S04 · S06',
  desc: 'Kho vật lý giữ hàng dự trữ theo productId (chưa bán được ngay). Chuyển hàng giữa hai kho qua WarehouseTransferService, khoá theo thứ tự id để không deadlock; nhập lên hub bằng InventoryManager.replenish (hub khoá trước, kho sau — đang giữ lock kho thì không được gọi vào hub).',
  api: ['stockOf(productId)', 'add(productId, qty)', 'remove(productId, qty)', 'WarehouseTransferService.transfer(from, to, id, qty)', 'InventoryManager.replenish(from, id, qty)'],
  items: 'productId → số lượng',
};

const PRODUCT = (cls, calc, extra = '') => ({
  cls, pkg: 'domain.catalog', session: 'S03',
  desc: `Một Product cụ thể trong package catalog, giá cuối tính đa hình qua calculateFinalPrice(): ${calc}${extra}`,
  api: ['calculateFinalPrice()', 'getBasePrice()', 'decreaseStock(qty)', 'increaseStock(qty)'],
  items: 'basePrice, giá cuối',
});

// Khoá theo cls (class domain) hoặc tên package của khu vực; kind: object | collection | area
export const INFO = {
  Warehouse: {
    cls: 'Warehouse', pkg: 'domain.inventory', session: 'S04 · S06',
    desc: 'Kho vật lý giữ hàng dự trữ theo productId (chưa bán được ngay). Chuyển hàng giữa hai kho qua WarehouseTransferService, khoá theo thứ tự id để không deadlock; nhập lên hub bằng InventoryManager.replenish (hub khoá trước, kho sau — đang giữ lock kho thì không được gọi vào hub).',
    api: ['stockOf(productId)', 'add(productId, qty)', 'remove(productId, qty)', 'WarehouseTransferService.transfer(from, to, id, qty)', 'InventoryManager.replenish(from, id, qty)'],
    items: 'productId → số lượng',
  },
  InventoryManager: {
    cls: 'InventoryManager', pkg: 'domain.inventory', session: 'S04 · S06',
    desc: 'Hub: danh mục sản phẩm và hàng sẵn bán (Product.stock); kho Warehouse là hàng dự trữ. decreaseStock trừ cả đơn, restock hoàn lại cả đơn (đối xứng, dùng khi vận đơn bị trả về), replenish chuyển hàng từ một kho lên hub, đều all-or-nothing và synchronized. Thứ tự khoá toàn cục: hub trước, kho sau.',
    api: ['addProduct(product)', 'findById(id)', 'search(predicate)', 'decreaseStock(order) throws OutOfStockException', 'restock(order) throws ProductNotFoundException', 'replenish(from, id, qty) throws ProductNotFoundException, OutOfStockException'],
    items: 'productId → stock',
  },
  Cart: {
    cls: 'Cart', pkg: 'domain.order', session: 'S02',
    desc: 'Giỏ hàng gom Product với số lượng. toOrder chốt giỏ thành Order kèm giảm giá và thuế: truyền tỉ lệ trực tiếp, hoặc truyền DiscountCode (domain.pricing) — Order chỉ giữ tỉ lệ của mã, không giữ mã.',
    api: ['add(product, qty)', 'changeQuantity(product, qty)', 'remove(product)', 'toOrder(discountRate, taxRate)', 'toOrder(code, taxRate)'],
    items: 'productId → số lượng',
  },
  Order: {
    cls: 'Order', pkg: 'domain.order', session: 'S02 · S04',
    desc: 'Một đơn hàng giữ OrderLine với giá đã snapshot lúc đặt; tổng tiền tính từ subtotal, giảm giá và thuế. Khối này xuất hiện khi Cart.toOrder tạo đơn.',
    api: ['addLine(line)', 'subtotal() · discountAmount() · taxAmount()', 'finalAmount()', 'quantitiesByProductId()'],
    items: 'OrderLine: sản phẩm × số lượng → đơn giá snapshot',
  },
  'List<Order>': {
    cls: 'List<Order>', pkg: 'java.util', session: 'S02 · S04',
    desc: 'Danh sách đơn do visualizer (World) giữ để gom các Order đã tạo; domain không có object này. Mỗi khối con là một Order thật.',
    api: ['add(order)', 'size()'],
    items: 'các Order đã tạo',
  },
  'domain.inventory': {
    cls: 'domain.inventory', pkg: 'package', session: 'S04 · S06',
    desc: 'Package tồn kho: Warehouse (kho dự trữ), InventoryManager (hub: danh mục + Product.stock sẵn bán, replenish nhập từ kho) và WarehouseTransferService chuyển hàng giữa hai kho.',
    api: ['Warehouse', 'InventoryManager', 'WarehouseTransferService'],
    items: 'thành viên',
  },
  'domain.order': {
    cls: 'domain.order', pkg: 'package', session: 'S02 · S04',
    desc: 'Package đơn hàng: Cart chốt thành Order qua toOrder (theo tỉ lệ hoặc DiscountCode của domain.pricing); Order giữ OrderLine với giá đã snapshot. Danh sách đơn là do visualizer (World) giữ.',
    api: ['Cart', 'Order', 'OrderLine'],
    items: 'thành viên',
  },
  'domain.catalog': {
    cls: 'domain.catalog', pkg: 'package', session: 'S03',
    desc: 'Package sản phẩm: Product và các lớp con (Physical, Digital, Subscription, Bundle) tính giá cuối đa hình. Object hiện ra khi kịch bản tạo chúng.',
    api: ['Product', 'PhysicalProduct · DigitalProduct', 'SubscriptionProduct · BundleProduct'],
    items: 'sản phẩm đã tạo',
  },
  'domain.pricing': {
    cls: 'domain.pricing', pkg: 'package', session: 'S03',
    desc: 'Package giá gồm PricingRule, DiscountCode và Discountable. Chỉ là khung package: rule và mã giảm giá hiện ra khi kịch bản tạo chúng.',
    api: ['PricingRule', 'DiscountCode', 'Discountable'],
    items: 'rule / mã đã tạo',
  },
  PhysicalProduct: PRODUCT('PhysicalProduct', 'basePrice làm tròn scale 2 (implements Shippable, Discountable); calculateShipping(address) gọi ShippingZone.'),
  DigitalProduct: PRODUCT('DigitalProduct', 'basePrice làm tròn scale 2 (implements Discountable).'),
  SubscriptionProduct: PRODUCT('SubscriptionProduct', 'giá theo tháng × số tháng.', ' basePrice là giá một tháng.'),
  BundleProduct: PRODUCT('BundleProduct', 'basePrice × (1 − discountRate), gói nhiều Product.'),
  PricingRule: {
    cls: 'PricingRule', pkg: 'domain.pricing', session: 'S03',
    desc: 'Functional interface: nhận giá, trả giá mới. Khối này là một rule cụ thể; ô nét đứt là kết quả tính ra khi áp rule, không phải state.',
    api: ['apply(price)', 'andThen(next) · chain(rules)', 'percentOff(rate) · amountOff(amount)'],
    items: 'kết quả áp rule',
  },
  DiscountCode: {
    cls: 'DiscountCode', pkg: 'domain.pricing', session: 'S03',
    desc: 'Value object bất biến: mã và tỉ lệ giảm trong (0, 1]. Dùng để giảm giá từng sản phẩm (Discountable.applyDiscount) hoặc cả đơn lúc chốt giỏ (Cart.toOrder(code, taxRate)).',
    api: ['getCode()', 'getRate()'],
    items: 'code, rate',
  },
  'domain.shipping': {
    cls: 'domain.shipping', pkg: 'package', session: 'S02',
    desc: 'Package giao hàng: Shippable, enum ShippingZone, ShippingFeeCalculator (phí cả đơn) và Shipment (vận đơn, có vòng đời). Hai vùng luôn có mặt; vận đơn hiện ra khi kịch bản tạo chúng.',
    api: ['Shippable', 'ShippingZone', 'ShippingFeeCalculator', 'Shipment · ShipmentStatus · ShipmentEvent'],
    items: 'các vùng giao hàng, vận đơn',
  },
  ShippingZone: {
    cls: 'ShippingZone', pkg: 'domain.shipping', session: 'S02',
    desc: 'Enum vùng giao hàng, không có state. Phí = baseFee + 5000/kg × cân nặng (HALF_UP), được PhysicalProduct.calculateShipping gọi; Order không biết gì về phí ship. Ô nét đứt là phí tính ra.',
    api: ['ShippingZone.of(address)', 'feeFor(weightKg)', 'getBaseFee()', 'PhysicalProduct.calculateShipping(address)'],
    items: 'baseFee, phí theo thành phố',
  },
  Shipment: {
    cls: 'Shipment', pkg: 'domain.shipping', session: 'Extra',
    desc: 'Vận đơn: aggregate riêng, tham chiếu đơn qua orderId, giữ địa chỉ, phí ship (cố định lúc tạo) và trạng thái. Luồng: CREATED → IN_TRANSIT → DELIVERED; IN_TRANSIT → RETURNING (người nhận từ chối, hoặc giao thất bại lần thứ 3) → RETURNED; CREATED → CANCELLED. Chuyển sai luồng ném IllegalShipmentTransitionException và không đổi gì. Mỗi lần chuyển / giao thất bại thêm một ShipmentEvent vào history. Vận đơn không tự hoàn kho: hoàn hàng là InventoryManager.restock(order).',
    api: ['pickUp()', 'markDelivered()', 'failAttempt(reason)', 'refuse(reason)', 'markReturned()', 'cancel()', 'getStatus() · getFailedAttempts() · history()'],
    items: 'status, failedAttempts, fee, trackingCode',
  },
  ShippingFeeCalculator: {
    cls: 'ShippingFeeCalculator', pkg: 'domain.shipping', session: 'Extra',
    desc: 'Hàm thuần tính phí ship cho cả đơn: ShippingZone.of(address).feeFor(tổng kg), baseFee của vùng chỉ cộng một lần cho cả đơn (không cộng calculateShipping từng dòng). Tổng kg = Σ weightKg × số lượng của dòng vật lý; BundleProduct cộng các item vật lý bên trong. Đơn không có hàng vật lý thì ném IllegalArgumentException.',
    api: ['ShippingFeeCalculator.feeFor(order, address)'],
    items: 'không có khối riêng (phí hiện ở ô fee của Shipment)',
  },
};
const KIND = { object: 'object', collection: 'collection', area: 'area (package)' };

const esc = s => String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c]);

// Vẽ bảng cho node (id, kind, cls, title, sub, parent, items); node null thì ẩn bảng
export function renderInfo(el, id, node) {
  const info = node && (INFO[node.cls] ?? INFO[node.title] ?? INFO[`domain.${node.id}`]);
  if (!id || !node || !info) { el.hidden = true; return; }
  const top = (PLACED[id] ?? ROOTS[id]) ? id : node.parent; // con: lấy màu container
  const root = ROOTS[top] ?? PLACED[top];
  const items = node.items ?? [];
  const kids = node.children ?? [];
  el.style.setProperty('--c', `var(${root?.color ?? '--hub'})`);
  const row = it => `<li><span>${esc(it.computed ? '(tính ra) ' : '')}${esc(it.label ?? it.key)}</span><b>${esc(it.computed && !String(it.label ?? '').startsWith('=') ? '= ' : '')}${esc(it.value)}</b></li>`;
  el.innerHTML = `
    <button class="close" aria-label="Đóng">×</button>
    <div class="t">${esc(node.title)} <span class="kind">${esc(KIND[node.kind] ?? node.kind)}</span></div>
    <div class="cls">${esc(info.cls)} <span>· ${esc(info.pkg)} · ${esc(info.session)}</span></div>
    <p>${esc(info.desc)}</p>
    <div class="h">API chính</div>
    <ul>${info.api.map(a => `<li>${esc(a)}</li>`).join('')}</ul>
    <div class="h">${node.kind === 'object' ? `Trạng thái · ${esc(info.items)}${items.length ? ` · ${items.length}` : ''}` : `Khối con · ${kids.length}`}</div>
    ${node.kind === 'object'
      ? (items.length ? `<ul class="kv">${items.map(row).join('')}</ul>` : '<div class="empty">(trống)</div>')
      : (kids.length ? `<ul>${kids.map(k => `<li>${esc(k.title)}${k.sub ? ` · ${esc(k.sub)}` : ''}</li>`).join('')}</ul>` : '<div class="empty">(chưa có khối con)</div>')}`;
  el.hidden = false;
}
