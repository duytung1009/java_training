// Bộ lọc xem: context, aggregate, kết quả, loại flow, thread, "chỉ object có thay đổi". Hàm thuần (không đụng DOM).
// Trạng thái lọc: { contexts, aggregates, outcome, flowKind, thread, changedOnly }.
// Context/aggregate cần mô hình; không có mô hình (chế độ fixture) thì hai điều kiện này bị bỏ qua.
import { classOfNode, contextOfNode, lookup } from '../model/draft.js';
import { aggregateKey } from './nav.js';

export const DEFAULT = Object.freeze({ contexts: [], aggregates: [], outcome: 'all', flowKind: 'all', thread: null, changedOnly: false });

const OUTCOMES = ['all', 'OK', 'THROWN'], KINDS = ['all', 'CALL', 'EVENT'];

// Đọc từ dữ liệu lưu (có thể thiếu/sai trường) → trạng thái hợp lệ
export function normalize(raw) {
  const r = raw && typeof raw === 'object' ? raw : {};
  const strs = v => (Array.isArray(v) ? v.filter(x => typeof x === 'string') : []);
  return {
    contexts: strs(r.contexts), aggregates: strs(r.aggregates),
    outcome: OUTCOMES.includes(r.outcome) ? r.outcome : 'all',
    flowKind: KINDS.includes(r.flowKind) ? r.flowKind : 'all',
    thread: typeof r.thread === 'string' && r.thread ? r.thread : null,
    changedOnly: r.changedOnly === true,
  };
}

export const isActive = f =>
  f.contexts.length > 0 || f.aggregates.length > 0 || f.outcome !== 'all' || f.flowKind !== 'all' || f.thread != null || f.changedOnly;

// Số chiều lọc đang bật (hiện trên nút "Lọc")
export const activeCount = f =>
  [f.contexts.length > 0, f.aggregates.length > 0, f.outcome !== 'all', f.flowKind !== 'all', f.thread != null, f.changedOnly].filter(Boolean).length;

// Danh sách aggregate chọn được: khoá aggregate của class không ẩn thuộc các context đã chọn (rỗng = mọi context), theo thứ tự
// model.classes. Chỉ giữ khoá có vai trò aggregate: service/value lẻ vẫn có khối riêng ở tầng aggregate nhưng không làm rối bộ lọc
export function aggregateOptions(model, contexts = []) {
  const out = [], seen = new Set();
  for (const c of model?.classes ?? []) {
    if (c.hidden || !c.context || (contexts.length && !contexts.includes(c.context))) continue;
    const key = aggregateKey(model, c.cls);
    if (seen.has(key) || lookup(model, key)?.role !== 'aggregate') continue;
    seen.add(key);
    out.push({ id: key, ctx: c.context });
  }
  return out;
}

const selecting = (f, model) => !!model && (f.contexts.length > 0 || f.aggregates.length > 0);

// Node có nằm trong vùng context/aggregate đã chọn không (area chỉ xét context)
export function inSelection(f, model, node) {
  if (f.contexts.length && !f.contexts.includes(contextOfNode(model, node))) return false;
  if (f.aggregates.length && node.kind !== 'area') {
    const cls = classOfNode(node);
    if (cls == null || !f.aggregates.includes(aggregateKey(model, cls))) return false;
  }
  return true;
}

// Một trong các đầu mút nằm trong vùng chọn; đầu mút không tra ra được thì coi là khớp
function endpointsMatch(f, model, lookupNode, ids) {
  if (!selecting(f, model)) return true;
  const nodes = ids.map(id => lookupNode(id)).filter(Boolean);
  return nodes.length === 0 || nodes.some(n => inSelection(f, model, n));
}

export const flowsOf = e => e?.flows ?? (e?.flow ? [e.flow] : []);

export function eventMatches(f, event, model, lookupNode) {
  if (f.outcome !== 'all' && event.outcome !== f.outcome) return false;
  if (f.thread != null && event.thread !== f.thread) return false;
  return endpointsMatch(f, model, lookupNode, flowsOf(event).flatMap(fl => [fl.from, fl.to]));
}

export function flowMatches(f, flow, model, lookupNode) {
  if (f.flowKind !== 'all' && (flow.kind ?? 'CALL') !== f.flowKind) return false;
  return endpointsMatch(f, model, lookupNode, [flow.from, flow.to]);
}

// Node khớp bộ lọc? Area không xét "chỉ có thay đổi" (là khung chứa)
export function nodeMatches(f, node, model, changedIds) {
  if (selecting(f, model) && !inSelection(f, model, node)) return false;
  if (f.changedOnly && node.kind !== 'area' && !changedIds?.has(node.id)) return false;
  return true;
}

// Id node mới hoặc có items (key/value) khác so với world trước, so theo node id (không dùng ObjectChange.ref: đó là tiêu đề của adapter)
export function changedNodeIds(worldBefore, worldNow) {
  const before = new Map((worldBefore?.nodes ?? []).map(n => [n.id, n]));
  const out = new Set();
  for (const n of worldNow?.nodes ?? []) {
    const o = before.get(n.id);
    if (!o) { out.add(n.id); continue; }
    const a = n.items ?? [], b = o.items ?? [];
    if (a.length !== b.length || a.some((it, i) => it.key !== b[i].key || it.value !== b[i].value)) out.add(n.id);
  }
  return out;
}

export const threadsOf = events => [...new Set((events ?? []).map(e => e.thread))];
