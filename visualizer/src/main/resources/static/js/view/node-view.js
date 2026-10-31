// Bản đồ ego của tầng Instance: thẻ trung tâm lớn (mọi item) + thẻ hàng xóm nhỏ nối bằng cạnh trực giao.
// Là <svg> thứ hai trong panel bản đồ; router quyết định khi nào hiện. Không biết domain: nhận thẻ + bố cục dựng sẵn (ego.js) và màu.
// Thẻ nằm trong lớp nền `.plane` chịu ma trận phép chiếu (px phẳng, screenMatrix); cạnh, mũi tên, huy hiệu và flow ở lớp overlay
// ngoài nền với điểm ánh xạ qua ma trận, nên mũi tên/chữ không méo (như flow của scene).
import { el, css, sleep, arrowMarkers, restartFlash, fadeOut, pathD, corners, screenRect, drawFlow, placeFlowPath, activatable } from '../map/iso.js';
import { screenMatrix, apply, bounds, lerp, toAttr, fitRect } from '../map/projection.js';
import { reduced, easeOut, tween } from '../anim.js';
import { ellipsize } from '../dom.js';
import { CARD } from './ego.js';

const PROJ_MS = 400, MARGIN = 24, MAX_SCALE = 1.5, MIN_H = 420;
// Bề rộng ký tự trung bình theo cỡ chữ: tiêu đề 13px ≈ 7.6, tiêu đề lớn 15px ≈ 9, dòng phụ 11px ≈ 6.6
const fit = (text, px, charW) => ellipsize(String(text ?? ''), Math.max(3, Math.floor(px / charW)));
// Đoạn dài nhất của đường gấp khúc → { x, y } ở giữa đoạn đó
function longestMid(pts) {
  let best = null;
  for (let i = 0; i + 1 < pts.length; i++) {
    const len = Math.abs(pts[i + 1][0] - pts[i][0]) + Math.abs(pts[i + 1][1] - pts[i][1]);
    if (!best || len > best.len) best = { len, x: (pts[i][0] + pts[i + 1][0]) / 2, y: (pts[i][1] + pts[i + 1][1]) / 2 };
  }
  return best ?? { x: pts[0]?.[0] ?? 0, y: pts[0]?.[1] ?? 0 };
}

// onSelect(id, node): click thẻ trung tâm (id null = bỏ chọn); onNode(id): click / Enter một thẻ hàng xóm
export function createNodeView(svg, { onSelect = () => {}, onNode = () => {} } = {}) {
  const layers = {}, cards = new Map(); // id → { g, frame, key }
  let matrix = screenMatrix('iso'), cancelProj = null;
  let size = { w: 0, h: 0 };       // kích thước nội dung (px, trước phép chiếu)
  let focusId = null, selected = false;
  let edges = [], edgesKey = '';   // [{ e, path, badge }] + khoá dữ liệu cạnh đã vẽ
  let stale = false;               // hộp đổi kích thước khi svg đang ẩn: lần render kế tiếp phải fit lại
  const routes = new Map();        // id hàng xóm → điểm cạnh (đi từ thẻ trung tâm tới thẻ đó)
  const activeFlows = new Set();   // flow đang chạy: vẽ ở overlay nên phải ánh xạ lại khi ma trận / bố cục đổi
  let flowColor = {};

  function build() {
    svg.textContent = '';
    const defs = el('defs', {}, svg);
    flowColor = { flow: css('--flow'), err: css('--err'), event: css('--event') };
    arrowMarkers(defs, 'nv-arrow-', flowColor);
    // Mũi tên của cạnh: auto-start-reverse để đầu cạnh cũng chỉ vào trong thẻ trung tâm khi flow đi vào
    arrowMarkers(defs, 'nv-', { dir: flowColor.flow }, { orient: 'auto-start-reverse' });
    layers.plane = el('g', { class: 'plane' }, svg);
    const overlay = el('g', { class: 'nv-overlay' }, svg);
    layers.edges = el('g', {}, overlay);
    layers.flows = el('g', {}, overlay);
    svg.onclick = () => select(null);
  }

  // Hộp khung nhìn: bề ngang panel, bề dọc panel nhưng không thấp quá MIN_H và không cao quá max-height của .map-view
  function boxSize() {
    const b = svg.parentElement;
    return { w: b.clientWidth, h: Math.min(Math.max(b.clientHeight, MIN_H), Math.max(MIN_H, innerHeight - 150)) };
  }

  // viewBox ôm nội dung sau phép chiếu, svg = hộp
  function fitView() {
    if (!size.w) return;
    const box = boxSize();
    if (box.w <= 0) return;
    const b = bounds(matrix, corners({ x: 0, y: 0, w: size.w, h: size.h }));
    const f = fitRect(b, box, { margin: MARGIN, maxScale: MAX_SCALE });
    svg.setAttribute('viewBox', `${f.viewBox.x} ${f.viewBox.y} ${f.viewBox.w} ${f.viewBox.h}`);
    svg.setAttribute('width', f.width); svg.setAttribute('height', f.height);
    svg.style.width = f.width + 'px'; svg.style.height = f.height + 'px';
    stale = false;
  }

  function applyMatrix(m) {
    matrix = m;
    layers.plane.setAttribute('transform', toAttr(m));
    placeEdges(); placeFlows(); fitView();
  }

  // Chuyển phép chiếu 'iso' | 'flat': lerp ma trận (viewBox đi theo); lượt mới huỷ lượt cũ
  function setProjection(name, { animate = true } = {}) {
    const to = screenMatrix(name), from = matrix;
    cancelProj?.(); cancelProj = null;
    if (!animate || reduced()) return applyMatrix(to);
    cancelProj = tween(PROJ_MS, t => applyMatrix(t < 1 ? lerp(from, to, easeOut(t)) : to));
  }

  // ---- Thẻ ----
  // c: { id, title, sub, color, focus, dim, more, caption, items: [{ k, v }], node, tip }
  function drawCard(c, r) {
    const big = !!c.focus, inner = r.w - 24;
    const g = el('g', { class: 'nv-card' + (big ? ' focus' : '') + (c.dim ? ' dim' : '') + (c.more ? ' more' : '') }, layers.plane);
    const frame = el('rect', { x: r.x, y: r.y, width: r.w, height: r.h, rx: 6, class: 'frame', style: `stroke:${c.color};fill:${c.color}` }, g);
    el('text', { x: r.x + 12, y: r.y + (big ? 26 : 22), class: 'nv-title' + (big ? ' big' : '') }, g).textContent = fit(c.title, inner, big ? 9 : 7.6);
    if (c.sub) el('text', { x: r.x + 12, y: r.y + (big ? 42 : 38), class: 'nv-sub' }, g).textContent = fit(c.sub, inner, 6.6);
    if (c.caption) el('text', { x: r.x + 12, y: r.y + 54, class: 'nv-row' }, g).textContent = fit(c.caption, inner, 6);
    if (big) {
      el('line', { x1: r.x, x2: r.x + r.w, y1: r.y + CARD.headH - 4, y2: r.y + CARD.headH - 4, class: 'nv-sep' }, g);
      const items = c.items ?? [], y0 = r.y + CARD.headH + 10;
      if (!items.length) el('text', { x: r.x + 12, y: y0, class: 'nv-row' }, g).textContent = '(trống)';
      items.forEach((it, i) => {
        const y = y0 + i * CARD.rowH;
        el('text', { x: r.x + 12, y, class: 'nv-row' }, g).textContent = fit(it.k, inner * 0.55, 6);
        el('text', { x: r.x + r.w - 12, y, class: 'nv-row val', 'text-anchor': 'end' }, g).textContent = fit(it.v, inner * 0.45, 6);
      });
    }
    el('title', {}, g).textContent = c.tip ?? (c.sub ? `${c.title}\n${c.sub}` : c.title);
    if (!c.more) activatable(g, () => (big ? toggle(c) : onNode(c.id)), { dbl: true });
    return { g, frame };
  }

  function toggle(c) {
    selected = !selected;
    markSelected();
    onSelect(selected ? c.id : null, selected ? c.node : null);
  }
  const markSelected = () => { for (const [id, k] of cards) k.g.classList.toggle('sel', selected && id === focusId); };

  // Bỏ chọn thẻ trung tâm (chỉ báo ra ngoài khi đang có chọn)
  function select(id) {
    if (id != null || !selected) return;
    selected = false; markSelected(); onSelect(null, null);
  }

  // ---- Cạnh (overlay) ----
  function renderEdges(es) {
    layers.edges.textContent = ''; edges = []; routes.clear();
    for (const e of es) {
      routes.set(e.to, e.points);
      const g = el('g', { class: 'nv-edge' + (e.structural ? ' structural' : '') }, layers.edges);
      const path = el('path', { class: 'line', 'stroke-width': e.structural ? 1.3 : Math.min(1.5 + (e.count - 1) * 0.5, 5) }, g);
      if (e.dir === 'out' || e.dir === 'both') path.setAttribute('marker-end', 'url(#nv-dir)');
      if (e.dir === 'in' || e.dir === 'both') path.setAttribute('marker-start', 'url(#nv-dir)');
      let badge = null;
      if (!e.structural && e.count > 1) {
        const text = `×${e.count}`, w = text.length * 6 + 8;
        badge = el('g', { class: 'nv-badge' }, g);
        el('rect', { x: -w / 2, y: -8, width: w, height: 15, rx: 3 }, badge);
        el('text', { x: 0, y: 3, 'text-anchor': 'middle' }, badge).textContent = text;
      }
      edges.push({ e, path, badge });
    }
  }
  function placeEdges() {
    for (const { e, path, badge } of edges) {
      const pts = e.points.map(p => apply(matrix, p));
      path.setAttribute('d', pathD(pts));
      if (badge) { const m = longestMid(pts); badge.setAttribute('transform', `translate(${m.x} ${m.y})`); }
    }
  }

  // spec: { cards: [{ id, title, sub, color, focus, dim, more, caption, items, node, tip }], layout: { rects, edges, width, height } }
  // Thẻ không đổi nội dung/vị trí thì giữ nguyên DOM (không mất focus bàn phím, nháy đang chạy)
  function render({ cards: list, layout }) {
    const { rects, edges: es, width, height } = layout;
    const resized = width !== size.w || height !== size.h;
    size = { w: width, h: height };
    const want = new Set();
    for (const c of list) {
      const r = rects[c.id];
      if (!r) continue;
      want.add(c.id);
      if (c.focus && c.id !== focusId) { focusId = c.id; selected = false; }
      const key = JSON.stringify([c.title, c.sub, c.color, c.dim, c.caption, c.items, c.tip, r]), old = cards.get(c.id);
      if (old?.key === key) continue;
      old?.g.remove();
      cards.set(c.id, { ...drawCard(c, r), key });
    }
    for (const [id, k] of [...cards]) if (!want.has(id)) { k.g.remove(); cards.delete(id); }
    markSelected();
    // Cạnh chỉ dựng lại khi dữ liệu cạnh đổi; fit lại khi kích thước nội dung đổi (hộp đổi thì ResizeObserver lo)
    const ekey = JSON.stringify(es.map(e => [e.to, e.points, e.dir, e.structural, e.count]));
    if (ekey !== edgesKey) { edgesKey = ekey; renderEdges(es); placeEdges(); placeFlows(); }
    if (resized || stale) fitView();
  }

  // Dọn sạch (chưa có node để vẽ)
  function clear() {
    for (const k of cards.values()) k.g.remove();
    cards.clear(); renderEdges([]); edgesKey = ''; size = { w: 0, h: 0 }; focusId = null; selected = false;
  }

  function flash(ids, ok) {
    for (const id of ids ?? []) { const k = cards.get(id); if (k) restartFlash(k.frame, ok); }
  }

  // Mũi tên chạy dọc cạnh giữa thẻ trung tâm và hàng xóm: f = { from, to, kind, label } với một đầu là thẻ trung tâm.
  // Flow event: nét chấm --event kèm nhãn. Chưa có cạnh (hàng xóm lạ) → không vẽ, không chờ
  async function flow(f, ok, ms) {
    const out = f.from === focusId, other = out ? f.to : f.from;
    if (!routes.has(other)) return;
    const fl = { ...drawFlow(layers.flows, { kind: f.kind, ok, label: f.label, markerPrefix: 'nv-arrow-', colors: flowColor }), other, out };
    activeFlows.add(fl);
    placeFlow(fl);
    await sleep(ms);
    activeFlows.delete(fl);
    fadeOut(fl.p, fl.label);
  }
  function placeFlow(fl) {
    const route = routes.get(fl.other);
    if (!route) return;
    placeFlowPath(fl, (fl.out ? route : [...route].reverse()).map(q => apply(matrix, q)));
  }
  function placeFlows() { for (const fl of activeFlows) placeFlow(fl); }

  // Hình chữ nhật màn hình của thẻ (px viewport) cho hiệu ứng chuyển cảnh; null nếu chưa có / đang ẩn
  const cardRect = id => screenRect(cards.get(id)?.frame);

  build();
  new ResizeObserver(() => { if (svg.classList.contains('lv-off')) stale = true; else fitView(); }).observe(svg.parentElement);
  return { svg, render, clear, flash, flow, select, setProjection, cardRect };
}
