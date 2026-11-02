// Contract giữa FE core (static/js/**) và adapter của domain (static/adapter/**).
// Core CHỈ import file này; mọi dữ liệu riêng của domain (id, toạ độ, màu, mô tả, lịch sử) nằm sau object `adapter`:
//
//   adapter = {
//     layout: {
//       ROOTS:    { [areaId]: { kind: 'area', x, y, w, d, color: '--biến-css', title, sub, child?: { w, d, tileW } } },
//       PLACED:   { [id]: { area: areaId, kind: 'object' | 'collection', x, y, w, d, color, title, sub, child? } },
//       WIRES:    [{ a: id, b: id, pts: [[x, y], ...] }],   // chỉ đoạn ngang/dọc; a, b là id trong PLACED hoặc ROOTS
//       PREFIXES: [[tiềnTốId, idĐầuMút], ...],              // đoán đầu mút đường nối cho id con lạ
//     },
//     infoOf:    node => { cls, pkg, session, desc, api: [...], items } | undefined,  // chú thích cho bảng click khối (tra theo cls / title / id area)
//     about:     { INTRO, STAGES: [{ title, commit?, why, what: [...] }], PRODUCT: [...], LATER: [...], LESSONS: [...] },
//     legend:    [{ color: '--biến-css', label }],            // mục legend của domain, hiện trước các mục chung
//     tileColor: (rootId, itemKey) => '#rrggbb' | null,       // màu ô item; null → màu mặc định của core
//   }
//
// Biến CSS màu của domain (--wh, --hub, ...) định nghĩa trong adapter/theme.css, được index.html nạp.
import { ROOTS, PLACED, WIRES, PREFIXES } from './layout.js';
import { INFO } from './info.js';
import { about } from './about.js';

const PRODUCT_COLORS = { LAPTOP: '#e8c46a', PHONE: '#7fd4e0', HEADPHONE: '#b7e07f', BOOK: '#e88fb4' };

function tileColor(rootId, key) {
  if (PRODUCT_COLORS[key]) return PRODUCT_COLORS[key];
  if (rootId === 'pricing' || rootId === 'catalog' || rootId === 'orders') return '#c9b6ff';
  if (rootId === 'shipping') return '#8fe0d6';
  return null;
}

export const adapter = {
  layout: { ROOTS, PLACED, WIRES, PREFIXES },
  infoOf: node => INFO[node.cls] ?? INFO[node.title] ?? INFO[`domain.${node.id}`],
  about,
  legend: [
    { color: '--wh', label: 'Warehouse' },
    { color: '--hub', label: 'InventoryManager' },
    { color: '--store', label: 'Cart / Order' },
    { color: '--zone', label: 'ShippingZone' },
  ],
  tileColor,
};
