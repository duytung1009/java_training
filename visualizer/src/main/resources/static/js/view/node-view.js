// Bản đồ ego của tầng Instance: thẻ trung tâm lớn (mọi item) + thẻ hàng xóm nhỏ nối bằng cạnh trực giao.
// Là <svg> thứ hai trong panel bản đồ; router quyết định khi nào hiện. Không biết domain: nhận thẻ + bố cục dựng sẵn (ego.js) và màu.
// Thẻ nằm trong lớp nền `.plane` chịu ma trận phép chiếu (px phẳng, screenMatrix); cạnh, mũi tên, huy hiệu và flow ở lớp overlay
// ngoài nền với điểm ánh xạ qua ma trận, nên mũi tên/chữ không méo (như flow của scene).
import { el, css, sleep, arrowMarkers, restartFlash, fadeOut } from '../map/iso.js';
import { screenMatrix, apply, bounds, lerp, toAttr, fitRect } from '../map/projection.js';
import { ellipsize } from '../dom.js';
import { CARD } from './ego.js';

const PROJ_MS = 400, MARGIN = 24, MAX_SCALE = 1.5, MIN_H = 420;
const reduced = () => matchMedia('(prefers-reduced-motion: reduce)').matches;
// Bề rộng ký tự trung bình theo cỡ chữ: tiêu đề 13px ≈ 7.6, tiêu đề lớn 15px ≈ 9, dòng phụ 11px ≈ 6.6
const fit = (text, px, charW) => ellipsize(String(text ?? ''), Math.max(3, Math.floor(px / charW)));
const pathOf = pts => pts.map((p, i) => `${i ? 'L' : 'M'}${p[0]} ${p[1]}`).join('');
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
  let matrix = screenMatrix('iso'), projToken = 0;
  let size = { w: 0, h: 0 };       // kích thước nội dung (px, trước phép chiếu)
  let focusId = null, selected = false;
  let edges = [];                  // [{ e, path, badge }]
  const routes = new Map();        // id hàng xóm → điểm cạnh (đi từ thẻ trung tâm tới thẻ đó)
  const activeFlows = new Set();   // flow đang chạy: vẽ ở overlay nên phải ánh xạ lại khi ma trận / bố cục đổi
  let flowColor = {};

  function build() {
    svg.textContent = '';
    const defs = el('defs', {}, svg);
    flowColor = { flow: css('--flow'), err: css('--err'), event: css('--event') };
    arrowMarkers(defs, 'nv-arrow-', flowColor);
    // Mũi tên của cạnh: auto-start-reverse để đầu cạnh cũng chỉ vào trong thẻ trung tâm khi flow đi vào
    const m = el('marker', { id: 'nv-dir', viewBox: '0 0 10 10', refX: 7, refY: 5, markerWidth: 6, markerHeight: 6, orient: 'auto-start-reverse' }, defs);
    el('path', { d: 'M1 1L8 5L1 9', fill: 'none', stroke: flowColor.flow, 'stroke-width': 1.6 }, m);
    layers.plane = el('g', { class: 'plane' }, svg);
    layers.cards = layers.plane;
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
    const b = bounds(matrix, [[0, 0], [size.w, 0], [size.w, size.h], [0, size.h]]);
    const f = fitRect(b, box, { margin: MARGIN, maxScale: MAX_SCALE });
    svg.setAttribute('viewBox', `${f.viewBox.x} ${f.viewBox.y} ${f.viewBox.w} ${f.viewBox.h}`);
    svg.setAttribute('width', f.width); svg.setAttribute('height', f.height);
    svg.style.width = f.width + 'px'; svg.style.height = f.height + 'px';
  }

  function applyMatrix(m) {
    matrix = m;
    layers.plane.setAttribute('transform', toAttr(m));
    placeEdges(); placeFlows(); fitView();
  }

  // Chuyển phép chiếu 'iso' | 'flat': lerp ma trận (viewBox đi theo); token huỷ lượt cũ
  function setProjection(name, { animate = true } = {}) {
    const to = screenMatrix(name), from = matrix, token = ++projToken;
    if (!animate || reduced()) return applyMatrix(to);
    const start = performance.now();
    (function frame(now) {
      if (token !== projToken) return;
      const f = Math.min(1, (now - start) / PROJ_MS);
      applyMatrix(f < 1 ? lerp(from, to, 1 - (1 - f) ** 3) : to);
      if (f < 1) requestAnimationFrame(frame);
    })(start);
  }

  // ---- Thẻ ----
  // c: { id, title, sub, color, focus, dim, more, caption, items: [{ k, v }], node, tip }
  function drawCard(c, r) {
    const big = !!c.focus, inner = r.w - 24;
    const g = el('g', { class: 'nv-card' + (big ? ' focus' : '') + (c.dim ? ' dim' : '') + (c.more ? ' more' : '') }, layers.cards);
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
    if (!c.more) {
      g.setAttribute('tabindex', 0);
      const act = e => { e.stopPropagation(); big ? toggle(c) : onNode(c.id); };
      g.addEventListener('click', act);
      g.addEventListener('dblclick', e => e.stopPropagation());
      g.addEventListener('keydown', e => { if (e.key === 'Enter' && !e.repeat) { e.preventDefault(); act(e); } });
    }
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
      path.setAttribute('d', pathOf(pts));
      if (badge) { const m = longestMid(pts); badge.setAttribute('transform', `translate(${m.x} ${m.y})`); }
    }
  }

  // spec: { cards: [{ id, title, sub, color, focus, dim, more, caption, items, node, tip }], layout: { rects, edges, width, height } }
  // Thẻ không đổi nội dung/vị trí thì giữ nguyên DOM (không mất focus bàn phím, nháy đang chạy)
  function render({ cards: list, layout }) {
    const { rects, edges: es, width, height } = layout;
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
    renderEdges(es);
    placeEdges(); placeFlows(); fitView();
  }

  // Dọn sạch (chưa có node để vẽ)
  function clear() {
    for (const k of cards.values()) k.g.remove();
    cards.clear(); renderEdges([]); size = { w: 0, h: 0 }; focusId = null; selected = false;
  }

  function flash(ids, ok) {
    for (const id of ids ?? []) { const k = cards.get(id); if (k) restartFlash(k.frame, ok); }
  }

  // Mũi tên chạy dọc cạnh giữa thẻ trung tâm và hàng xóm: f = { from, to, kind, label } với một đầu là thẻ trung tâm.
  // Flow event: nét chấm --event kèm nhãn. Chưa có cạnh (hàng xóm lạ) → không vẽ, không chờ
  async function flow(f, ok, ms) {
    const out = f.from === focusId, other = out ? f.to : f.from;
    if (!routes.has(other)) return;
    const isEvent = f.kind === 'EVENT', k = isEvent ? 'event' : ok ? 'flow' : 'err';
    const p = el('path', { class: isEvent ? 'flow event' : 'flow', stroke: flowColor[k], 'marker-end': `url(#nv-arrow-${k})` }, layers.flows);
    let label = null;
    if (isEvent && f.label) { label = el('text', { class: 'flow-label', 'text-anchor': 'middle' }, layers.flows); label.textContent = f.label; }
    const fl = { p, other, out, label };
    activeFlows.add(fl);
    placeFlow(fl);
    await sleep(ms);
    activeFlows.delete(fl);
    fadeOut(p, label);
  }
  function placeFlow({ p, other, out, label }) {
    const route = routes.get(other);
    if (!route) return;
    const pts = (out ? route : [...route].reverse()).map(q => apply(matrix, q));
    p.setAttribute('d', pathOf(pts));
    if (!label) return;
    const mid = p.getPointAtLength(p.getTotalLength() / 2);
    label.setAttribute('x', mid.x); label.setAttribute('y', mid.y - 5);
  }
  function placeFlows() { for (const fl of activeFlows) placeFlow(fl); }

  // Hình chữ nhật màn hình của thẻ (px viewport) cho hiệu ứng chuyển cảnh; null nếu chưa có / đang ẩn
  function cardRect(id) {
    const r = cards.get(id)?.frame.getBoundingClientRect();
    return r && r.width ? { left: r.left, top: r.top, width: r.width, height: r.height } : null;
  }

  build();
  new ResizeObserver(fitView).observe(svg.parentElement);
  return { svg, render, clear, flash, flow, select, setProjection, cardRect, fitView };
}
