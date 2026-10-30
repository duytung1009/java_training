// Xem theo tầng: context map → aggregate → instance. Hàm thuần (không đụng DOM) để test bằng node --test.
// Trạng thái tầng: { level, ctx, agg }; khối (block) là đơn vị vẽ ở mỗi tầng, id khối có tiền tố:
//   ctx:<context> · agg:<aggregate hoặc class> · port:<context khác> · (tầng instance) chính node id.
import { classOfNode, contextOfNode, aggregateOf, lookup } from '../model/draft.js';

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
  const known = id => model.contexts.some(c => c.id === id);
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
    const root = prefixes.find(([pre]) => String(id).startsWith(pre))?.[1];
    return root != null ? byId.get(root) : undefined;
  };
}

// Khối chứa node ở tầng hiện tại; null nếu node không có khối ở tầng này
export function blockOf(state, model, node) {
  if (!node) return null;
  if (state.level === 'instance') return node.id;
  if (!model) return null;
  const ctx = contextOfNode(model, node);
  if (ctx == null) return null;
  if (state.level === 'context') return `ctx:${ctx}`;
  if (ctx !== state.ctx) return `port:${ctx}`;
  if (node.kind === 'area') return null;
  const cls = classOfNode(node);
  if (cls == null) return null;
  return `agg:${aggregateOf(model, cls) ?? cls}`;
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

const isObject = n => n.kind === 'object';
export const pairKey = (a, b) => (a < b ? `${a}|${b}` : `${b}|${a}`);

// Khối context (theo thứ tự model.contexts, bỏ context không có class) + cạnh = quan hệ gộp theo cặp context
export function buildContextBlocks(model, world) {
  const has = new Set((model?.classes ?? []).map(c => c.context).filter(Boolean));
  const count = new Map();
  for (const n of world?.nodes ?? []) {
    if (!isObject(n)) continue;
    const c = contextOfNode(model, n);
    if (c != null) count.set(c, (count.get(c) ?? 0) + 1);
  }
  const blocks = (model?.contexts ?? []).filter(c => has.has(c.id)).map(c => ({
    id: `ctx:${c.id}`, ctx: c.id, title: c.title ?? c.id, count: count.get(c.id) ?? 0, color: contextColor(model, c.id),
  }));
  const shown = new Set(blocks.map(b => b.ctx));
  const pairs = new Map();
  for (const r of model?.relations ?? []) {
    const a = lookup(model, r.from)?.context, b = lookup(model, r.to)?.context;
    if (!a || !b || a === b || !shown.has(a) || !shown.has(b)) continue;
    const k = pairKey(a, b), e = pairs.get(k);
    if (e) e.relations++;
    else pairs.set(k, { a: `ctx:${[a, b].sort()[0]}`, b: `ctx:${[a, b].sort()[1]}`, relations: 1 });
  }
  return { blocks, edges: [...pairs.values()] };
}

// Khối aggregate trong context ctx (theo thứ tự model.classes, bỏ class ẩn) + cổng sang các context khác có quan hệ
export function buildAggregateBlocks(model, world, ctx) {
  const state = { level: 'aggregate', ctx, agg: null };
  const blocks = [], seen = new Set();
  for (const c of model?.classes ?? []) {
    if (c.context !== ctx || c.hidden) continue;
    const key = aggregateKey(model, c.cls);
    if (seen.has(key)) continue;
    seen.add(key);
    const def = lookup(model, key);
    blocks.push({ id: `agg:${key}`, ctx, title: key, role: def?.role ?? c.role, count: 0, color: contextColor(model, ctx) });
  }
  const byId = new Map(blocks.map(b => [b.id, b]));
  for (const n of world?.nodes ?? []) {
    if (!isObject(n)) continue;
    const b = byId.get(blockOf(state, model, n));
    if (b) b.count++;
  }
  const other = new Set();
  for (const r of model?.relations ?? []) {
    const a = lookup(model, r.from)?.context, b = lookup(model, r.to)?.context;
    if (a === ctx && b && b !== ctx) other.add(b);
    if (b === ctx && a && a !== ctx) other.add(a);
  }
  const ports = (model?.contexts ?? []).filter(c => other.has(c.id)).map(c => ({
    id: `port:${c.id}`, ctx: c.id, title: c.title ?? c.id, port: true, count: 0, color: contextColor(model, c.id),
  }));
  return { blocks, ports };
}

// Flow ở tầng hiện tại: mũi tên giữa hai khối khác nhau, nháy khi cùng khối, null nếu một đầu không có khối
export function resolveFlowAt(state, model, lookupNode, flow) {
  if (!flow) return null;
  const from = blockOf(state, model, lookupNode(flow.from));
  const to = blockOf(state, model, lookupNode(flow.to));
  if (from == null || to == null) return null;
  return from === to ? { kind: 'flash', block: from } : { kind: 'arrow', from, to };
}

// Đếm flow theo cặp khối (không phân biệt hướng): Map "a|b" → { a, b, calls, events }; chỉ tính mũi tên
export function countFlows(state, model, lookupNode, flows) {
  const out = new Map();
  for (const f of flows) {
    const r = resolveFlowAt(state, model, lookupNode, f);
    if (r?.kind !== 'arrow') continue;
    const k = pairKey(r.from, r.to);
    let e = out.get(k);
    if (!e) { e = { a: r.from < r.to ? r.from : r.to, b: r.from < r.to ? r.to : r.from, calls: 0, events: 0 }; out.set(k, e); }
    if (f.kind === 'EVENT') e.events++; else e.calls++;
  }
  return out;
}

// Lưới đơn giản, xác định: khối thường theo hàng, cổng (port) xếp dọc ở cột riêng bên phải
export function gridLayout(blocks, { cols, w = 190, h = 84, gap = 28, margin = 24 } = {}) {
  const main = blocks.filter(b => !b.port), ports = blocks.filter(b => b.port);
  const c = cols ?? Math.max(1, Math.min(4, Math.ceil(Math.sqrt(main.length))));
  const rects = {};
  main.forEach((b, i) => { rects[b.id] = { x: margin + (i % c) * (w + gap), y: margin + Math.floor(i / c) * (h + gap), w, h }; });
  const usedCols = Math.min(c, main.length), rows = Math.ceil(main.length / c);
  const mainW = usedCols ? usedCols * w + (usedCols - 1) * gap : 0;
  const portX = margin + mainW + (main.length ? gap * 2 : 0);
  ports.forEach((b, i) => { rects[b.id] = { x: portX, y: margin + i * (h + gap), w, h }; });
  const mainH = rows ? rows * h + (rows - 1) * gap : 0, portH = ports.length ? ports.length * h + (ports.length - 1) * gap : 0;
  return { rects, width: ports.length ? portX + w + margin : margin * 2 + mainW, height: margin * 2 + Math.max(mainH, portH) };
}
