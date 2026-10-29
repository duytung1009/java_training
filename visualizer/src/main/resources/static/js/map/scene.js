// Vẽ bản đồ SVG: panel, ô con, đường nối và animation
import { el, iso, diamond, wirePath, css, flat } from './iso.js';
import { PANELS, HEADER, WIRES, MISSING_WIRE, resolveFlow, checkLayout } from './layout.js';

const PRODUCT = { LAPTOP: '#e8c46a', PHONE: '#7fd4e0', HEADPHONE: '#b7e07f', BOOK: '#e88fb4' };
const reduced = () => matchMedia('(prefers-reduced-motion: reduce)').matches;
const MORE = '__more'; // ô "+N" gom các item không đủ chỗ
const sleep = ms => new Promise(r => setTimeout(r, ms));

function tileColor(panelId, key) {
  if (key === MORE) return '#7d8290';
  if (PRODUCT[key]) return PRODUCT[key];
  if (panelId === 'pricing' || panelId === 'order') return '#c9b6ff';
  if (panelId.startsWith('zone:')) return '#8fe0d6';
  return '#cfd3dc';
}

// onSelect(id, panelView): click một khối (id null = bỏ chọn)
export function createScene(svg, { onSelect = () => {} } = {}) {
  const layers = {}, groups = {}, frames = {};
  let world = null, selected = null;
  const tiles = new Map(); // `${panelId}/${key}` → { g, polygon, text, label, value, anim }

  function build() {
    svg.textContent = '';
    tiles.clear();
    const defs = el('defs', {}, svg);
    for (const [id, c] of [['arrow-flow', css('--flow')], ['arrow-err', css('--err')]]) {
      const m = el('marker', { id, viewBox: '0 0 10 10', refX: 7, refY: 5, markerWidth: 6, markerHeight: 6, orient: 'auto' }, defs);
      el('path', { d: 'M1 1L8 5L1 9', fill: 'none', stroke: c, 'stroke-width': 1.6 }, m);
    }
    for (const err of checkLayout()) console.warn('Visualizer layout:', err);
    for (const n of ['ground', 'wires', 'zones', 'flows', 'labels']) layers[n] = el('g', {}, svg);

    // Ground + viewBox suy ra từ kích thước các panel và đường nối
    const ps = Object.values(PANELS);
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

    for (const [id, p] of Object.entries(PANELS)) {
      const color = css(p.color);
      groups[id] = el('g', { class: 'zone' }, layers.zones);
      groups[id].addEventListener('click', e => { e.stopPropagation(); select(selected === id ? null : id); });
      frames[id] = el('polygon', { points: diamond(p.x, p.y, p.w, p.d), fill: color, 'fill-opacity': 0.05, stroke: color, 'stroke-width': 1.5, class: 'zone-frame' }, groups[id]);
      // Tiêu đề nằm phẳng trong dải đầu panel, chạy dọc cạnh sau
      const head = el('text', { transform: flat(...iso(p.x + 0.2, p.y + 0.4)), class: 'zone-head' }, groups[id]);
      el('tspan', { x: 0, y: 0, class: 'zone-title', fill: color }, head).textContent = p.title;
      el('tspan', { x: 0, y: 11, class: 'zone-sub' }, head).textContent = p.sub;
    }
    svg.onclick = () => select(null);
    if (selected) frames[selected]?.classList.add('sel');
  }

  const panelOf = id => world?.panels?.find(p => p.id === id) ?? null;

  function select(id) {
    if (selected) frames[selected]?.classList.remove('sel');
    selected = id && frames[id] ? id : null;
    if (selected) frames[selected].classList.add('sel');
    onSelect(selected, panelOf(selected));
  }


  // Đếm dần số trong ô; huỷ lượt đếm cũ nếu có giá trị mới đến
  function countTo(t, to, ms) {
    const from = t.value, start = performance.now(), token = ++t.anim;
    if (ms <= 0) { t.val.textContent = String(to); return; }
    (function frame(now) {
      if (token !== t.anim) return;
      const f = Math.min(1, (now - start) / ms);
      t.val.textContent = String(Math.round(from + (to - from) * f));
      if (f < 1) requestAnimationFrame(frame);
    })(start);
  }

  // Đồng bộ ô con với WorldView: mới pop-in, mất pop-out, số đổi thì đếm dần + nháy viền.
  // animate=false: dựng lại ngay, không hiệu ứng (Reset / lần đầu)
  function sync(next, { animate = true, speed = 1 } = {}) {
    world = next;
    if (selected) onSelect(selected, panelOf(selected));
    const byId = new Map((world?.panels ?? []).map(p => [p.id, p]));
    const slow = reduced() ? 0 : 1;
    for (const [id, p] of Object.entries(PANELS)) {
      const all = byId.get(id)?.items ?? [];
      const tw = p.tileW || 1.3, td = 0.9, gap = 0.2;
      const cols = Math.max(1, Math.floor((p.w - 0.3) / (tw + gap)));
      // Số hàng vừa trong card: đáy ô cuối ≤ p.y + p.d - 0.1
      const rows = Math.max(1, Math.floor((p.d - HEADER - 0.1 + gap) / (td + gap)));
      const cap = cols * rows;
      let list = all;
      if (all.length > cap) {
        const hidden = all.slice(cap - 1);
        list = [...all.slice(0, cap - 1), {
          key: MORE, label: `+${hidden.length}`, value: 'khác',
          tip: `${p.title} · +${hidden.length} khác (click khối để xem đủ)\n` + hidden.map(h => `${h.key} · ${h.value}`).join('\n'),
        }];
      }
      const seen = new Set();
      list.forEach((it, i) => {
        const k = `${id}/${it.key}`;
        seen.add(k);
        const tx = p.x + 0.3 + (i % cols) * (tw + gap), ty = p.y + HEADER + Math.floor(i / cols) * (td + gap);
        const [cx, cy] = iso(tx + tw / 2, ty + td / 2);
        const tip = it.tip ?? `${p.title} · ${it.key} · ${it.value}`;
        let t = tiles.get(k);
        if (!t) {
          const g = el('g', { class: animate ? 'tile pop-in' : 'tile' }, groups[id]);
          const color = tileColor(id, it.key);
          const polygon = el('polygon', { points: diamond(tx, ty, tw, td), fill: color, 'fill-opacity': 0.06, stroke: color, 'stroke-width': 1.5 }, g);
          // Ô 2 dòng: tên trên, số dưới; có type thì thêm dòng nhỏ tên class ở trên cùng; panel valueOnly chỉ có số
          const text = el('text', { transform: flat(cx, cy), 'text-anchor': 'middle', fill: color }, g);
          const dy = it.type ? 2.5 : 0;
          if (it.type) el('tspan', { x: 0, y: -8.5, class: 'typ' }, text).textContent = it.type;
          const name = p.valueOnly ? null : el('tspan', { x: 0, y: -1.5 + dy }, text);
          if (name) name.textContent = it.label ?? it.key;
          const val = el('tspan', { x: 0, y: (p.valueOnly ? 3.5 : 9.5) + dy, class: 'val' }, text);
          val.textContent = String(it.value);
          const title = el('title', {}, g);
          title.textContent = tip;
          tiles.set(k, { g, polygon, text, name, val, title, value: it.value, anim: 0 });
          return;
        }
        t.polygon.setAttribute('points', diamond(tx, ty, tw, td));
        t.text.setAttribute('transform', flat(cx, cy));
        t.title.textContent = tip;
        if (t.name) t.name.textContent = it.label ?? it.key;
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
          t.val.textContent = String(it.value);
        }
        t.value = it.value;
      });
      for (const [k, t] of tiles) {
        if (!k.startsWith(id + '/') || seen.has(k)) continue;
        tiles.delete(k);
        if (animate) {
          t.g.classList.remove('pop-in'); t.g.classList.add('pop-out');
          setTimeout(() => t.g.remove(), 380 / speed);
        } else t.g.remove();
      }
    }
  }

  function flash(ids, ok) {
    for (const id of ids ?? []) {
      const f = frames[id];
      if (!f) continue; // panel lạ: bỏ qua
      f.classList.remove('flash-ok', 'flash-err'); void f.getBBox();
      f.classList.add(ok ? 'flash-ok' : 'flash-err');
    }
  }

  // Chỉ đường có action mới chạy: nét đứt chảy theo hướng data, mũi tên ở đích.
  // Flow không hỗ trợ → không animation, không chờ.
  async function flow(f, ok, ms) {
    const pts = resolveFlow(f);
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
    layers.flows.textContent = '';
    for (const f of Object.values(frames)) f.classList.remove('flash-ok', 'flash-err');
    sync(world, { animate: false });
  }

  enablePan(svg.parentElement);
  build();
  return { sync, flash, flow, reset, select };
}

// Kéo chuột để cuộn bản đồ; dịch chuyển < 4px vẫn là click (chọn/bỏ chọn panel)
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
