// Vẽ bản đồ SVG: panel, ô con, đường nối và animation
import { el, iso, diamond, wirePath, css, flat } from './iso.js';
import { ROOTS, HEADER, WIRES, MISSING_WIRE, resolveFlow, checkLayout } from './layout.js';

const PRODUCT = { LAPTOP: '#e8c46a', PHONE: '#7fd4e0', HEADPHONE: '#b7e07f', BOOK: '#e88fb4' };
const reduced = () => matchMedia('(prefers-reduced-motion: reduce)').matches;
const MORE = '__more'; // ô "+N" gom các item không đủ chỗ
const sleep = ms => new Promise(r => setTimeout(r, ms));

function tileColor(rootId, key) {
  if (key === MORE) return '#7d8290';
  if (PRODUCT[key]) return PRODUCT[key];
  if (rootId === 'pricing' || rootId === 'orders') return '#c9b6ff';
  if (rootId === 'shipping') return '#8fe0d6';
  return '#cfd3dc';
}

const EPS = 1e-6;
const CELL = 35.78; // độ dài 1 ô lưới dọc trục x trên màn hình (px)

// Vừa chữ vào maxPx: thu font xuống tối thiểu minPx, vẫn dài thì cắt và thêm "…". Trả về true nếu giữ nguyên chữ gốc.
function fit(node, text, maxPx, basePx, minPx = 6.5) {
  node.textContent = text;
  node.style.fontSize = basePx + 'px';
  let len = node.getComputedTextLength();
  if (len <= maxPx) return true;
  const px = Math.max(minPx, basePx * maxPx / len);
  node.style.fontSize = px + 'px';
  len = node.getComputedTextLength();
  let t = String(text);
  while (len > maxPx && t.length > 1) {
    t = t.slice(0, -1);
    node.textContent = t + '…';
    len = node.getComputedTextLength();
  }
  return len <= maxPx && t.length === String(text).length;
}
const CHILD_MARGIN = 0.2, CHILD_GAP = 0.2, CHILD_HEADER = 0.9; // dải đầu khối con: tiêu đề + sub + khe ≥ 4px // lưới khối con trong container
const FRAME = { // kiểu viền theo loại khối
  object: { fill: 0.05 },
  collection: { fill: 0.12, dash: '6 4' },
  area: { fill: 0, dash: '1 4', cap: 'round' },
};
const prefix = it => dispVal(it).slice(0, dispVal(it).length - String(it.value).length); // phần '= ' đứng trước số
const dispVal = it => it.computed && !String(it.label ?? '').startsWith('=') ? `= ${it.value}` : String(it.value);

// onSelect(id, node): click một khối (id null = bỏ chọn)
export function createScene(svg, { onSelect = () => {} } = {}) {
  const layers = {}, frames = {};
  const blocks = new Map(); // id (root hoặc con) → { id, root, kind, color, small, g, frame, head, titleEl, subEl, rect, more }
  let world = null, selected = null, nodes = new Map();
  const tiles = new Map(); // `${blockId}/${key}` → { g, polygon, text, name, val, title, value, pre, anim }

  function build() {
    svg.textContent = '';
    tiles.clear(); blocks.clear();
    for (const k of Object.keys(frames)) delete frames[k];
    const defs = el('defs', {}, svg);
    for (const [id, c] of [['arrow-flow', css('--flow')], ['arrow-err', css('--err')]]) {
      const m = el('marker', { id, viewBox: '0 0 10 10', refX: 7, refY: 5, markerWidth: 6, markerHeight: 6, orient: 'auto' }, defs);
      el('path', { d: 'M1 1L8 5L1 9', fill: 'none', stroke: c, 'stroke-width': 1.6 }, m);
    }
    for (const err of checkLayout()) console.warn('Visualizer layout:', err);
    for (const n of ['ground', 'wires', 'zones', 'flows', 'labels']) layers[n] = el('g', {}, svg);

    // Ground + viewBox suy ra từ kích thước các root và đường nối
    const ps = Object.values(ROOTS);
    const maxX = Math.max(...ps.map(p => p.x + p.w)) + 0.5, maxY = Math.max(...ps.map(p => p.y + p.d)) + 0.5;
    const pts = [...ps.flatMap(p => [[p.x, p.y], [p.x + p.w, p.y], [p.x + p.w, p.y + p.d], [p.x, p.y + p.d]]),
      ...WIRES.flatMap(w => w.pts), ...MISSING_WIRE].map(q => iso(...q));
    const M = 24, x0 = Math.min(...pts.map(q => q[0])) - M, y0 = Math.min(...pts.map(q => q[1])) - M;
    const vw = Math.max(...pts.map(q => q[0])) + M - x0, vh = Math.max(...pts.map(q => q[1])) + M - y0;
    svg.setAttribute('viewBox', `${x0} ${y0} ${vw} ${vh}`);
    svg.setAttribute('width', Math.round(vw)); svg.setAttribute('height', Math.round(vh));
    svg.style.setProperty('--nat-w', Math.round(vw) + 'px'); // min-width trong CSS: hẹp thì cuộn ngang

    for (let x = -0.5; x < maxX; x++) for (let y = -0.5; y < maxY; y++)
      el('polygon', { points: diamond(x, y, 1, 1), fill: 'none', stroke: '#1b1f27' }, layers.ground);

    for (const w of WIRES)
      el('path', { d: wirePath(w.pts), fill: 'none', stroke: css('--wire'), 'stroke-width': 1.5 }, layers.wires);
    el('path', { d: wirePath(MISSING_WIRE), fill: 'none', stroke: css('--wire'), 'stroke-width': 1, 'stroke-dasharray': '2 5' }, layers.wires);
    // Ghi chú nằm phẳng dọc theo đường, phía sau đường
    const note = el('text', { transform: flat(...iso(4.45, 2.4)), 'text-anchor': 'middle', class: 'zone-sub' }, layers.labels);
    el('tspan', { x: 0, y: -13 }, note).textContent = 'WH → hub';
    el('tspan', { x: 0, y: -3 }, note).textContent = 'chưa có';

    for (const [id, p] of Object.entries(ROOTS)) {
      setHead(makeBlock(id, id, p, layers.zones, false, p.kind), p.title, p.sub);
    }
    svg.onclick = () => select(null);
    if (selected) frames[selected]?.classList.add('sel');
  }

  // Dựng khung + tiêu đề một khối (root hoặc con). Con: tiêu đề nhỏ hơn, không có nhãn loại
  function makeBlock(id, root, rect, parent, small, kind, cls = 'zone', selectable = true) {
    const color = css(ROOTS[root].color), st = FRAME[kind] ?? FRAME.object;
    const g = el('g', { class: cls }, parent);
    if (selectable) g.addEventListener('click', e => { e.stopPropagation(); select(selected === id ? null : id); });
    const frame = el('polygon', { fill: color, 'fill-opacity': st.fill, stroke: color, 'stroke-width': small ? 1.2 : 1.5, class: 'zone-frame' }, g);
    if (st.dash) frame.setAttribute('stroke-dasharray', st.dash);
    if (st.cap) frame.setAttribute('stroke-linecap', st.cap);
    const head = el('text', { class: 'zone-head' + (small ? ' small' : '') + (kind === 'area' ? ' area' : '') }, g);
    const titleEl = el('tspan', { x: 0, y: 0, class: 'zone-title', fill: color }, head);
    const subEl = el('tspan', { x: 0, y: small ? 9 : 11, class: 'zone-sub' }, head);
    const tip = el('title', {}, head);
    const b = { id, root, kind, color, small, g, frame, head, titleEl, subEl, tip, rect };
    blocks.set(id, b); frames[id] = frame;
    placeBlock(b, rect);
    return b;
  }

  // Tiêu đề + sub (kèm nhãn loại ở cuối dòng sub nếu còn chỗ; khối con không có nhãn), vừa trong dải đầu khối
  function setHead(b, title, sub) {
    const mx = (b.rect.w - (b.small ? 0.22 : 0.3)) * CELL, base = b.small ? 7.5 : 9;
    fit(b.titleEl, title, mx, b.small ? 8.5 : 11);
    const full = b.small ? sub : (sub ? `${sub} · ${b.kind}` : b.kind);
    if (!fit(b.subEl, full, mx, base)) { /* không đủ chỗ cho nhãn: bỏ nhãn */
      if (full !== sub) fit(b.subEl, sub, mx, base);
    }
    b.tip.textContent = sub ? `${title}\n${sub}` : title;
  }

  function placeBlock(b, r) {
    b.rect = r;
    b.frame.setAttribute('points', diamond(r.x, r.y, r.w, r.d));
    b.head.setAttribute('transform', flat(...iso(r.x + (b.small ? 0.12 : 0.2), r.y + (b.small ? 0.3 : 0.4))));
  }

  const nodeOf = id => nodes.get(id) ?? null;
  // Node kèm danh sách con (cho thẻ chú thích của container)
  const describe = id => { const n = nodeOf(id); return n && { ...n, children: [...nodes.values()].filter(c => c.parent === id) }; };

  function select(id) {
    if (selected) frames[selected]?.classList.remove('sel');
    selected = id && frames[id] ? id : null;
    if (selected) frames[selected].classList.add('sel');
    onSelect(selected, describe(selected));
  }

  // Đếm dần số trong ô; huỷ lượt đếm cũ nếu có giá trị mới đến
  function countTo(t, to, ms) {
    const from = t.value, start = performance.now(), token = ++t.anim;
    fit(t.val, t.pre + String(to), t.maxPx, t.valBase); // co chữ theo giá trị cuối để không nhảy cỡ khi đếm
    if (ms <= 0) return;
    t.val.textContent = t.pre + String(from);
    (function frame(now) {
      if (token !== t.anim) return;
      const f = Math.min(1, (now - start) / ms);
      t.val.textContent = t.pre + String(Math.round(from + (to - from) * f));
      if (f < 1) requestAnimationFrame(frame);
    })(start);
  }

  // Đồng bộ ô của một khối với danh sách item
  function syncTiles(b, all, spec, { animate, speed, slow }) {
    const { tw, td, gap, margin, head } = spec, r = b.rect;
    const maxPx = tw * CELL - 4, sm = b.small, valBase = sm ? 9 : 11;
    const fitTile = (t, it) => {
      fit(t.name, it.label ?? it.key, maxPx, sm ? 8 : 10);
      fit(t.val, dispVal(it), maxPx, valBase);
      if (t.typ) fit(t.typ, it.type, maxPx, sm ? 5.5 : 6.5, 5);
    };
    const cols = Math.max(1, Math.floor((r.w - margin + gap + EPS) / (tw + gap)));
    // Số hàng vừa trong khối: đáy ô cuối ≤ r.y + r.d - 0.1
    const rows = Math.max(1, Math.floor((r.d - head - 0.1 + gap + EPS) / (td + gap)));
    const cap = cols * rows, title = nodeOf(b.id)?.title ?? b.titleEl.textContent;
    let list = all;
    if (all.length > cap) {
      const hidden = all.slice(cap - 1);
      list = [...all.slice(0, cap - 1), {
        key: MORE, label: `+${hidden.length}`, value: 'khác',
        tip: `${title} · +${hidden.length} khác (click khối để xem đủ)\n` + hidden.map(h => `${h.key} · ${h.value}`).join('\n'),
      }];
    }
    const seen = new Set();
    list.forEach((it, i) => {
      const k = `${b.id}/${it.key}`;
      seen.add(k);
      const tx = r.x + margin + (i % cols) * (tw + gap), ty = r.y + head + Math.floor(i / cols) * (td + gap);
      const [cx, cy] = iso(tx + tw / 2, ty + td / 2);
      const tip = it.tip ?? `${title} · ${it.key} · ${dispVal(it)}`;
      const dash = it.computed ? '3 3' : null;
      let t = tiles.get(k);
      if (!t) {
        const g = el('g', { class: animate ? 'tile pop-in' : 'tile' }, b.g);
        const color = tileColor(b.root, it.key);
        const polygon = el('polygon', { points: diamond(tx, ty, tw, td), fill: color, 'fill-opacity': 0.06, stroke: color, 'stroke-width': 1.5 }, g);
        if (dash) polygon.setAttribute('stroke-dasharray', dash);
        // Ô 2 dòng: tên trên, số dưới; có type thì thêm dòng nhỏ tên class ở trên cùng
        const text = el('text', { transform: flat(cx, cy), 'text-anchor': 'middle', fill: color }, g);
        const dy = it.type ? 2.5 : 0;
        const typ = it.type ? el('tspan', { x: 0, y: -8.5, class: 'typ' }, text) : null;
        const name = el('tspan', { x: 0, y: -1.5 + dy }, text);
        const val = el('tspan', { x: 0, y: 9.5 + dy, class: 'val' }, text);
        const ttl = el('title', {}, g);
        ttl.textContent = tip;
        t = { g, polygon, text, name, val, typ, title: ttl, value: it.value, pre: prefix(it), anim: 0, maxPx, valBase };
        tiles.set(k, t);
        fitTile(t, it);
        return;
      }
      t.polygon.setAttribute('points', diamond(tx, ty, tw, td));
      if (dash) t.polygon.setAttribute('stroke-dasharray', dash); else t.polygon.removeAttribute('stroke-dasharray');
      t.text.setAttribute('transform', flat(cx, cy));
      t.title.textContent = tip;
      t.pre = prefix(it); t.maxPx = maxPx; t.valBase = valBase;
      fit(t.name, it.label ?? it.key, maxPx, sm ? 8 : 10);
      if (it.value === t.value) return;
      if (typeof it.value === 'number' && typeof t.value === 'number') {
        countTo(t, it.value, animate ? 500 / speed * slow : 0);
        if (animate) {
          t.g.classList.remove('pop-in', 'bump-up', 'bump-down');
          void t.g.getBBox();
          t.g.classList.add(it.value > t.value ? 'bump-up' : 'bump-down');
        }
      } else {
        t.anim++;
        fit(t.val, dispVal(it), maxPx, valBase);
      }
      t.value = it.value;
    });
    for (const [k, t] of tiles) {
      if (!k.startsWith(b.id + '/') || seen.has(k)) continue;
      tiles.delete(k);
      if (animate) {
        t.g.classList.remove('pop-in'); t.g.classList.add('pop-out');
        setTimeout(() => t.g.remove(), 380 / speed);
      } else t.g.remove();
    }
  }

  // Bỏ khối con (kèm ô của nó): pop-out hoặc xoá ngay
  function removeBlock(b, { animate, speed }) {
    blocks.delete(b.id); delete frames[b.id];
    for (const k of [...tiles.keys()]) if (k.startsWith(b.id + '/')) tiles.delete(k);
    if (animate) {
      b.g.classList.remove('pop-in'); b.g.classList.add('pop-out');
      setTimeout(() => b.g.remove(), 380 / speed);
    } else b.g.remove();
  }

  // Xếp khối con của container vào lưới dưới dải tiêu đề; quá chỗ thì ô cuối thành "+N"
  function syncChildren(rid, r, kids, opts) {
    const cs = r.child, gap = CHILD_GAP;
    const cols = Math.max(1, Math.floor((r.w - 2 * CHILD_MARGIN + gap + EPS) / (cs.w + gap)));
    const rows = Math.max(1, Math.floor((r.d - HEADER - 0.1 + gap + EPS) / (cs.d + gap)));
    const cap = cols * rows;
    let shown = kids, hidden = [];
    if (kids.length > cap) { shown = kids.slice(0, cap - 1); hidden = kids.slice(cap - 1); }
    const slots = shown.map(n => ({ id: n.id, node: n }));
    const moreId = `${rid}/${MORE}`;
    if (hidden.length) slots.push({ id: moreId, more: hidden });
    const want = new Set(slots.map(s => s.id));
    for (const b of [...blocks.values()])
      if (b.root === rid && b.small && !want.has(b.id)) removeBlock(b, opts);
    slots.forEach((s, i) => {
      const rect = { x: r.x + CHILD_MARGIN + (i % cols) * (cs.w + gap), y: r.y + HEADER + Math.floor(i / cols) * (cs.d + gap), w: cs.w, d: cs.d };
      let b = blocks.get(s.id);
      if (!b) {
        const pop = opts.animate ? 'zone child pop-in' : 'zone child';
        // "+N" không chọn riêng: click rơi về container
        b = makeBlock(s.id, rid, rect, blocks.get(rid).g, true, 'object', pop, !s.more);
      } else placeBlock(b, rect);
      if (s.more) {
        setHead(b, `+${s.more.length}`, 'khác');
        let tip = b.g.querySelector(':scope > title') ?? el('title', {}, b.g);
        tip.textContent = `${nodeOf(rid)?.title ?? rid} · +${s.more.length} khác (click container để xem đủ)\n` + s.more.map(n => n.title).join('\n');
      } else {
        const n = s.node;
        setHead(b, n.title ?? n.cls ?? n.id, n.sub ?? '');
        syncTiles(b, n.items ?? [], { tw: cs.tileW, td: 1.0, gap: 0.15, margin: 0.15, head: CHILD_HEADER }, opts);
      }
    });
  }

  // Đồng bộ khối + ô với WorldView: mới pop-in, mất pop-out, số đổi thì đếm dần + nháy viền.
  // animate=false: dựng lại ngay, không hiệu ứng (Reset / lần đầu)
  function sync(next, { animate = true, speed = 1 } = {}) {
    world = next;
    nodes = new Map((world?.nodes ?? []).map(n => [n.id, n]));
    const opts = { animate, speed, slow: reduced() ? 0 : 1 };
    for (const [id, r] of Object.entries(ROOTS)) {
      const b = blocks.get(id), n = nodeOf(id);
      setHead(b, n?.title ?? r.title, n?.sub ?? r.sub);
      if (r.child) syncChildren(id, r, [...nodes.values()].filter(c => c.parent === id), opts);
      else syncTiles(b, n?.items ?? [], { tw: r.tileW || 1.3, td: 0.9, gap: 0.2, margin: 0.3, head: HEADER }, opts);
    }
    if (selected && !frames[selected]) select(null);
    else if (selected) onSelect(selected, describe(selected));
  }

  function flash(ids, ok) {
    for (const id of ids ?? []) {
      const f = frames[id];
      if (!f) continue; // id lạ: bỏ qua
      f.classList.remove('flash-ok', 'flash-err'); void f.getBBox();
      f.classList.add(ok ? 'flash-ok' : 'flash-err');
    }
  }

  // Chỉ đường có action mới chạy: nét đứt chảy theo hướng data, mũi tên ở đích.
  // Flow không hỗ trợ → không animation, không chờ.
  async function flow(f, ok, ms, after) {
    const afterNodes = after?.nodes && new Map(after.nodes.map(n => [n.id, n])); // con mới chưa có trong world hiện tại
    const pts = resolveFlow(f, id => nodes.get(id)?.parent ?? afterNodes?.get(id)?.parent);
    if (!pts) return;
    const p = el('path', { d: wirePath(pts), class: 'flow', stroke: ok ? css('--flow') : css('--err'), 'marker-end': `url(#arrow-${ok ? 'flow' : 'err'})` }, layers.flows);
    await sleep(ms);
    p.style.transition = 'opacity .3s'; p.style.opacity = 0;
    setTimeout(() => p.remove(), 300);
  }

  // Về trạng thái ban đầu, không animation
  function reset(world) {
    for (const t of tiles.values()) t.g.remove();
    tiles.clear();
    for (const b of [...blocks.values()]) if (b.small) removeBlock(b, { animate: false });
    layers.flows.textContent = '';
    for (const f of Object.values(frames)) f.classList.remove('flash-ok', 'flash-err');
    sync(world, { animate: false });
  }

  enablePan(svg.parentElement);
  build();
  return { sync, flash, flow, reset, select };
}

// Kéo chuột để cuộn bản đồ; dịch chuyển < 4px vẫn là click (chọn/bỏ chọn khối)
function enablePan(box) {
  let drag = null, moved = false;
  box.addEventListener('pointerdown', e => {
    if (e.button !== 0) return;
    drag = { x: e.clientX, y: e.clientY, sl: box.scrollLeft, st: box.scrollTop };
    moved = false;
  });
  window.addEventListener('pointermove', e => {
    if (!drag) return;
    const dx = e.clientX - drag.x, dy = e.clientY - drag.y;
    if (!moved && Math.hypot(dx, dy) < 4) return;
    moved = true; box.classList.add('panning');
    box.scrollLeft = drag.sl - dx; box.scrollTop = drag.st - dy;
  });
  const end = () => { drag = null; box.classList.remove('panning'); };
  window.addEventListener('pointerup', end);
  window.addEventListener('pointercancel', end);
  // Chặn click sinh ra sau khi kéo thật (capture: chạy trước handler của panel/svg)
  box.addEventListener('click', e => { if (moved) { e.stopPropagation(); moved = false; } }, true);
}
