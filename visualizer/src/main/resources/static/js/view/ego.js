// Mô hình ego của tầng Instance (hàm thuần, không đụng DOM): hàng xóm của một node + bố cục thẻ trung tâm.
// egoNeighbours: ai nối với node (flow đã chạy, cha/con, dây nối); egoLayout: đặt thẻ và đi cạnh trực giao, đơn vị px.

export const CARD = { headH: 52, rowH: 14, padB: 10 }; // thẻ trung tâm: dải tiêu đề + mỗi dòng item + lề đáy (view vẽ theo cùng số)
export const REL_LABEL = { parent: 'container', child: 'con', wire: 'nối' };
export const MORE_ID = 'more:children';

const RANK = { parent: 0, in: 1, out: 2, wire: 3, child: 4 };
const sum = x => x.calls + x.events;
const cmp = (a, b) => (a < b ? -1 : a > b ? 1 : 0);

// Vai trò quyết định cột đặt thẻ: cha ở trên, con ở dưới; còn lại theo hướng flow (vào-thuần → trái; ra/hai chiều → phải; chỉ có dây nối → phải)
export function roleOf(n) {
  if (n.rel === 'parent') return 'parent';
  if (n.rel === 'child') return 'child';
  const i = sum(n.in), o = sum(n.out);
  if (i && !o) return 'in';
  return i || o ? 'out' : 'wire';
}

// Dòng nhỏ dưới thẻ hàng xóm: số flow ("2 lời gọi vào · 1 ra"), không có flow thì nhãn quan hệ cấu trúc
export function captionOf(n) {
  const i = sum(n.in), o = sum(n.out);
  if (!i && !o) return REL_LABEL[n.rel] ?? '';
  const calls = n.in.calls + n.out.calls, events = n.in.events + n.out.events;
  const unit = calls && !events ? 'lời gọi ' : events && !calls ? 'event ' : '';
  return [[i, 'vào'], [o, 'ra']].filter(p => p[0]).map(([k, dir], j) => `${k} ${j ? '' : unit}${dir}`).join(' · '); // đơn vị chỉ ghi ở phần đầu
}

const insideFn = (nodes, focusId) => id => {
  for (let cur = id, k = 0; cur != null && k < 64; cur = nodes.get(cur)?.parent, k++) if (cur === focusId) return true;
  return false;
};

// Flow (from → to) có chạm cây con của focus không: { other, dir } với other = đầu mút bên ngoài, dir 'in' (other → focus) | 'out'.
// null nếu hai đầu cùng trong (flow nội bộ) hoặc cùng ngoài focus
export function touchOf(focusId, nodes, from, to) {
  const inside = insideFn(nodes, focusId), a = inside(from), b = inside(to);
  if (a === b) return null;
  return a ? { other: to, dir: 'out' } : { other: from, dir: 'in' };
}

// nodes: Map id → node { id, title, sub, parent }; flows: [{ from, to, kind: 'CALL' | 'EVENT' }] (id node); wires: [{ a, b }]
// → [{ id, title, sub, in: { calls, events }, out: { calls, events }, rel: 'parent' | 'child' | 'wire' | null }], xếp theo vai trò rồi tiêu đề
export function egoNeighbours({ focusId, nodes, flows = [], wires = [] }) {
  const acc = new Map();
  const get = id => {
    const nd = nodes.get(id);
    if (!nd || id === focusId) return null;
    if (!acc.has(id)) acc.set(id, { id, title: nd.title ?? nd.cls ?? id, sub: nd.sub ?? '', in: { calls: 0, events: 0 }, out: { calls: 0, events: 0 }, rel: null });
    return acc.get(id);
  };
  const REL_PRI = { parent: 3, child: 2, wire: 1 };
  const link = (id, rel) => { const n = get(id); if (n && (REL_PRI[rel] > (REL_PRI[n.rel] ?? 0))) n.rel = rel; };

  for (const f of flows) {
    const t = touchOf(focusId, nodes, f.from, f.to), n = t && get(t.other);
    if (n) n[t.dir][f.kind === 'EVENT' ? 'events' : 'calls']++;
  }
  const focus = nodes.get(focusId);
  if (focus?.parent != null) link(focus.parent, 'parent');
  for (const nd of nodes.values()) if (nd.parent === focusId) link(nd.id, 'child');
  for (const w of wires) {
    if (w.a === focusId) link(w.b, 'wire'); else if (w.b === focusId) link(w.a, 'wire');
  }
  return [...acc.values()].sort((x, y) => RANK[roleOf(x)] - RANK[roleOf(y)] || cmp(x.title, y.title) || cmp(x.id, y.id));
}

const DEF = { cardW: 300, nbW: 170, nbH: 64, childW: 140, gapX: 90, gapY: 56, colGap: 18, childGap: 14, maxChildren: 8 };

// focus: { id, rows } (rows = số item); neighbours: kết quả egoNeighbours.
// → { rects: { id: {x,y,w,h} }, edges: [{ from (focus), to, points (từ viền focus tới viền thẻ), dir, structural, count }], width, height, more }
// Cột trái: chỉ-vào; cột phải: ra/hai chiều rồi chỉ-dây; cha ở trên; con thành một hàng dưới (tối đa maxChildren, dư gộp thành thẻ MORE_ID)
export function egoLayout(focus, neighbours, opts = {}) {
  const c = { ...DEF, ...opts };
  const fh = Math.max(84, CARD.headH + Math.max(focus.rows, 1) * CARD.rowH + CARD.padB);
  const by = role => neighbours.filter(n => roleOf(n) === role);
  const parent = by('parent')[0], left = by('in'), right = [...by('out'), ...by('wire')];
  const kids = by('child');
  const touched = n => sum(n.in) + sum(n.out) > 0;
  const shownKids = kids.length > c.maxChildren
    ? new Set([...kids].sort((a, b) => touched(b) - touched(a)).slice(0, c.maxChildren).map(n => n.id)) : null;
  const shown = shownKids ? kids.filter(n => shownKids.has(n.id)) : kids;
  const more = kids.length - shown.length;

  const colH = n => (n ? n * c.nbH + (n - 1) * c.colGap : 0);
  const fx = left.length ? c.nbW + c.gapX : 0, fy0 = parent ? c.nbH + c.gapY : 0;
  const midH = Math.max(fh, colH(left.length), colH(right.length)), fy = fy0 + (midH - fh) / 2;
  const rx = fx + c.cardW, fcx = fx + c.cardW / 2;
  const rects = { [focus.id]: { x: fx, y: fy, w: c.cardW, h: fh } };
  const column = (list, x) => list.forEach((n, i) => { rects[n.id] = { x, y: fy0 + (midH - colH(list.length)) / 2 + i * (c.nbH + c.colGap), w: c.nbW, h: c.nbH }; });
  column(left, 0); column(right, rx + c.gapX);
  if (parent) rects[parent.id] = { x: fcx - c.nbW / 2, y: 0, w: c.nbW, h: c.nbH };
  const kidIds = [...shown.map(n => n.id), ...(more ? [MORE_ID] : [])];
  const rowW = kidIds.length * c.childW + Math.max(0, kidIds.length - 1) * c.childGap, kidY = fy0 + midH + c.gapY;
  kidIds.forEach((id, i) => { rects[id] = { x: fcx - rowW / 2 + i * (c.childW + c.childGap), y: kidY, w: c.childW, h: c.nbH }; });

  // Cạnh: cổng rải đều dọc viền thẻ trung tâm; đoạn thân (trunk) của cạnh ngoài cùng gần thẻ trung tâm nhất để cạnh không cắt nhau
  const meta = new Map(neighbours.map(n => [n.id, n]));
  const flowDir = n => { const i = sum(n.in), o = sum(n.out); return i && o ? 'both' : i ? 'in' : o ? 'out' : null; };
  const lane = (i, n) => (Math.min(i, n - 1 - i) + 1) / (Math.ceil(n / 2) + 1); // 0..1: càng sâu vào giữa càng xa thẻ trung tâm
  const edges = [];
  const push = (id, points) => {
    const n = meta.get(id), dir = n ? flowDir(n) : null;
    edges.push({ from: focus.id, to: id, points: simplify(points), dir, structural: !dir, count: n ? sum(n.in) + sum(n.out) : 0 });
  };
  const side = (list, dirSign) => list.forEach((n, i) => { // dirSign -1: cột trái, +1: cột phải
    const k = list.length, py = fy + fh * (i + 1) / (k + 1), r = rects[n.id], ny = r.y + r.h / 2;
    const x0 = dirSign < 0 ? fx : rx, x1 = dirSign < 0 ? r.x + r.w : r.x, mx = x0 + dirSign * c.gapX * lane(i, k);
    push(n.id, [[x0, py], [mx, py], [mx, ny], [x1, ny]]);
  });
  side(left, -1); side(right, 1);
  if (parent) push(parent.id, [[fcx, fy], [fcx, rects[parent.id].y + c.nbH]]);
  kidIds.forEach((id, i) => {
    const k = kidIds.length, px = fx + c.cardW * (i + 1) / (k + 1), r = rects[id], cx = r.x + r.w / 2;
    const my = fy + fh + (kidY - fy - fh) * lane(i, k);
    push(id, [[px, fy + fh], [px, my], [cx, my], [cx, kidY]]);
  });

  // Dời về gốc (0,0): hàng con có thể rộng hơn cả bố cục
  const xs = Object.values(rects).flatMap(r => [r.x, r.x + r.w]), ys = Object.values(rects).flatMap(r => [r.y, r.y + r.h]);
  const dx = -Math.min(...xs), dy = -Math.min(...ys);
  for (const r of Object.values(rects)) { r.x += dx; r.y += dy; }
  for (const e of edges) e.points = e.points.map(([x, y]) => [x + dx, y + dy]);
  return { rects, edges, width: Math.max(...xs) + dx, height: Math.max(...ys) + dy, more };
}

// Bỏ điểm trùng và điểm giữa nằm thẳng hàng (đoạn vuông góc suy biến)
function simplify(pts) {
  const out = [];
  for (const p of pts) {
    if (out.length && out.at(-1)[0] === p[0] && out.at(-1)[1] === p[1]) continue;
    out.push(p);
    while (out.length >= 3) {
      const [a, b, d] = out.slice(-3);
      if ((a[0] === b[0] && b[0] === d[0]) || (a[1] === b[1] && b[1] === d[1])) out.splice(-2, 1); else break;
    }
  }
  return out;
}
