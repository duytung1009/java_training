// Bảng chú thích khi click một khối trên bản đồ: class domain, vai trò, API chính, trạng thái hiện tại
import { PANELS } from '../map/layout.js';

const WAREHOUSE = {
  cls: 'Warehouse', pkg: 'domain.inventory', session: 'S04 · S06',
  desc: 'Kho vật lý giữ số lượng theo productId. Chuyển hàng giữa hai kho qua WarehouseTransferService, khoá theo thứ tự id để không deadlock.',
  api: ['stockOf(productId)', 'add(productId, qty)', 'remove(productId, qty)', 'WarehouseTransferService.transfer(from, to, id, qty)'],
  items: 'productId → số lượng',
};

const ZONE = {
  cls: 'ShippingZone', pkg: 'domain.shipping', session: 'S02',
  desc: 'Enum vùng giao hàng. Phí = baseFee + FEE_PER_KG × cân nặng, làm tròn HALF_UP.',
  api: ['ShippingZone.of(address)', 'feeFor(weightKg)', 'getBaseFee()'],
  items: 'thành phố → phí ship',
};

export const INFO = {
  'warehouse:WH-HN': WAREHOUSE,
  'warehouse:WH-HCM': WAREHOUSE,
  hub: {
    cls: 'InventoryManager', pkg: 'domain.inventory', session: 'S04 · S06',
    desc: 'Danh mục sản phẩm và tồn kho (Product.stock). decreaseStock trừ cả đơn theo kiểu all-or-nothing, synchronized để nhiều thread không bán âm kho.',
    api: ['addProduct(product)', 'findById(id)', 'search(predicate)', 'decreaseStock(order) throws OutOfStockException'],
    items: 'productId → stock',
  },
  cart: {
    cls: 'Cart', pkg: 'domain.order', session: 'S02',
    desc: 'Giỏ hàng gom Product với số lượng. toOrder chốt giỏ thành Order kèm tỉ lệ giảm giá và thuế.',
    api: ['add(product, qty)', 'changeQuantity(product, qty)', 'remove(product)', 'toOrder(discountRate, taxRate)'],
    items: 'productId → số lượng',
  },
  pricing: {
    cls: 'PricingRule · DiscountCode', pkg: 'domain.pricing', session: 'S03',
    desc: 'Giá theo loại sản phẩm (calculateFinalPrice đa hình) và PricingRule là functional interface ghép được bằng andThen / chain. DiscountCode là value object bất biến.',
    api: ['Product.calculateFinalPrice()', 'PricingRule.apply(price)', 'andThen(next) · chain(rules)', 'percentOff(rate) · amountOff(amount)'],
    items: 'quy tắc / mã → giá trị',
  },
  order: {
    cls: 'Order', pkg: 'domain.order', session: 'S02 · S04',
    desc: 'Đơn hàng giữ OrderLine với giá đã snapshot lúc đặt; tổng tiền tính từ subtotal, giảm giá và thuế.',
    api: ['addLine(line)', 'subtotal() · discountAmount() · taxAmount()', 'finalAmount()', 'quantitiesByProductId()'],
    items: 'mã đơn → thành tiền',
  },
  'zone:INNER_CITY': ZONE,
  'zone:OTHER_CITY': ZONE,
};

const esc = s => String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c]);

// Vẽ bảng cho panel id với dữ liệu PanelView hiện tại; id null thì ẩn bảng
export function renderInfo(el, id, panel) {
  const info = INFO[id];
  if (!id || !info) { el.hidden = true; return; }
  const p = PANELS[id];
  const items = panel?.items ?? [];
  el.style.setProperty('--c', `var(${p.color})`);
  el.innerHTML = `
    <button class="close" aria-label="Đóng">×</button>
    <div class="t">${esc(p.title)}</div>
    <div class="cls">${esc(info.cls)} <span>· ${esc(info.pkg)} · ${esc(info.session)}</span></div>
    <p>${esc(info.desc)}</p>
    <div class="h">API chính</div>
    <ul>${info.api.map(a => `<li>${esc(a)}</li>`).join('')}</ul>
    <div class="h">Trạng thái · ${esc(info.items)}${items.length ? ` · ${items.length}` : ''}</div>
    ${items.length
      ? `<ul class="kv">${items.map(it => `<li><span>${esc(it.label ?? it.key)}</span><b>${esc(it.value)}</b></li>`).join('')}</ul>`
      : '<div class="empty">(trống)</div>'}`;
  el.hidden = false;
}
