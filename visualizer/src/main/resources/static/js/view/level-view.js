// Bản đồ 2D phẳng cho tầng context / aggregate: khối chữ nhật + cạnh + mũi tên flow.
// Là <svg> thứ hai trong cùng panel bản đồ; router quyết định khi nào hiện. Không biết domain: nhận khối đã dựng sẵn.
import { el, css, sleep, arrowMarkers, restartFlash, fadeOut } from '../map/iso.js';
import { ellipsize } from '../dom.js';

const MAX_CHARS = 24; // tiêu đề dài hơn thì cắt (bản đầy đủ nằm ở <title>)

// Điểm trên viền chữ nhật r theo hướng về tâm của o
function anchor(r, o) {
  const cx = r.x + r.w / 2, cy = r.y + r.h / 2;
  const dx = o.x + o.w / 2 - cx, dy = o.y + o.h / 2 - cy;
  if (!dx && !dy) return [cx, cy];
  const k = Math.min(dx ? r.w / 2 / Math.abs(dx) : Infinity, dy ? r.h / 2 / Math.abs(dy) : Infinity);
  return [cx + dx * k, cy + dy * k];
}

export function createLevelView(svg) {
  let rects = {};
  let flowColor = {}; // màu mũi tên đọc một lần ở build()
  const frames = new Map(); // id khối → <rect>, để nháy
  const layers = {};

  function build() {
    svg.textContent = '';
    const defs = el('defs', {}, svg);
    // id riêng (lv-): svg isometric có bộ marker cùng tên mà có thể đang ẩn
    flowColor = { flow: css('--flow'), err: css('--err'), event: css('--event') };
    arrowMarkers(defs, 'lv-arrow-', flowColor);
    for (const n of ['edges', 'blocks', 'flows']) layers[n] = el('g', {}, svg);
  }

  // spec: { blocks: [{ id, title, sub, color, port, dim, touched }], edges: [{ a, b, label, tip }], rects, width, height, onOpen(block) }
  function render({ blocks, edges, rects: r, width, height, onOpen }) {
    rects = r;
    layers.edges.textContent = ''; layers.blocks.textContent = ''; frames.clear();
    svg.setAttribute('viewBox', `0 0 ${width} ${height}`);
    svg.setAttribute('width', width); svg.setAttribute('height', height);
    for (const e of edges) {
      const ra = r[e.a], rb = r[e.b];
      if (!ra || !rb) continue;
      const [x1, y1] = anchor(ra, rb), [x2, y2] = anchor(rb, ra);
      const g = el('g', { class: 'lv-edge' }, layers.edges);
      el('line', { x1, y1, x2, y2 }, g);
      if (e.label) {
        const t = el('text', { x: (x1 + x2) / 2, y: (y1 + y2) / 2 - 4, 'text-anchor': 'middle' }, g);
        t.textContent = e.label;
      }
      if (e.tip) el('title', {}, g).textContent = e.tip;
    }
    for (const b of blocks) {
      const p = r[b.id];
      if (!p) continue;
      const g = el('g', { class: 'lv-block' + (b.port ? ' port' : '') + (b.dim ? ' dim' : '') + (b.touched ? ' touched' : '') }, layers.blocks);
      const frame = el('rect', { x: p.x, y: p.y, width: p.w, height: p.h, style: `stroke:${b.color};fill:${b.color}` }, g);
      frames.set(b.id, frame);
      el('text', { x: p.x + 12, y: p.y + 28, class: 'lv-title' }, g).textContent = ellipsize(b.title, MAX_CHARS);
      el('text', { x: p.x + 12, y: p.y + 50, class: 'lv-sub' }, g).textContent = b.sub ?? '';
      el('title', {}, g).textContent = b.title;
      g.addEventListener('click', e => { e.stopPropagation(); onOpen?.(b); });
    }
  }

  function flash(ids, ok) {
    for (const id of ids ?? []) {
      const f = frames.get(id);
      if (!f) continue;
      restartFlash(f, ok);
    }
  }

  // Mũi tên nét đứt giữa tâm hai khối (flow event: nét chấm --event kèm nhãn), tự mờ đi sau ms. Khối lạ → không vẽ, không chờ
  async function flow({ from, to }, kind, ok, ms, label) {
    const a = rects[from], b = rects[to];
    if (!a || !b) return;
    const isEvent = kind === 'EVENT';
    const k = isEvent ? 'event' : ok ? 'flow' : 'err';
    const [x1, y1] = anchor(a, b), [x2, y2] = anchor(b, a);
    const p = el('path', {
      d: `M${x1} ${y1}L${x2} ${y2}`, class: isEvent ? 'flow event' : 'flow', stroke: flowColor[k], 'marker-end': `url(#lv-arrow-${k})`,
    }, layers.flows);
    let t = null;
    if (isEvent && label) {
      t = el('text', { x: (x1 + x2) / 2, y: (y1 + y2) / 2 - 6, class: 'flow-label', 'text-anchor': 'middle' }, layers.flows);
      t.textContent = label;
    }
    await sleep(ms);
    fadeOut(p, t);
  }

  function clearFlows() { layers.flows.textContent = ''; }

  build();
  return { render, flash, flow, clearFlows };
}
