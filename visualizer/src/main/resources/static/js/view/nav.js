// Điều hướng đi sâu: Tổng quan → Package → Instance. Hàm thuần (không đụng DOM) để test bằng node --test.
// Trạng thái: { area: id | null, node: id | null } (area null = tổng quan; node chỉ có nghĩa khi có area).
import { aggregateOf } from '../model/draft.js';
import { guessByPrefix } from '../map/layout.js';

export const OVERVIEW = Object.freeze({ area: null, node: null });

// "#area=inventory&node=hub" ↔ { area, node }; hash rỗng/lạ (kể cả "#level=…" cũ) → tổng quan; node không có area thì bỏ
export function parseNav(hash) {
  const p = new URLSearchParams(String(hash ?? '').replace(/^#/, ''));
  const area = p.get('area') || null;
  return { area, node: area ? p.get('node') || null : null };
}

// Tổng quan → '' (không để lại "#" thừa trong URL)
export function toHash(state) {
  if (!state?.area) return '';
  return '#' + new URLSearchParams(state.node ? { area: state.area, node: state.node } : { area: state.area }).toString();
}

// Trạng thái hiệu dụng: area lạ (không có trong areaIds) → tổng quan; node giữ nguyên (có tồn tại hay không chỉ biết khi có world)
export function normalizeNav(state, areaIds = []) {
  return areaIds.includes(state?.area) ? { area: state.area, node: state.node || null } : { ...OVERVIEW };
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

// Khoá aggregate của một class: aggregate chứa nó, không có thì chính class (service, value lẻ…)
export const aggregateKey = (model, cls) => aggregateOf(model, cls) ?? cls;

// Sắp phần tử thoả pred lên trước, giữ nguyên thứ tự tương đối ở cả hai nửa (stable partition); pred null → giữ nguyên
export function touchedFirst(list, pred) {
  if (!pred) return [...list];
  const yes = [], no = [];
  for (const x of list) (pred(x) ? yes : no).push(x);
  return [...yes, ...no];
}
