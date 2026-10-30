// Thao tác bất biến trên mô hình domain (DomainModel: { version, contexts, classes, relations }).
// Hàm thuần: không đụng DOM/localStorage; mỗi thao tác trả mô hình mới, không sửa mô hình truyền vào.
// classOfNode / contextOfNode / aggregateOf phản chiếu ModelAnnotator + NodeClasses phía Java.

export const ROLES = ['aggregate', 'entity', 'value', 'service', 'other'];

// Sửa một class: đặt source = manual. Class chưa có trong mô hình thì ném lỗi
function editClass(m, cls, patch) {
  if (!lookup(m, cls)) throw new Error(`Class không có trong mô hình: ${cls}`);
  return { ...m, classes: m.classes.map(c => c.cls === cls ? { ...c, ...patch, source: 'manual' } : c) };
}

const hasContext = (m, id) => m.contexts.some(c => c.id === id);

// Chỉ mục theo mô hình (memo bằng WeakMap): thao tác draft luôn trả object mới nên không bao giờ lỗi thời
const indexes = new WeakMap();
function indexOf(m) {
  let ix = indexes.get(m);
  if (!ix) {
    const byCls = new Map();
    for (const c of m.classes ?? []) if (!byCls.has(c.cls)) byCls.set(c.cls, c); // trùng cls: lấy cái đầu, như find
    const byCtx = new Map();
    for (const c of m.contexts ?? []) if (!byCtx.has(c.id)) byCtx.set(c.id, c);
    indexes.set(m, ix = { byCls, byCtx, agg: new Map() });
  }
  return ix;
}

export const lookup = (m, cls) => (m && typeof m === 'object' ? indexOf(m).byCls.get(cls) ?? null : null);

export const contextById = (m, id) => (m && typeof m === 'object' ? indexOf(m).byCtx.get(id) ?? null : null);

// Tên class của node: object → cls; collection List<X> → X; area và node không có cls → null
export function classOfNode(node) {
  if (!node || node.cls == null) return null;
  if (node.kind === 'object') return node.cls;
  if (node.kind === 'collection') {
    const open = node.cls.indexOf('<'), close = node.cls.lastIndexOf('>');
    return open >= 0 && close > open ? node.cls.substring(open + 1, close).trim() : null;
  }
  return null;
}

// Context của node: area lấy context có id trùng cls hoặc cls kết thúc bằng "." + id; còn lại theo class của node
export function contextOfNode(m, node) {
  if (!m || !node) return null;
  if (node.kind === 'area') {
    if (node.cls == null) return null;
    return m.contexts.find(c => node.cls === c.id || node.cls.endsWith('.' + c.id))?.id ?? null;
  }
  return lookup(m, classOfNode(node))?.context ?? null;
}

// Aggregate chứa class: aggregate là chính nó; entity/value thuộc aggregate duy nhất cùng context sở hữu nó qua field
export function aggregateOf(m, cls) {
  const def = lookup(m, cls);
  if (!def) return null;
  const memo = indexOf(m).agg;
  if (!memo.has(cls)) memo.set(cls, computeAggregateOf(m, def, cls));
  return memo.get(cls);
}

function computeAggregateOf(m, def, cls) {
  if (def.role === 'aggregate') return def.cls;
  if (def.role !== 'entity' && def.role !== 'value') return null;
  const owners = [...new Set(m.relations.filter(r => r.source === 'field' && r.to === cls).map(r => r.from))]
    .filter(from => { const o = lookup(m, from); return o && o.role === 'aggregate' && o.context === def.context; });
  return owners.length === 1 ? owners[0] : null;
}

export function setClassContext(m, cls, contextId) {
  const ctx = contextId ?? null;
  if (ctx !== null && !hasContext(m, ctx)) throw new Error(`Context không tồn tại: ${ctx}`);
  return editClass(m, cls, { context: ctx });
}

export function setRole(m, cls, role) {
  if (!ROLES.includes(role)) throw new Error(`Vai trò không hợp lệ: ${role}`);
  return editClass(m, cls, { role });
}

export const setHidden = (m, cls, hidden) => editClass(m, cls, { hidden: !!hidden });

export function addContext(m, id, title) {
  const key = typeof id === 'string' ? id.trim() : '';
  if (!key) throw new Error('Id context không được rỗng');
  if (hasContext(m, key)) throw new Error(`Context đã tồn tại: ${key}`);
  return { ...m, contexts: [...m.contexts, { id: key, title: (title ?? '').trim() || key, color: null }] };
}

// Chỉ đổi tiêu đề; id giữ nguyên
export function renameContext(m, id, newTitle) {
  if (!hasContext(m, id)) throw new Error(`Context không tồn tại: ${id}`);
  const title = (newTitle ?? '').trim();
  if (!title) throw new Error('Tiêu đề context không được rỗng');
  return { ...m, contexts: m.contexts.map(c => c.id === id ? { ...c, title } : c) };
}

// Chuyển class của fromId sang intoId rồi bỏ fromId
export function mergeContexts(m, fromId, intoId) {
  if (fromId === intoId) throw new Error('Không thể gộp context vào chính nó');
  if (!hasContext(m, fromId) || !hasContext(m, intoId)) throw new Error('Context không tồn tại');
  return {
    ...m,
    contexts: m.contexts.filter(c => c.id !== fromId),
    classes: m.classes.map(c => c.context === fromId ? { ...c, context: intoId, source: 'manual' } : c),
  };
}

// Class của context bị xoá về "chưa xếp" (context = null)
export function deleteContext(m, id) {
  if (!hasContext(m, id)) throw new Error(`Context không tồn tại: ${id}`);
  return {
    ...m,
    contexts: m.contexts.filter(c => c.id !== id),
    classes: m.classes.map(c => c.context === id ? { ...c, context: null, source: 'manual' } : c),
  };
}

export function removeRelation(m, from, to) {
  return { ...m, relations: m.relations.filter(r => !(r.from === from && r.to === to)) };
}

// Thêm lại quan hệ đã bỏ; đã có cặp (from, to) thì giữ nguyên
export function restoreRelation(m, rel) {
  if (m.relations.some(r => r.from === rel.from && r.to === rel.to)) return m;
  return { ...m, relations: [...m.relations, { ...rel }] };
}

// Bỏ class (không còn trong domain) kèm mọi quan hệ chạm tới nó
export function removeClass(m, cls) {
  return {
    ...m,
    classes: m.classes.filter(c => c.cls !== cls),
    relations: m.relations.filter(r => r.from !== cls && r.to !== cls),
  };
}
