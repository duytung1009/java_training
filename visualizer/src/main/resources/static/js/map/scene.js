// Vẽ bản đồ SVG: panel, ô con, đường nối và animation
import { el, iso, diamond, wirePath, css, flat, sleep, arrowMarkers, restartFlash, fadeOut, corners, screenRect, drawFlow, placeFlowPath, activatable, CELL } from './iso.js';
import { planeMatrix, apply, bounds, lerp, toAttr, fitRect, visibleRect, lerpRect } from './projection.js';
import { reduced, easeOut, tween } from '../anim.js';
import { dispVal, valPrefix, itemLabel } from '../items.js';
import { adapter } from '../../adapter/index.js';
import { touchedFirst } from '../view/nav.js';
import { ROOTS, PLACED, WIRES as WIRES_BASE, HEADER, CHILD_MARGIN, CHILD_GAP, computeLayout, resolveWire, checkLayout } from './layout.js';
import { clipWire, placePorts } from './ports.js';

const MORE = '__more'; // ô "+N" gom các item không đủ chỗ

// Màu ô item: "+N" cố định; còn lại do adapter.tileColor(rootId, key) quyết định, trả null/undefined thì dùng màu mặc định
function tileColor(rootId, key) {
  if (key === MORE) return '#7d8290';
  return adapter.tileColor?.(rootId, key) ?? '#cfd3dc';
}

const EPS = 1e-6;
const specOf = id => ROOTS[id] ?? PLACED[id]; // area hoặc thành viên cố định
const PROJ_MS = 400; // thời gian chuyển phép chiếu
const ZOOM_MIN = 0.4, ZOOM_MAX = 3;
const CAM_MS = 450, FOCUS_MARGIN = 16, FOCUS_MAX_SCALE = 2.5; // camera: thời gian tween, lề quanh area (đơn vị viewBox), scale tối đa khi zoom vào area
const PORT = { w: 4, d: 0.8, gap: 0.15 }; // khối cổng (đơn vị ô lưới) và khe tới mép area

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
const CHILD_HEADER = 0.9; // dải đầu khối con: tiêu đề + sub + khe ≥ 4px // lưới khối con trong container
const KIND_LABEL = { area: 'package' }; // nhãn loại ở cuối dòng sub; vùng gốc là một package domain
const FRAME = { // kiểu viền theo loại khối
  object: { fill: 0.05 },
  collection: { fill: 0.12, dash: '6 4' },
  area: { fill: 0, dash: '1 4', cap: 'round' },
};

// onSelect(id, node): click một khối (id null = bỏ chọn); onOpen(id): double click hoặc Enter trên khối (đi sâu vào);
// onGo(areaId): click hoặc Enter trên một cổng (sang area bên kia)
export function createScene(svg, { onSelect = () => {}, onOpen = () => {}, onGo = () => {} } = {}) {
  const layers = {}, frames = {};
  const blocks = new Map(); // id (root hoặc con) → { id, root, kind, color, small, g, frame, head, titleEl, subEl, rect, more }
  let world = null, selected = null, nodes = new Map();
  let layout = null, layoutKey = '', groundKey = '', wirePaths = [];
  let dimPred = null; // predicate(node) → boolean: khối nào làm mờ (class 'dim'); đặt qua setDim
  let priorityPred = null; // predicate(node) → boolean: node "đã chạm" được xếp trước khi container cắt "+N"; đặt qua setPriority
  let plane = planeMatrix('iso'), cancelProj = null; // ma trận phép chiếu áp lên lớp nền (đang chạy dở thì ở giữa hai đầu) + huỷ tween đang chạy
  let isoPts = []; // điểm bao nội dung (toạ độ iso); viewBox suy ra từ (plane, isoPts) mỗi khung hình
  const activeFlows = new Set(); // flow đang hiện: vẽ ở overlay nên phải ánh xạ lại khi ma trận đổi
  let zoom = 1, natW = 0, natH = 0; // kích thước tự nhiên theo viewBox; width/height = nat × zoom
  // Camera: focus = area đang zoom vào (null = tổng quan); tw = { from, t } khi đang tween (from = vùng nhìn lúc bắt đầu, t = tiến độ 0..1);
  // viewBox đích luôn suy ra lại từ (plane, layout, focus) ở applyView nên sync / đổi phép chiếu giữa chừng không làm camera nhảy
  let focus = null, tw = null, cancelCam = null, centre = null; // centre: area vừa rời, tâm cuộn khi quay về tổng quan
  let curVB = null; // viewBox hiện tại (đang tween thì là khung hình hiện tại)
  const warnedLayout = new Set();
  // Cổng tầng package: portList / portByWire / wireView tính lại từ (layout, focus) mỗi lần bố cục hoặc area đang xem đổi;
  // wireView[i] = { visible, pts }: đường i có hiện không và vẽ theo điểm nào (nguyên đường, hoặc phần trong area + stub tới cổng);
  // ports = DOM theo chỉ số đường; wireCount = số lời gọi đã qua mỗi đường (kể cả khi đang ở tổng quan)
  let portList = [], wireView = [], portsFocus;
  const portByWire = new Map(), ports = new Map(), wireCount = new Map(), areaColors = new Map();
  let flowColor = {};
  const tiles = new Map(); // `${blockId}/${key}` → { g, polygon, text, name, val, title, value, pre, cancel }

  function build() {
    svg.textContent = '';
    tiles.clear(); blocks.clear(); ports.clear(); portList = []; portByWire.clear(); wireView = []; layoutKey = ''; portsFocus = undefined;
    for (const k of Object.keys(frames)) delete frames[k];
    const defs = el('defs', {}, svg);
    flowColor = { flow: css('--flow'), err: css('--err'), event: css('--event') };
    arrowMarkers(defs, 'arrow-', flowColor);
    // Lớp nền (plane) chịu ma trận phép chiếu; overlay nằm ngoài để chữ/mũi tên flow luôn đứng thẳng, không méo
    const planeG = el('g', { class: 'plane' }, svg);
    for (const n of ['ground', 'wires', 'zones', 'ports']) layers[n] = el('g', {}, planeG);
    layers.overlay = el('g', {}, svg);
    layers.plane = planeG;
    activeFlows.clear();
    wirePaths = WIRES_BASE.map(() => el('path', { fill: 'none', stroke: css('--wire'), 'stroke-width': 1.5 }, layers.wires));
    groundKey = '';

    // Area vẽ trước (nền), thành viên cố định nằm trong group của area nên click sâu nhất thắng
    for (const [id, p] of Object.entries(ROOTS))
      setHead(makeBlock({ id, root: id, rect: p, parent: layers.zones, kind: p.kind }), p.title, p.sub);
    for (const [id, p] of Object.entries(PLACED))
      setHead(makeBlock({ id, root: id, rect: p, parent: blocks.get(p.area).g, kind: p.kind }), p.title, p.sub);
    svg.onclick = () => select(null);
    if (selected) frames[selected]?.classList.add('sel');
    layout = null;
    applyLayout(computeLayout([]), false);
    applyFade();
  }

  // Khối cục bộ: x/y gốc, d hiệu dụng (group cha/chính nó được tịnh tiến bằng transform khi bị đẩy xuống)
  const localRect = (id, lay) => ({ ...specOf(id), d: (lay.areas[id] ?? lay.placed[id]).d });

  function setShift(b, dy, animate) {
    if ((b.dy ?? 0) === dy) return;
    b.dy = dy;
    const [x, y] = iso(0, dy), [x0, y0] = iso(0, 0);
    if (!animate) b.g.style.transition = 'none';
    b.g.style.transform = `translate(${x - x0}px, ${y - y0}px)`;
    if (!animate) { void b.g.getBBox(); b.g.style.transition = ''; }
  }

  // Áp bố cục hiệu dụng: khung, độ dịch, và (khi bố cục hoặc area đang xem đổi) cổng, đường nối, nền, viewBox.
  // Tiêu đề khối / cổng không nằm ở đây: sync gọi setHead và renderPorts sau đó
  function applyLayout(lay, animate) {
    layout = lay;
    const key = JSON.stringify([lay.areas, lay.placed, lay.wires]), changed = key !== layoutKey || focus !== portsFocus;
    if (key !== layoutKey) {
      layoutKey = key;
      for (const err of checkLayout(lay.areas, lay.placed, lay.wires))
        if (!warnedLayout.has(err)) { warnedLayout.add(err); console.warn('Visualizer layout:', err); }
    }
    for (const id of [...Object.keys(ROOTS), ...Object.keys(PLACED)]) {
      const b = blocks.get(id);
      placeBlock(b, localRect(id, lay));
      setShift(b, lay.dy[id], animate);
    }
    if (!changed) return;
    computePorts();
    applyWires();
    // Nền + viewBox suy ra từ area và đường nối hiệu dụng
    const ps = Object.values(lay.areas);
    const maxX = Math.max(...ps.map(p => p.x + p.w)) + 0.5, maxY = Math.max(...ps.map(p => p.y + p.d)) + 0.5;
    isoPts = [...ps.flatMap(corners), ...lay.wires.flatMap(w => w.pts)].map(q => iso(...q));
    applyView();
    const gk = `${maxX}|${maxY}`;
    if (gk !== groundKey) {
      groundKey = gk; layers.ground.textContent = '';
      for (let x = -0.5; x < maxX; x++) for (let y = -0.5; y < maxY; y++)
        el('polygon', { points: diamond(x, y, 1, 1), fill: 'none', stroke: '#1b1f27' }, layers.ground);
    }
  }

  const boxSize = () => { const b = svg.parentElement; return { w: b.clientWidth, h: b.clientHeight }; };
  const setViewBox = r => { curVB = r; svg.setAttribute('viewBox', `${r.x} ${r.y} ${r.w} ${r.h}`); };
  // Hộp bao (sau phép chiếu) của area theo bố cục hiệu dụng; lề ở fitRect
  function areaBounds(id) {
    const a = layout.areas[id], rects = id === focus ? [a, ...portList.map(p => p.rect)] : [a]; // area đang xem: kèm các cổng
    return bounds(plane, rects.flatMap(corners).map(q => iso(...q)));
  }
  const M = 24, overviewVB = () => { const b = bounds(plane, isoPts); return { x: b.x - M, y: b.y - M, w: b.w + 2 * M, h: b.h + 2 * M }; };

  // Vùng nhìn đích khi tween kết thúc (đúng trạng thái cuối applyView sẽ áp): area = viewBox fit; tổng quan = zoom 1, cuộn giữa area vừa rời
  function targetView(bs) {
    if (focus) return fitRect(areaBounds(focus), bs, { margin: FOCUS_MARGIN, maxScale: FOCUS_MAX_SCALE }).viewBox;
    const vb = overviewVB(), W = Math.max(bs.w, vb.w), H = W * vb.h / vb.w;
    return visibleRect(vb, { w: W, h: H }, overviewScroll(vb, W, H, bs), bs);
  }
  // Cuộn để tâm area `centre` nằm giữa hộp (tổng quan, svg W×H)
  function overviewScroll(vb, W, H, bs) {
    if (!centre || !layout) return { left: 0, top: 0 };
    const a = areaBounds(centre);
    return { left: (a.x + a.w / 2 - vb.x) * W / vb.w - bs.w / 2, top: (a.y + a.h / 2 - vb.y) * H / vb.h - bs.h / 2 };
  }

  // Áp ma trận phép chiếu lên lớp nền + viewBox: tổng quan ôm nội dung (zoom nhân kích thước), area fit vào hộp, tween nội suy vùng nhìn
  function applyView() {
    layers.plane.setAttribute('transform', toAttr(plane));
    if (isoPts.length) {
      const bs = boxSize(), cam = (focus || tw) && bs.w > 0 && bs.h > 0 && layout;
      let vb, w, h, k = zoom;
      if (cam && tw) { // tween: svg bằng hộp, chỉ viewBox đổi nên như camera chạy
        vb = lerpRect(tw.from, targetView(bs), easeOut(tw.t)); w = bs.w; h = bs.h; k = 1;
      } else if (cam) {
        const f = fitRect(areaBounds(focus), bs, { margin: FOCUS_MARGIN, maxScale: FOCUS_MAX_SCALE });
        vb = f.viewBox; w = f.width; h = f.height;
      } else {
        vb = overviewVB(); w = vb.w; h = vb.h;
      }
      setViewBox(vb); natW = w; natH = h;
      applyZoom(k);
    }
    placeFlows();
  }

  // Chuyển phép chiếu 'iso' | 'flat': lerp ma trận (viewBox đi theo), giữ tâm vùng nhìn; lượt mới huỷ lượt cũ
  function setProjection(name, { animate = true } = {}) {
    const to = planeMatrix(name), from = plane, box = svg.parentElement;
    cancelProj?.(); cancelProj = null;
    const step = m => {
      const cw = box.clientWidth, ch = box.clientHeight;
      const rx = box.scrollWidth ? (box.scrollLeft + cw / 2) / box.scrollWidth : 0.5;
      const ry = box.scrollHeight ? (box.scrollTop + ch / 2) / box.scrollHeight : 0.5;
      plane = m; applyView();
      box.scrollLeft = rx * box.scrollWidth - cw / 2; box.scrollTop = ry * box.scrollHeight - ch / 2;
    };
    if (!animate || reduced()) return step(to);
    cancelProj = tween(PROJ_MS, t => step(t < 1 ? lerp(from, to, easeOut(t)) : to));
  }

  // Giữ viewBox, chỉ nhân width/height: thanh cuộn của .map-view giãn theo nên pan vẫn chạy bằng scroll
  function applyZoom(k = zoom) {
    const w = Math.round(natW * k), h = Math.round(natH * k);
    svg.setAttribute('width', w); svg.setAttribute('height', h);
    // Area / tween: kích thước px đặt thẳng (đè width:100% của CSS); tổng quan để CSS tự co theo bề ngang khung
    const fitted = !!(focus || tw);
    svg.style.width = fitted ? w + 'px' : ''; svg.style.height = fitted ? h + 'px' : '';
    svg.style.setProperty('--nat-w', w + 'px'); // min-width trong CSS: hẹp thì cuộn ngang
  }

  // Zoom quanh điểm (clientX, clientY): điểm dưới con trỏ đứng yên
  function zoomAt(next, clientX, clientY) {
    const box = svg.parentElement, r = box.getBoundingClientRect();
    next = Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, next));
    if (next === zoom || tw) return; // đang tween camera: không zoom
    const px = clientX - r.left, py = clientY - r.top, k = next / zoom;
    const sl = (box.scrollLeft + px) * k - px, st = (box.scrollTop + py) * k - py;
    zoom = next; applyZoom();
    box.scrollLeft = sl; box.scrollTop = st;
  }

  // Pinch trackpad: Chrome/Firefox/Edge gửi wheel + ctrlKey, Safari gửi gesture* với e.scale.
  // Cuộn 2 ngón (wheel không ctrlKey) để mặc định = pan. Double-click về 100%.
  function enableZoom(box) {
    box.addEventListener('wheel', e => {
      if (!e.ctrlKey) return;
      e.preventDefault(); // chặn trình duyệt zoom cả trang
      zoomAt(zoom * Math.exp(-e.deltaY * 0.01), e.clientX, e.clientY);
    }, { passive: false });
    let base = 1;
    box.addEventListener('gesturestart', e => { e.preventDefault(); base = zoom; });
    box.addEventListener('gesturechange', e => { e.preventDefault(); zoomAt(base * e.scale, e.clientX, e.clientY); });
    box.addEventListener('gestureend', e => e.preventDefault());
    box.addEventListener('dblclick', e => {
      if (zoom === 1) return;
      const r = box.getBoundingClientRect();
      zoomAt(1, r.left + box.clientWidth / 2, r.top + box.clientHeight / 2);
    });
  }

  // DOM chung của khối và cổng: nhóm, khung (fill = độ đậm nền), chữ tiêu đề + sub + tooltip. small: tiêu đề nhỏ hơn; area: tiêu đề nghiêng
  function blockDom(parent, color, { small = false, fill, dash, cap, cls, area = false }) {
    const g = el('g', { class: cls }, parent);
    const frame = el('polygon', { fill: color, 'fill-opacity': fill, stroke: color, 'stroke-width': small ? 1.2 : 1.5, class: 'zone-frame' }, g);
    if (dash) frame.setAttribute('stroke-dasharray', dash);
    if (cap) frame.setAttribute('stroke-linecap', cap);
    const head = el('text', { class: 'zone-head' + (small ? ' small' : '') + (area ? ' area' : '') }, g);
    const titleEl = el('tspan', { x: 0, y: 0, class: 'zone-title', fill: color }, head);
    const subEl = el('tspan', { x: 0, y: small ? 9 : 11, class: 'zone-sub' }, head);
    const tip = el('title', {}, head);
    return { g, frame, head, titleEl, subEl, tip };
  }

  // Dựng khung + tiêu đề một khối (root hoặc con). Con: tiêu đề nhỏ hơn, không có nhãn loại. selectable=false ("+N"): click rơi về container
  function makeBlock({ id, root, rect, parent, small = false, kind, cls = 'zone', selectable = true }) {
    const color = css(specOf(root).color), st = FRAME[kind] ?? FRAME.object;
    const dom = blockDom(parent, color, { small, fill: st.fill, dash: st.dash, cap: st.cap, cls, area: kind === 'area' });
    // Double click / Enter: đi sâu. Chặn nổi bọt để dblclick nền (reset zoom) không chạy khi trúng khối, và khối lồng nhau chỉ mở khối trong cùng
    activatable(dom.g, () => onOpen(id), { click: selectable && (() => select(selected === id ? null : id)), dbl: () => onOpen(id) });
    const b = { id, root, kind, color, small, ...dom, rect };
    blocks.set(id, b); frames[id] = dom.frame;
    placeBlock(b, rect);
    return b;
  }

  // Tiêu đề + sub (kèm nhãn loại ở cuối dòng sub nếu còn chỗ; khối con không có nhãn), vừa trong dải đầu khối
  function setHead(b, title, sub) {
    const mx = (b.rect.w - (b.small ? 0.22 : 0.3)) * CELL, base = b.small ? 7.5 : 9;
    fit(b.titleEl, title, mx, b.small ? 8.5 : 11);
    const label = KIND_LABEL[b.kind] ?? b.kind;
    const full = b.small || sub === label ? sub : (sub ? `${sub} · ${label}` : label);
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
    const from = t.value;
    t.cancel?.(); t.cancel = null;
    fit(t.val, t.pre + String(to), t.maxPx, t.valBase); // co chữ theo giá trị cuối để không nhảy cỡ khi đếm
    if (ms <= 0) return;
    t.val.textContent = t.pre + String(from);
    t.cancel = tween(ms, f => { t.val.textContent = t.pre + String(Math.round(from + (to - from) * f)); });
  }

  // Đồng bộ ô của một khối với danh sách item
  function syncTiles(b, all, spec, { animate, speed, slow }) {
    const { tw, td, gap, margin, head } = spec, r = b.rect;
    const maxPx = tw * CELL - 4, sm = b.small, valBase = sm ? 9 : 11;
    const fitTile = (t, it) => {
      fit(t.name, itemLabel(it), maxPx, sm ? 8 : 10);
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
        // Ô 2 dòng: tên trên, số dưới; có type thì thêm dòng nhỏ tên class ở trên cùng. Căn trái, cách mép 2px
        const text = el('text', { transform: flat(cx, cy), 'text-anchor': 'start', fill: color }, g);
        const dy = it.type ? 2.5 : 0, left = -tw * CELL / 2 + 2;
        const typ = it.type ? el('tspan', { x: left, y: -8.5, class: 'typ' }, text) : null;
        const name = el('tspan', { x: left, y: -1.5 + dy }, text);
        const val = el('tspan', { x: left, y: 9.5 + dy, class: 'val' }, text);
        const ttl = el('title', {}, g);
        ttl.textContent = tip;
        t = { g, polygon, text, name, val, typ, title: ttl, value: it.value, pre: valPrefix(it), cancel: null, maxPx, valBase };
        tiles.set(k, t);
        fitTile(t, it);
        return;
      }
      t.polygon.setAttribute('points', diamond(tx, ty, tw, td));
      if (dash) t.polygon.setAttribute('stroke-dasharray', dash); else t.polygon.removeAttribute('stroke-dasharray');
      t.text.setAttribute('transform', flat(cx, cy));
      t.title.textContent = tip;
      t.pre = valPrefix(it); t.maxPx = maxPx; t.valBase = valBase;
      fit(t.name, itemLabel(it), maxPx, sm ? 8 : 10);
      if (it.value === t.value) return;
      if (typeof it.value === 'number' && typeof t.value === 'number') {
        countTo(t, it.value, animate ? 500 / speed * slow : 0);
        if (animate) {
          t.g.classList.remove('pop-in', 'bump-up', 'bump-down');
          void t.g.getBBox();
          t.g.classList.add(it.value > t.value ? 'bump-up' : 'bump-down');
        }
      } else {
        t.cancel?.(); t.cancel = null;
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
    const { cols, cap } = layout.grids[rid];
    let shown = kids, hidden = [];
    if (kids.length > cap) { // node đã chạm xếp trước nên phần bị gộp vào "+N" là phần chưa chạm
      const ordered = touchedFirst(kids, priorityPred);
      shown = ordered.slice(0, cap - 1); hidden = ordered.slice(cap - 1);
    }
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
        b = makeBlock({ id: s.id, root: rid, rect, parent: blocks.get(rid).g, small: true, kind: 'object', cls: pop, selectable: !s.more });
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
    applyLayout(computeLayout(world?.nodes ?? []), animate);
    for (const id of [...Object.keys(ROOTS), ...Object.keys(PLACED)]) {
      const r = localRect(id, layout), b = blocks.get(id), n = nodeOf(id);
      setHead(b, n?.title ?? r.title, n?.sub ?? r.sub);
      if (r.child) syncChildren(id, r, [...nodes.values()].filter(c => c.parent === id), opts);
      else if (r.kind === 'object') syncTiles(b, n?.items ?? [], { tw: r.tileW || 1.3, td: 0.9, gap: 0.2, margin: 0.3, head: HEADER }, opts);
    }
    if (selected && !frames[selected]) select(null);
    else if (selected) onSelect(selected, describe(selected));
    renderPorts(); // tiêu đề node có thể đổi (bố cục đổi thì applyLayout đã tính lại cổng)
    applyDim();
    applyFade();
  }

  // Khối được tạo lại theo node nên phải áp lại sau mỗi sync/reset; khối không có node (vd. "+N") không mờ
  function applyDim() {
    for (const b of blocks.values()) {
      const n = nodeOf(b.id);
      b.g.classList.toggle('dim', !!(dimPred && n && dimPred(n)));
    }
  }

  // predicate(node) → true thì khối mờ đi; null bỏ mờ
  function setDim(predicate) { dimPred = predicate ?? null; applyDim(); }

  // predicate(node) → true thì node được ưu tiên giữ lại khi container quá chỗ; hiệu lực từ lần sync kế tiếp
  function setPriority(predicate) { priorityPred = predicate ?? null; }

  function flash(ids, ok) {
    for (const id of ids ?? []) {
      const f = frames[id];
      if (!f) continue; // id lạ: bỏ qua
      restartFlash(f, ok);
    }
  }

  // Đếm lời gọi qua đường nối (mọi chế độ, để vào area giữa chừng vẫn đúng số) và cập nhật dòng sub của cổng tương ứng; không vẽ.
  // `after`: world sau của flow (con mới chưa có trong world hiện tại). Trả { i, forward } hoặc null nếu flow không có đường
  function countFlow(f, after) {
    const afterNodes = after?.nodes && new Map(after.nodes.map(n => [n.id, n]));
    const r = resolveWire(f, id => nodes.get(id)?.parent ?? afterNodes?.get(id)?.parent, layout.wires);
    if (!r) return null;
    wireCount.set(r.i, (wireCount.get(r.i) ?? 0) + 1);
    const t = portByWire.get(r.i);
    if (t) renderPort(t);
    return r;
  }

  // Chỉ đường có action mới chạy: nét đứt chảy theo hướng data, mũi tên ở đích.
  // Flow không hỗ trợ → không animation, không chờ.
  async function flow(f, ok, ms, after) {
    const r = countFlow(f, after);
    if (!r) return;
    const v = wireView[r.i];
    if (!v.visible) return; // đường ngoài area đang xem: không vẽ, không chờ
    const t = portByWire.get(r.i); // cắt tại cổng: đi từ trong ra cổng (hoặc ngược lại)
    const pts = (t ? r.forward === t.inA : r.forward) ? v.pts : v.pts.slice().reverse();
    const fl = { ...drawFlow(layers.overlay, { kind: f.kind, ok, label: f.label, markerPrefix: 'arrow-', colors: flowColor }), pts };
    activeFlows.add(fl);
    placeFlow(fl);
    await sleep(ms);
    activeFlows.delete(fl);
    fadeOut(fl.p, fl.label);
  }

  // Flow vẽ ở overlay (mũi tên marker không bị méo theo ma trận) nên điểm phải ánh xạ qua `plane`; nhãn đặt ở giữa đường
  function placeFlow(fl) { placeFlowPath(fl, fl.pts.map(q => apply(plane, iso(...q)))); }
  function placeFlows() { for (const fl of activeFlows) placeFlow(fl); }

  // ---- Điều hướng: package (area) và camera ----
  const areas = () => Object.entries(ROOTS).map(([id, p]) => ({ id, title: nodeOf(id)?.title ?? p.title }));

  // Area chứa khối/node `id`: root là chính nó, khối cố định theo PLACED, khối con theo container, node không vẽ (gom "+N") theo chuỗi parent
  function areaOf(id) {
    let cur = blocks.get(id)?.root ?? id;
    for (let i = 0; cur != null && i < 8; i++) {
      if (ROOTS[cur]) return cur;
      if (PLACED[cur]) return PLACED[cur].area;
      cur = nodes.get(cur)?.parent;
    }
    return null;
  }

  // Màu area chứa `id` (không thuộc area nào: --muted); đọc CSS một lần cho mỗi area
  function areaColor(id) {
    const area = areaOf(id) ?? '';
    if (!areaColors.has(area)) areaColors.set(area, css(ROOTS[area]?.color ?? '--muted'));
    return areaColors.get(area);
  }

  // Dây đang hiện: nguyên đường, hoặc phần trong area + stub tới cổng khi đang xem một area
  function applyWires() { wireView.forEach((v, i) => wirePaths[i].setAttribute('d', wirePath(v.pts))); }

  // Cổng của area đang xem: mỗi đường chỉ có một đầu trong area bị cắt tại chỗ rời area đầu tiên.
  // Điền wireView (đường không chạm area thì ẩn), portList, portByWire
  function computePorts() {
    portsFocus = focus;
    portList = []; portByWire.clear();
    const wires = layout?.wires ?? [];
    wireView = wires.map(w => ({ visible: true, pts: w.pts }));
    if (!focus || !layout) return;
    const rect = layout.areas[focus], items = [];
    wires.forEach((w, i) => {
      const inA = areaOf(w.a) === focus, inB = areaOf(w.b) === focus;
      if (!inA && !inB) { wireView[i].visible = false; return; }
      if (inA === inB) return;
      const c = clipWire(w.pts, rect, inA);
      if (c) items.push({ i, c, inA, other: inA ? w.b : w.a });
    });
    const placed = placePorts(items.map(t => t.c), rect, PORT);
    items.forEach((t, k) => {
      const port = { i: t.i, inA: t.inA, other: t.other, rect: placed[k], shown: [...t.c.inner, ...placed[k].stub.slice(1)] };
      portList.push(port); portByWire.set(t.i, port);
      wireView[t.i].pts = port.shown;
    });
  }

  // Dựng / cập nhật / gỡ khối cổng theo portList (idempotent: gọi lại sau layout, sync)
  function renderPorts() {
    for (const [i, p] of ports) if (!portByWire.has(i)) { p.g.remove(); ports.delete(i); }
    for (const t of portList) renderPort(t);
  }

  // Một khối cổng: tạo nếu chưa có; tiêu đề / vị trí chỉ đặt lại khi [title, sub, rect] đổi
  function renderPort(t) {
    let p = ports.get(t.i);
    if (!p) {
      const dom = blockDom(layers.ports, '', { small: true, fill: 0.12, cls: 'port' });
      p = { ...dom, small: true, kind: 'port', other: null, key: '' };
      activatable(dom.g, () => onGo(p.area), { dbl: true });
      ports.set(t.i, p);
    }
    if (p.other !== t.other) { // đầu mút bên kia đổi (đi thẳng từ area này sang area kia): màu và đích của cổng đổi theo
      p.other = t.other; p.key = '';
      p.area = areaOf(t.other); p.color = areaColor(t.other);
      p.frame.setAttribute('fill', p.color); p.frame.setAttribute('stroke', p.color); p.titleEl.setAttribute('fill', p.color);
    }
    const n = wireCount.get(t.i) ?? 0, ot = nodeOf(t.other)?.title ?? specOf(t.other)?.title ?? t.other;
    const at = nodeOf(p.area)?.title ?? ROOTS[p.area]?.title ?? p.area; // đầu mút là chính area: không lặp tên ở dòng phụ
    const title = `→ ${ot}`, sub = [t.other === p.area ? '' : at, n ? `${n} lời gọi` : ''].filter(Boolean).join(' · ');
    const key = JSON.stringify([title, sub, t.rect]);
    if (key === p.key) return;
    p.key = key;
    placeBlock(p, t.rect);
    setHead(p, title, sub);
  }

  // Làm mờ phần ngoài area đang xem: khối gốc của area khác và đường nối không chạm area; khối lồng nhau mờ theo cha
  function applyFade() {
    for (const b of blocks.values()) {
      const out = !!focus && areaOf(b.id) !== focus;
      if (ROOTS[b.id]) b.g.classList.toggle('out', out);
      b.g.setAttribute('tabindex', out ? -1 : 0);
    }
    wireView.forEach((v, i) => wirePaths[i].classList.toggle('out', !v.visible));
  }

  // Khung khối `id` trên màn hình (px viewport) cho hiệu ứng chuyển cảnh; null nếu khối không có hoặc đang ẩn
  function blockRect(id) {
    return screenRect(blocks.get(id)?.frame);
  }

  // Vùng viewBox đang thấy trên màn hình (đang tween thì là khung hình hiện tại)
  function currentView() {
    if (tw) return curVB;
    const box = svg.parentElement, r = svg.getBoundingClientRect();
    if (!curVB || !r.width || !r.height) return null;
    return visibleRect(curVB, { w: r.width, h: r.height }, { left: box.scrollLeft, top: box.scrollTop }, boxSize());
  }

  // Trạng thái cuối, không tween: áp khung rồi đặt cuộn (area: 0; tổng quan: giữa area vừa rời)
  function settleCamera() {
    tw = null;
    applyView();
    const box = svg.parentElement;
    if (focus) { box.scrollLeft = 0; box.scrollTop = 0; return; }
    const vb = curVB, r = svg.getBoundingClientRect(), bs = boxSize();
    if (!vb || !r.width) return;
    const s = overviewScroll(vb, r.width, r.height, bs);
    box.scrollLeft = s.left; box.scrollTop = s.top;
  }

  // Camera: id = area cần zoom vào, null = về tổng quan. Tween ~450ms từ vùng đang thấy tới vùng đích (lượt mới huỷ lượt cũ)
  function focusArea(id, { animate = true } = {}) {
    id = id && ROOTS[id] ? id : null;
    if (id === focus) return;
    cancelCam?.(); cancelCam = null;
    const from = animate && !reduced() ? currentView() : null;
    if (focus) centre = focus;
    focus = id; zoom = 1;
    computePorts(); applyWires(); renderPorts();
    applyFade();
    if (!from || !layout) return settleCamera();
    tw = { from, t: 0 };
    applyView();
    cancelCam = tween(CAM_MS, t => { tw.t = t; if (t < 1) applyView(); }, settleCamera);
  }

  // Về trạng thái ban đầu, không animation
  function reset(world) {
    for (const t of tiles.values()) t.g.remove();
    tiles.clear();
    for (const b of [...blocks.values()]) if (b.small) removeBlock(b, { animate: false });
    layers.overlay.textContent = ''; activeFlows.clear(); wireCount.clear();
    for (const f of Object.values(frames)) f.classList.remove('flash-ok', 'flash-err');
    sync(world, { animate: false });
  }

  enablePan(svg.parentElement);
  enableZoom(svg.parentElement);
  build();
  // Hộp đổi kích thước khi đang zoom vào area: fit lại (tổng quan để CSS tự co; đang tween thì frame kế tiếp tự lấy kích thước mới)
  new ResizeObserver(() => { if (focus && !tw) applyView(); }).observe(svg.parentElement);
  return { svg, sync, flash, flow, countFlow, reset, select, setDim, setPriority, setProjection, areas, areaOf, areaColor, focusArea, blockRect };
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
