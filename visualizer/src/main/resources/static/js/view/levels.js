// Xem theo tầng: context map → aggregate → instance. Hàm thuần (không đụng DOM) để test bằng node --test.
// Trạng thái tầng: { level, ctx, agg }; khối (block) là đơn vị vẽ ở mỗi tầng, id khối có tiền tố:
//   ctx:<context> · agg:<aggregate hoặc class> · port:<context khác> · (tầng instance) chính node id.
import { classOfNode, contextOfNode, aggregateOf, lookup, contextById } from '../model/draft.js';
import { guessByPrefix } from '../map/layout.js';
import { layeredLayout } from './graph-layout.js';

const isObject = n => n.kind === 'object';

export const LEVELS = ['context', 'aggregate', 'instance'];
export const DEFAULT_STATE = Object.freeze({ level: 'instance', ctx: null, agg: null });
export const PALETTE_SIZE = 6; // --lv-1..--lv-6 trong styles.css

// "#level=aggregate&ctx=sales&agg=Invoice" ↔ { level, ctx, agg }; hash lạ → trạng thái mặc định
export function parseHash(hash) {
  const p = new URLSearchParams(String(hash ?? '').replace(/^#/, ''));
  const level = p.get('level');
  if (!LEVELS.includes(level)) return { ...DEFAULT_STATE };
  return { level, ctx: p.get('ctx') || null, agg: p.get('agg') || null };
}

export function toHash(state) {
  const p = new URLSearchParams({ level: state.level });
  if (state.ctx) p.set('ctx', state.ctx);
  if (state.agg) p.set('agg', state.agg);
  return '#' + p.toString();
}

// Trạng thái hiệu dụng theo mô hình: không có mô hình → instance; context lạ → bỏ; tầng aggregate cần ctx (mặc định context đầu tiên)
export function normalizeState(state, model) {
  const first = model?.contexts?.[0]?.id ?? null;
  if (!model || !first) return { ...DEFAULT_STATE };
  const known = id => !!contextById(model, id);
  const ctx = known(state.ctx) ? state.ctx : null;
  if (state.level === 'context') return { level: 'context', ctx: null, agg: null };
  if (state.level === 'aggregate') return { level: 'aggregate', ctx: ctx ?? first, agg: null };
  return { level: 'instance', ctx, agg: ctx ? state.agg ?? null : null };
}

// id → node: ưu tiên world sau, rồi hiện tại, rồi trước; id lạ thì đoán đầu mút theo tiền tố (prefixes: [[tiềnTố, idGốc]])
export function nodeLookup(worldBefore, worldNow, worldAfter, prefixes = []) {
  const byId = new Map();
  for (const w of [worldBefore, worldNow, worldAfter]) for (const n of w?.nodes ?? []) byId.set(n.id, n);
  return id => {
    if (id == null) return undefined;
    const n = byId.get(id);
    if (n) return n;
    const root = guessByPrefix(id, prefixes);
    return root != null ? byId.get(root) : undefined;
  };
}

// Focus ở tầng instance: có ctx (agg tuỳ chọn) thì chỉ vẽ phần thuộc focus ở bản đồ 2D thu gọn
export const hasFocus = state => state.level === 'instance' && !!state.ctx;

// Khoá aggregate của object nằm trong focus (object, class không ẩn, đúng context, đúng aggregate nếu có); ngoài focus → null.
// Cùng ngữ nghĩa với inSelection ở filters.js nhưng chỉ nhận object và bỏ class ẩn (khối con chưa xếp / ẩn không có chỗ vẽ)
function focusKey(state, model, node) {
  if (!isObject(node)) return null;
  const cls = classOfNode(node), def = lookup(model, cls);
  if (!def || def.hidden || def.context !== state.ctx) return null;
  const key = aggregateKey(model, cls);
  return state.agg && key !== state.agg ? null : key;
}

// Cổng của node nằm ngoài focus: context khác → port:<ctx>; cùng context nhưng khác aggregate (khi đang focus aggregate) → port:agg:<key>
function focusPort(state, model, node) {
  const ctx = contextOfNode(model, node);
  if (ctx == null) return null;
  if (ctx !== state.ctx) return `port:${ctx}`;
  if (!state.agg || node.kind === 'area') return null;
  const cls = classOfNode(node);
  if (cls == null) return null;
  const key = aggregateKey(model, cls);
  return key === state.agg ? null : `port:agg:${key}`;
}

// Khối chứa node ở tầng hiện tại; null nếu node không có khối ở tầng này.
// inst (tuỳ chọn, kết quả buildInstanceBlocks): ở tầng instance có focus, node bị gộp vào khối "+N" thì trả khối đó
export function blockOf(state, model, node, inst = null) {
  if (!node) return null;
  if (state.level === 'instance') {
    if (!model || !state.ctx) return node.id;
    return focusKey(state, model, node) != null ? inst?.hiddenOf?.get(node.id) ?? node.id : focusPort(state, model, node);
  }
  if (!model) return null;
  const ctx = contextOfNode(model, node);
  if (ctx == null) return null;
  if (state.level === 'context') return `ctx:${ctx}`;
  if (ctx !== state.ctx) return `port:${ctx}`;
  if (node.kind === 'area') return null;
  const cls = classOfNode(node);
  if (cls == null) return null;
  return `agg:${aggregateKey(model, cls)}`;
}

// Khoá aggregate của một class: aggregate chứa nó, không có thì chính class (service, value lẻ…)
export const aggregateKey = (model, cls) => aggregateOf(model, cls) ?? cls;

// Màu context: color của mô hình (tên biến CSS) hoặc bảng màu cố định theo thứ tự
export function contextColor(model, ctxId) {
  const i = (model?.contexts ?? []).findIndex(c => c.id === ctxId);
  const c = i >= 0 ? model.contexts[i].color : null;
  if (typeof c === 'string' && c.startsWith('--')) return `var(${c})`;
  return `var(--lv-${(Math.max(i, 0) % PALETTE_SIZE) + 1})`;
}

// Cặp không phân biệt hướng: [nhỏ, lớn]
export const orderedPair = (a, b) => (a < b ? [a, b] : [b, a]);
export const pairKey = (a, b) => orderedPair(a, b).join('|');

// Context của hai đầu quan hệ r (undefined nếu class không có trong mô hình)
const relContexts = (model, r) => [lookup(model, r.from)?.context, lookup(model, r.to)?.context];

export const MAX_TIPS = 10;        // số quan hệ tối đa liệt kê trong tooltip của một cạnh
const CTX_AGG_LINES = 4;           // số tên aggregate hiện trong khối context, dư thì thêm "+n"
const MEMBER_ROWS = 6;             // số dòng entity/value hiện trong khung aggregate, dư thì thêm "+n"
const SUB_CHARS = 30;              // số ký tự dòng phụ vừa trong thẻ 210px (chữ 11px)
const ROLE_ORDER = ['aggregate', 'entity', 'value', 'service', 'other'];
const isObservedSource = src => String(src ?? '').startsWith('observed');

// Độ dày nét theo số quan hệ gộp: 1px (1–2 quan hệ), 2px (3–4), 3px (từ 5)
export const edgeWidth = n => Math.min(3, 1 + Math.floor(Math.max(0, n - 1) / 2));

// Ghép các phần bằng " · " thành nhiều dòng, mỗi dòng không quá max ký tự (một phần không bị cắt đôi)
export function wrapParts(parts, max) {
  const out = [];
  for (const p of parts) {
    if (out.length && out.at(-1).length + 3 + p.length <= max) out[out.length - 1] += ` · ${p}`;
    else out.push(p);
  }
  return out;
}

// Gom quan hệ (rels: [{ a, b, style, tip }], a PHỤ THUỘC b) và flow đã đếm (counts, countFlows) thành MỘT cạnh cho mỗi cặp khối.
// Hướng cạnh: a phụ thuộc b theo đa số quan hệ trong mô hình (hoà → khối có id nhỏ hơn phụ thuộc); không có quan hệ thì theo đa số flow
// (flow từ X tới Y nghĩa là X gọi Y). strength: các style xếp từ mạnh tới yếu, cạnh lấy style mạnh nhất; chỉ có flow → 'observed'.
// valid: Set id khối được phép; cạnh có đầu ngoài tập này bị bỏ.
function mergeEdges(rels, counts, strength, valid) {
  const pairs = new Map();
  const entry = (x, y) => {
    const [lo, hi] = orderedPair(x, y), k = `${lo}|${hi}`;
    let e = pairs.get(k);
    if (!e) pairs.set(k, e = { lo, hi, loDeps: 0, hiDeps: 0, flowsLo: 0, flowsHi: 0, calls: 0, events: 0, rels: [] });
    return e;
  };
  for (const r of rels) {
    if (!valid.has(r.a) || !valid.has(r.b) || r.a === r.b) continue;
    const e = entry(r.a, r.b);
    if (r.a === e.lo) e.loDeps++; else e.hiDeps++;
    e.rels.push(r);
  }
  for (const c of counts?.values() ?? []) {
    if (!valid.has(c.a) || !valid.has(c.b)) continue;
    const e = entry(c.a, c.b); // countFlows đã sắp a < b nên c.a luôn là lo
    e.flowsLo += c.fwd ?? 0; e.flowsHi += c.back ?? 0; e.calls += c.calls; e.events += c.events;
  }
  return [...pairs.values()].map(e => {
    const loDepends = e.rels.length ? e.loDeps >= e.hiDeps : e.flowsLo >= e.flowsHi;
    const [a, b] = loDepends ? [e.lo, e.hi] : [e.hi, e.lo];
    const style = e.rels.map(r => r.style).sort((p, q) => strength.indexOf(p) - strength.indexOf(q))[0] ?? 'observed';
    const shown = e.rels.slice(0, MAX_TIPS).map(r => r.tip);
    if (e.rels.length > MAX_TIPS) shown.push(`… +${e.rels.length - MAX_TIPS}`);
    return {
      id: `${a}>${b}`, a, b, relations: e.rels.length, style, calls: e.calls, events: e.events,
      tip: shown.length ? shown.join('\n') : 'Chỉ có lời gọi/event đã chạy, chưa có quan hệ trong mô hình',
    };
  });
}

// Khối context (theo thứ tự model.contexts, bỏ context không có class) + cạnh: một cạnh cho mỗi cặp context có quan hệ hoặc flow.
// Cạnh { id, a, b, relations, style: 'solid'|'observed', calls, events, tip }: a phụ thuộc b (b là upstream).
// counts (tuỳ chọn, countFlows ở tầng context): flow đã chạy, thêm cạnh 'observed' cho cặp chưa có quan hệ.
export function buildContextBlocks(model, world, counts = new Map()) {
  const has = new Set((model?.classes ?? []).map(c => c.context).filter(Boolean));
  const count = new Map();
  for (const n of world?.nodes ?? []) {
    if (!isObject(n)) continue;
    const c = contextOfNode(model, n);
    if (c != null) count.set(c, (count.get(c) ?? 0) + 1);
  }
  const blocks = (model?.contexts ?? []).filter(c => has.has(c.id)).map(c => {
    const own = model.classes.filter(k => k.context === c.id && !k.hidden);
    const roles = ROLE_ORDER.map(r => [r, own.filter(k => k.role === r).length]).filter(([, n]) => n);
    const aggs = own.filter(k => k.role === 'aggregate').map(k => k.cls);
    const lines = aggs.slice(0, CTX_AGG_LINES);
    if (aggs.length > CTX_AGG_LINES) lines.push(`+${aggs.length - CTX_AGG_LINES}`);
    const n = count.get(c.id) ?? 0;
    const parts = roles.map(([r, k]) => `${k} ${r}`);
    const subLines = wrapParts(parts.length ? parts : ['chưa có class'], SUB_CHARS); // tóm tắt dài thì xuống dòng, khối cao thêm
    const lineBase = 45 + subLines.length * 13;
    return {
      id: `ctx:${c.id}`, ctx: c.id, title: c.title ?? c.id, count: n, color: contextColor(model, c.id),
      right: `${n} instance`, sub: parts.join(' · ') || 'chưa có class', subLines, lines,
      w: 210, h: lines.length ? lineBase - 4 + lines.length * 14 : 39 + subLines.length * 13,
    };
  });
  const valid = new Set(blocks.map(b => b.id));
  const rels = [];
  for (const r of model?.relations ?? []) {
    const [ca, cb] = relContexts(model, r);
    if (!ca || !cb || ca === cb) continue;
    rels.push({ a: `ctx:${ca}`, b: `ctx:${cb}`, style: isObservedSource(r.source) ? 'observed' : 'solid', tip: `${r.from} → ${r.to} (${r.source})` });
  }
  return { blocks, edges: mergeEdges(rels, counts, ['solid', 'observed'], valid) };
}

// Khối aggregate trong context ctx (theo thứ tự model.classes, bỏ class ẩn) + cổng sang các context khác có quan hệ hoặc flow + cạnh.
// Khối có `box` (gốc aggregate) vẽ thành khung nét đứt kèm `members` (entity/value thuộc nó); service và entity/value lẻ là khối đặc nhỏ.
// Cạnh { id, a, b, relations, style: 'id'|'param'|'observed', calls, events, tip }: a phụ thuộc b. Quan hệ trong cùng một khối bỏ qua;
// field giữa hai khối khác nhau là tham chiếu theo id ('id'), tham số thuần là 'param', quan hệ quan sát được là 'observed'.
export function buildAggregateBlocks(model, world, ctx, counts = new Map()) {
  const state = { level: 'aggregate', ctx, agg: null };
  const blocks = [], seen = new Set();
  for (const c of model?.classes ?? []) {
    if (c.context !== ctx || c.hidden) continue;
    const key = aggregateKey(model, c.cls);
    if (seen.has(key)) continue;
    seen.add(key);
    const def = lookup(model, key);
    const role = def?.role ?? c.role, color = contextColor(model, ctx);
    const box = role === 'aggregate';
    const members = box ? model.classes.filter(k => k.context === ctx && !k.hidden && k.cls !== key && aggregateKey(model, k.cls) === key) : [];
    blocks.push({ id: `agg:${key}`, key, ctx, title: key, role, count: 0, color, box, members: members.map(k => ({ name: k.cls, tag: k.role })) });
  }
  const byId = new Map(blocks.map(b => [b.id, b]));
  for (const n of world?.nodes ?? []) {
    if (!isObject(n)) continue;
    const b = byId.get(blockOf(state, model, n));
    if (b) b.count++;
  }
  for (const b of blocks) {
    if (b.box) {
      const rows = b.members.slice(0, MEMBER_ROWS), more = b.members.length - rows.length;
      b.sub = `aggregate · ${b.count} instance`;
      b.memberRows = more > 0 ? [...rows, { name: `+${more}`, tag: '' }] : rows;
      b.w = 210; b.h = b.memberRows.length ? 51 + b.memberRows.length * 16 : 42;
    } else {
      b.sub = `${b.role} · ${b.count} instance`;
      b.w = 170; b.h = 52;
    }
  }
  const classBlock = cls => {
    const def = lookup(model, cls);
    if (!def?.context) return null;
    return def.context === ctx ? `agg:${aggregateKey(model, cls)}` : `port:${def.context}`;
  };
  const rels = [];
  const other = new Set();
  for (const r of model?.relations ?? []) {
    const a = classBlock(r.from), b = classBlock(r.to);
    if (!a || !b || a === b) continue;
    if (a.startsWith('port:') && b.startsWith('port:')) continue;
    for (const id of [a, b]) if (id.startsWith('port:')) other.add(id.slice(5));
    const style = isObservedSource(r.source) ? 'observed' : r.source === 'field' ? 'id' : 'param';
    rels.push({ a, b, style, tip: `${r.from} → ${r.to} (${r.source})` });
  }
  for (const c of counts.values()) for (const id of [c.a, c.b]) if (id.startsWith('port:') && contextById(model, id.slice(5))) other.add(id.slice(5));
  const ports = (model?.contexts ?? []).filter(c => other.has(c.id)).map(c => ({
    id: `port:${c.id}`, ctx: c.id, title: `→ ${c.title ?? c.id}`, sub: 'cổng sang context khác', port: true, count: 0, color: contextColor(model, c.id), w: 170, h: 52,
  }));
  const valid = new Set([...blocks, ...ports].map(b => b.id));
  return { blocks, ports, edges: mergeEdges(rels, counts, ['id', 'param', 'observed'], valid) };
}

// Flow ở tầng hiện tại: mũi tên giữa hai khối khác nhau, nháy khi cùng khối, null nếu một đầu không có khối
export function resolveFlowAt(state, model, lookupNode, flow, inst = null) {
  if (!flow) return null;
  const from = blockOf(state, model, lookupNode(flow.from), inst);
  const to = blockOf(state, model, lookupNode(flow.to), inst);
  if (from == null || to == null) return null;
  return from === to ? { kind: 'flash', block: from } : { kind: 'arrow', from, to };
}

// Đếm flow theo cặp khối (không phân biệt hướng): Map "a|b" → { a, b, calls, events, fwd, back }; chỉ tính mũi tên.
// fwd = số flow đi từ a tới b, back = từ b tới a (a < b theo orderedPair)
export function countFlows(state, model, lookupNode, flows) {
  const out = new Map();
  for (const f of flows) {
    const r = resolveFlowAt(state, model, lookupNode, f);
    if (r?.kind !== 'arrow') continue;
    const k = pairKey(r.from, r.to);
    let e = out.get(k);
    if (!e) { const [a, b] = orderedPair(r.from, r.to); e = { a, b, calls: 0, events: 0, fwd: 0, back: 0 }; out.set(k, e); }
    if (r.from === e.a) e.fwd++; else e.back++;
    if (f.kind === 'EVENT') e.events++; else e.calls++;
  }
  return out;
}

// Bố cục phân tầng cho tầng context / aggregate. blocks có w, h; edges { id, a, b } với a PHỤ THUỘC b nên b (upstream) nằm trên a.
// layeredLayout đặt `from` ở trên `to`, do đó cạnh đưa vào là { from: b, to: a }; đường trả về đảo lại để đi từ a (phụ thuộc) tới b (upstream).
// Trả { rects: { id → { x, y, w, h } }, routes: { idCạnh → [[x, y], …] }, width, height }
export function layoutGraph(blocks, edges, opts = {}) {
  const lay = layeredLayout(blocks.map(b => ({ id: b.id, w: b.w, h: b.h })), edges.map(e => ({ id: e.id, from: e.b, to: e.a })), opts);
  const rects = {}, routes = {};
  for (const [id, p] of Object.entries(lay.nodes)) rects[id] = { x: p.x, y: p.y, w: p.w, h: p.h };
  for (const e of edges) if (lay.edges[e.id]) routes[e.id] = [...lay.edges[e.id].points].reverse();
  return { rects, routes, width: lay.width, height: lay.height };
}

// ---- Tầng instance có focus: bản đồ 2D chỉ vẽ object trong focus ----
export const INSTANCE_LIMIT = 8; // quá số này (trong một nhóm) thì chỉ giữ instance đã chạm + khối "+N chưa chạm"
export const ITEM_ROWS = 6;      // số dòng key/value tối đa trong một khối, dư thì thêm dòng "+n"

// Sắp phần tử thoả pred lên trước, giữ nguyên thứ tự tương đối ở cả hai nửa (stable partition); pred null → giữ nguyên
export function touchedFirst(list, pred) {
  if (!pred) return [...list];
  const yes = [], no = [];
  for (const x of list) (pred(x) ? yes : no).push(x);
  return [...yes, ...no];
}

// Dòng hiện trong khối: [{ key, value }] cắt còn ITEM_ROWS, phần dư thành { more: n }
export function itemRows(items, max = ITEM_ROWS) {
  const rows = (items ?? []).slice(0, max).map(it => ({ key: String(it.label ?? it.key), value: String(it.value) }));
  return items?.length > max ? [...rows, { more: items.length - max }] : rows;
}

// Khối instance của focus { ctx, agg }: object trong focus xếp nhóm theo aggregate (thứ tự model.classes) rồi theo thứ tự node.
// touched: Set id node đã chạm trong lượt chạy; nhóm có hơn `limit` instance thì chỉ giữ node đã chạm + một khối "+N chưa chạm".
// endpoints: các node là đầu mút của flow đã phát; đầu mút nằm ngoài focus sinh cổng (port:<ctx>, và port:agg:<key> khi focus aggregate).
// Trả { blocks, ports, hiddenOf }: hiddenOf = Map id node bị gộp → id khối "+N" của nhóm
export function buildInstanceBlocks(model, world, state, touched = new Set(), { limit = INSTANCE_LIMIT, endpoints = [] } = {}) {
  const blocks = [], ports = [], hiddenOf = new Map();
  if (!model || !state?.ctx) return { blocks, ports, hiddenOf };
  const groups = new Map(); // khoá aggregate → node, theo thứ tự model.classes
  for (const c of model.classes ?? []) {
    if (c.context !== state.ctx || c.hidden) continue;
    const key = aggregateKey(model, c.cls);
    if ((!state.agg || key === state.agg) && !groups.has(key)) groups.set(key, []);
  }
  for (const n of world?.nodes ?? []) {
    const key = focusKey(state, model, n);
    if (key != null) groups.get(key)?.push(n);
  }
  for (const [group, nodes] of groups) {
    const over = nodes.length > limit;
    const shown = over ? nodes.filter(n => touched.has(n.id)) : nodes;
    for (const n of shown) blocks.push({ id: n.id, title: n.title ?? n.cls ?? n.id, sub: n.sub ?? '', group, items: n.items ?? [] });
    const rest = nodes.length - shown.length;
    if (rest > 0) {
      const id = `more:${group}`;
      blocks.push({ id, title: `+${rest} chưa chạm`, sub: group, group, items: null, summary: true });
      for (const n of nodes) if (!touched.has(n.id)) hiddenOf.set(n.id, id);
    }
  }
  const wanted = new Set();
  for (const n of endpoints) {
    if (!n || focusKey(state, model, n) != null) continue;
    const p = focusPort(state, model, n);
    if (p) wanted.add(p);
  }
  for (const c of model.contexts ?? []) {
    if (!wanted.has(`port:${c.id}`)) continue;
    ports.push({ id: `port:${c.id}`, ctx: c.id, title: c.title ?? c.id, sub: 'cổng', port: true, color: contextColor(model, c.id) });
  }
  const seenAgg = new Set();
  for (const c of model.classes ?? []) {
    const key = aggregateKey(model, c.cls), id = `port:agg:${key}`;
    if (c.context !== state.ctx || seenAgg.has(id) || !wanted.has(id)) continue;
    seenAgg.add(id);
    ports.push({ id, ctx: state.ctx, agg: key, title: key, sub: 'cổng · aggregate', port: true, color: contextColor(model, state.ctx) });
  }
  return { blocks, ports, hiddenOf };
}

// Chiều cao khối theo số dòng item: tiêu đề + sub + dòng; khối "+N" thấp hơn
const blockHeight = b => 58 + (b.items ? itemRows(b.items).length * 14 : 0);

// Lưới cho tầng instance: mỗi nhóm một dòng nhãn rồi các khối xếp `cols` cột (chiều cao hàng = khối cao nhất); cổng xếp dọc ở cột phải.
// Trả { rects, labels: [{ group, x, y }], width, height }
export function instanceLayout(blocks, { cols = 3, w = 200, gap = 20, margin = 24, labelH = 26, portW = 170, portH = 56 } = {}) {
  const main = blocks.filter(b => !b.port), ports = blocks.filter(b => b.port);
  const rects = {}, labels = [];
  let y = margin, usedCols = 0;
  for (const group of new Set(main.map(b => b.group))) {
    labels.push({ group, x: margin, y: y + 14 });
    y += labelH;
    const list = main.filter(b => b.group === group);
    for (let i = 0; i < list.length; i += cols) {
      const row = list.slice(i, i + cols), rh = Math.max(...row.map(blockHeight));
      row.forEach((b, j) => { rects[b.id] = { x: margin + j * (w + gap), y, w, h: rh }; });
      y += rh + gap;
      usedCols = Math.max(usedCols, row.length);
    }
  }
  const mainW = usedCols ? usedCols * w + (usedCols - 1) * gap : 0;
  const portX = margin + mainW + (mainW ? gap * 2 : 0);
  ports.forEach((b, i) => { rects[b.id] = { x: portX, y: margin + i * (portH + gap), w: portW, h: portH }; });
  const mainH = main.length ? y - gap - margin : 0, portsH = ports.length ? ports.length * (portH + gap) - gap : 0;
  return { rects, labels, width: ports.length ? portX + portW + margin : margin * 2 + mainW, height: margin * 2 + Math.max(mainH, portsH) };
}
