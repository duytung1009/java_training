// (Chưa dùng: chờ P4d, tầng Instance sẽ dùng lại file này và graph-layout.js.)
// Bản đồ 2D phẳng cho tầng context / aggregate: khối chữ nhật + cạnh + mũi tên flow.
// Là <svg> thứ hai trong cùng panel bản đồ; router quyết định khi nào hiện. Không biết domain: nhận khối đã dựng sẵn.
import { el, css, sleep, arrowMarkers, restartFlash, fadeOut } from '../map/iso.js';
import { ellipsize } from '../dom.js';
import { placeLabels } from './graph-layout.js';

const MAX_CHARS = 24; // tiêu đề dài hơn thì cắt (bản đồ đầy đủ nằm ở <title>)
const END_LABEL = { a: 'D', b: 'U' }; // đầu a phụ thuộc (downstream), đầu b là upstream

const unit = (p, q) => { const d = Math.hypot(q[0] - p[0], q[1] - p[1]) || 1; return [(q[0] - p[0]) / d, (q[1] - p[1]) / d]; };
// Đoạn dài nhất của đường gấp khúc → { x, y } ở giữa đoạn đó
function longestMid(pts) {
  let best = null;
  for (let i = 0; i + 1 < pts.length; i++) {
    const len = Math.abs(pts[i + 1][0] - pts[i][0]) + Math.abs(pts[i + 1][1] - pts[i][1]);
    if (!best || len > best.len) best = { len, x: (pts[i][0] + pts[i + 1][0]) / 2, y: (pts[i][1] + pts[i + 1][1]) / 2 };
  }
  return best ?? { x: pts[0]?.[0] ?? 0, y: pts[0]?.[1] ?? 0 };
}
const pathOf = pts => pts.map((p, i) => `${i ? 'L' : 'M'}${p[0]} ${p[1]}`).join('');

export function createLevelView(svg) {
  let rects = {};
  let routes = new Map();  // "a|b" (đã sắp) → { a, points }: đường của cạnh để flow chạy theo
  let flowColor = {};      // màu mũi tên đọc một lần ở build()
  const frames = new Map(); // id khối → <rect>, để nháy
  const layers = {};

  function build() {
    svg.textContent = '';
    const defs = el('defs', {}, svg);
    // id riêng (lv-): svg isometric có bộ marker cùng tên mà có thể đang ẩn
    flowColor = { flow: css('--flow'), err: css('--err'), event: css('--event') };
    arrowMarkers(defs, 'lv-arrow-', flowColor);
    arrowMarkers(defs, 'lv-rel-', { rel: css('--muted') });
    for (const n of ['edges', 'blocks', 'flows']) layers[n] = el('g', {}, svg);
  }

  // Nhãn có nền chữ nhật (đọc được khi đè lên đường); (x, y) là tâm, w từ placeLabels
  function badge(parent, x, y, w, text) {
    const g = el('g', { class: 'lv-badge' }, parent);
    el('rect', { x: x - w / 2, y: y - 8, width: w, height: 15, rx: 3 }, g);
    el('text', { x, y: y + 3, 'text-anchor': 'middle' }, g).textContent = text;
  }

  // Cắt chữ theo bề ngang còn trống (px) và bề rộng ký tự trung bình của cỡ chữ đang dùng
  const fit = (text, width, charW) => ellipsize(String(text), Math.max(3, Math.floor(width / charW)));

  // Khối "thẻ": tiêu đề (+ chữ bên phải), dòng phụ (nhiều dòng), danh sách dòng nhỏ; khối `box` là khung aggregate nét đứt có hàng thành viên.
  // Mọi dòng cắt theo bề ngang thẻ: tiêu đề 13px ≈ 7.4px/ký tự, dòng phụ 11px ≈ 6.2, dòng nhỏ 10.5px ≈ 5.9
  function drawCard(g, b, p) {
    const inner = p.w - 24, rightW = b.right ? b.right.length * 6.2 + 8 : 0;
    el('text', { x: p.x + 12, y: p.y + (b.box ? 20 : 22), class: 'lv-title' }, g).textContent = fit(b.title, inner - rightW, 7.4);
    if (b.right) el('text', { x: p.x + p.w - 12, y: p.y + 22, class: 'lv-sub', 'text-anchor': 'end' }, g).textContent = b.right;
    const subs = b.subLines ?? (b.sub ? [b.sub] : []);
    subs.forEach((t, i) => { el('text', { x: p.x + 12, y: p.y + (b.box ? 34 : 40) + i * 13, class: 'lv-sub' }, g).textContent = fit(t, inner, 6.2); });
    const base = 45 + subs.length * 13;
    (b.lines ?? []).forEach((t, i) => { el('text', { x: p.x + 12, y: p.y + base + i * 14, class: 'lv-row' }, g).textContent = fit(t, inner, 5.9); });
    if (!b.box) return;
    el('line', { x1: p.x, x2: p.x + p.w, y1: p.y + 40, y2: p.y + 40, class: 'lv-sep' }, g);
    (b.memberRows ?? []).forEach((m, i) => {
      const tagW = m.tag ? m.tag.length * 5.9 + 8 : 0;
      el('rect', { x: p.x + 8, y: p.y + 45 + i * 16, width: p.w - 16, height: 14, class: 'lv-member' }, g);
      el('text', { x: p.x + 14, y: p.y + 56 + i * 16, class: 'lv-row val' }, g).textContent = fit(m.name, inner - 4 - tagW, 5.9);
      if (m.tag) el('text', { x: p.x + p.w - 14, y: p.y + 56 + i * 16, class: 'lv-row', 'text-anchor': 'end' }, g).textContent = m.tag;
    });
  }

  // Tô sáng khối và hàng xóm của nó, làm mờ phần còn lại; id null → bỏ làm mờ
  function highlight(id, blockEls, edgeEls) {
    const near = new Set(id == null ? [] : [id]);
    if (id != null) for (const [e] of edgeEls) if (e.a === id || e.b === id) { near.add(e.a); near.add(e.b); }
    for (const [bid, g] of blockEls) g.classList.toggle('faded', id != null && !near.has(bid));
    for (const [e, g] of edgeEls) g.classList.toggle('faded', id != null && e.a !== id && e.b !== id);
  }

  // spec: { blocks: [{ id, title, sub, color, port, dim, touched, selected, rows, card, right, lines, box, memberRows }], rects, width, height, onOpen(block),
  //   edges: [{ id, a, b, points (đi từ a tới b, mũi tên ở b), style, width, label, tip }], labels: [{ group, x, y }] }
  //   rows (tuỳ chọn): dòng "key value" trong khối, { more: n } là dòng "+n" (tầng instance thu gọn)
  function render({ blocks, edges, labels = [], rects: r, width, height, onOpen }) {
    rects = r;
    routes = new Map();
    layers.edges.textContent = ''; layers.blocks.textContent = ''; frames.clear();
    svg.setAttribute('viewBox', `0 0 ${width} ${height}`);
    svg.setAttribute('width', width); svg.setAttribute('height', height);
    const edgeEls = [], blockEls = new Map(), labelItems = [];
    for (const e of edges) {
      const pts = e.points;
      if (!pts || pts.length < 2) continue;
      routes.set(`${[e.a, e.b].sort().join('|')}`, { a: e.a, points: pts });
      const g = el('g', { class: `lv-edge ${e.style}` }, layers.edges);
      el('path', { d: pathOf(pts), class: 'hit' }, g);
      el('path', { d: pathOf(pts), class: 'line', 'stroke-width': e.width ?? 1, 'marker-end': 'url(#lv-rel-rel)' }, g);
      for (const [end, p, q] of [['a', pts[0], pts[1]], ['b', pts.at(-1), pts.at(-2)]]) {
        const d = unit(p, q);
        el('text', { x: p[0] + d[0] * 11 - d[1] * 7, y: p[1] + d[1] * 11 + d[0] * 7 + 3, class: 'lv-end', 'text-anchor': 'middle' }, g).textContent = END_LABEL[end];
      }
      if (e.label) labelItems.push({ id: edgeEls.length, points: pts, text: e.label, g });
      if (e.tip) el('title', {}, g).textContent = e.tip;
      edgeEls.push([e, g]);
    }
    const spots = placeLabels(labelItems, Object.values(r));
    for (const it of labelItems) { const sp = spots[it.id]; if (sp) badge(it.g, sp.x, sp.y, sp.w, it.text); }
    for (const l of labels) el('text', { x: l.x, y: l.y, class: 'lv-group' }, layers.blocks).textContent = l.group;
    for (const b of blocks) {
      const p = r[b.id];
      if (!p) continue;
      const cls = 'lv-block' + (b.port ? ' port' : '') + (b.box ? ' box' : '') + (b.dim ? ' dim' : '') + (b.touched ? ' touched' : '') + (b.selected ? ' sel' : '');
      const g = el('g', { class: cls }, layers.blocks);
      blockEls.set(b.id, g);
      const frame = el('rect', { x: p.x, y: p.y, width: p.w, height: p.h, class: 'frame', style: `stroke:${b.color};fill:${b.color}` }, g);
      frames.set(b.id, frame);
      if (b.card) drawCard(g, b, p);
      else {
        const compact = !!b.rows; // khối có dòng item: tiêu đề và sub sát lên trên để nhường chỗ
        el('text', { x: p.x + 12, y: p.y + (compact ? 20 : 28), class: 'lv-title' }, g).textContent = ellipsize(b.title, MAX_CHARS);
        el('text', { x: p.x + 12, y: p.y + (compact ? 35 : 50), class: 'lv-sub' }, g).textContent = ellipsize(b.sub ?? '', MAX_CHARS + 6);
        (b.rows ?? []).forEach((row, i) => {
          const y = p.y + 54 + i * 14;
          if (row.more) { el('text', { x: p.x + 12, y, class: 'lv-row' }, g).textContent = `+${row.more}`; return; }
          el('text', { x: p.x + 12, y, class: 'lv-row' }, g).textContent = ellipsize(row.key, 14);
          el('text', { x: p.x + p.w - 12, y, class: 'lv-row val', 'text-anchor': 'end' }, g).textContent = ellipsize(row.value, 16);
        });
      }
      el('title', {}, g).textContent = b.tip ?? b.title;
      g.addEventListener('click', e => { e.stopPropagation(); onOpen?.(b); });
      if (edges.length) {
        g.addEventListener('mouseenter', () => highlight(b.id, blockEls, edgeEls));
        g.addEventListener('mouseleave', () => highlight(null, blockEls, edgeEls));
      }
    }
  }

  function flash(ids, ok) {
    for (const id of ids ?? []) {
      const f = frames.get(id);
      if (!f) continue;
      restartFlash(f, ok);
    }
  }

  // Mũi tên chạy theo đường của cạnh giữa hai khối (đảo chiều nếu cạnh đi ngược; chưa có cạnh thì nối tâm hai khối).
  // Flow event: nét chấm --event kèm nhãn. Tự mờ đi sau ms. Khối lạ → không vẽ, không chờ
  async function flow({ from, to }, kind, ok, ms, label) {
    const a = rects[from], b = rects[to];
    if (!a || !b) return;
    const isEvent = kind === 'EVENT';
    const k = isEvent ? 'event' : ok ? 'flow' : 'err';
    const route = routes.get([from, to].sort().join('|'));
    const pts = route ? (route.a === from ? route.points : [...route.points].reverse())
      : [[a.x + a.w / 2, a.y + a.h / 2], [b.x + b.w / 2, b.y + b.h / 2]];
    const p = el('path', {
      d: pathOf(pts), class: isEvent ? 'flow event' : 'flow', stroke: flowColor[k], 'marker-end': `url(#lv-arrow-${k})`,
    }, layers.flows);
    let t = null;
    if (isEvent && label) {
      const m = longestMid(pts);
      t = el('text', { x: m.x, y: m.y - 6, class: 'flow-label', 'text-anchor': 'middle' }, layers.flows);
      t.textContent = label;
    }
    await sleep(ms);
    fadeOut(p, t);
  }

  function clearFlows() { layers.flows.textContent = ''; }

  build();
  return { render, flash, flow, clearFlows };
}
