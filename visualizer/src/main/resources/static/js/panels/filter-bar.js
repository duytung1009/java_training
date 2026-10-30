// Thanh lọc trên bản đồ: context, aggregate (chip chọn nhiều), kết quả, loại flow, thread, "chỉ object có thay đổi", Xoá lọc.
// Lựa chọn lưu ở localStorage (viz.filters); logic lọc nằm ở view/filters.js.
import { h } from '../dom.js';
import { DEFAULT, normalize, isActive, aggregateOptions } from '../view/filters.js';
import { shortThread } from './log.js';

const KEY = 'viz.filters';

function load() {
  try { return normalize(JSON.parse(localStorage.getItem(KEY))); } catch { return { ...DEFAULT }; }
}
function save(f) {
  try { localStorage.setItem(KEY, JSON.stringify(f)); } catch { /* localStorage bị chặn: bỏ qua, không lưu */ }
}

function select(label, options) {
  const wrap = h('label', 'fb-item');
  const s = h('select');
  for (const [value, text] of options) { const o = h('option', '', text); o.value = value; s.append(o); }
  wrap.append(label + ' ', s);
  return { wrap, select: s };
}

export function createFilterBar({ el, onChange }) {
  let f = load(), model = null, threads = [];

  const ctxGroup = h('span', 'fb-group'), aggGroup = h('span', 'fb-group');
  const outcome = select('Kết quả', [['all', 'tất cả'], ['OK', 'OK'], ['THROWN', 'THROWN']]);
  const kind = select('Flow', [['all', 'tất cả'], ['CALL', 'call'], ['EVENT', 'event']]);
  const thread = select('Thread', [['', 'tất cả']]);
  const changedLabel = h('label', 'fb-item');
  const changed = h('input'); changed.type = 'checkbox';
  changedLabel.append(changed, ' chỉ object có thay đổi');
  const clear = h('button', 'fb-clear', 'Xoá lọc'); clear.type = 'button';
  el.append(ctxGroup, aggGroup, outcome.wrap, kind.wrap, thread.wrap, changedLabel, clear);

  function chips(group, title, items, selected, key) {
    group.textContent = '';
    group.hidden = items.length === 0;
    group.append(h('span', 'fb-label', title));
    for (const { id, text } of items) {
      const lb = h('label', 'fb-chip' + (selected.includes(id) ? ' on' : ''));
      const cb = h('input'); cb.type = 'checkbox'; cb.checked = selected.includes(id);
      cb.addEventListener('change', () => change({ [key]: cb.checked ? [...selected, id] : selected.filter(x => x !== id) }));
      lb.append(cb, text);
      group.append(lb);
    }
  }

  function render() {
    chips(ctxGroup, 'Context', (model?.contexts ?? []).map(c => ({ id: c.id, text: c.title ?? c.id })), f.contexts, 'contexts');
    chips(aggGroup, 'Aggregate', aggregateOptions(model, f.contexts).map(o => ({ id: o.id, text: o.id })), f.aggregates, 'aggregates');
    outcome.select.value = f.outcome;
    kind.select.value = f.flowKind;
    thread.select.textContent = '';
    for (const t of [null, ...threads]) {
      const o = h('option', '', t == null ? 'tất cả' : shortThread(t)); o.value = t ?? ''; o.title = t ?? '';
      thread.select.append(o);
    }
    thread.select.value = f.thread ?? '';
    changed.checked = f.changedOnly;
    clear.disabled = !isActive(f);
  }

  function change(patch) {
    f = normalize({ ...f, ...patch });
    // Đổi context thì bỏ aggregate không còn thuộc context nào được chọn
    const ok = new Set(aggregateOptions(model, f.contexts).map(o => o.id));
    if (model) f.aggregates = f.aggregates.filter(a => ok.has(a));
    save(f); render(); onChange(f);
  }

  outcome.select.addEventListener('change', () => change({ outcome: outcome.select.value }));
  kind.select.addEventListener('change', () => change({ flowKind: kind.select.value }));
  thread.select.addEventListener('change', () => change({ thread: thread.select.value || null }));
  changed.addEventListener('change', () => change({ changedOnly: changed.checked }));
  clear.addEventListener('click', () => change({ ...DEFAULT }));
  render();

  return {
    get: () => f,
    // Mô hình đổi: bỏ context/aggregate không còn tồn tại (không gọi onChange; caller áp lại bộ lọc)
    setModel(m) {
      model = m ?? null;
      if (model) {
        const ctxs = new Set(model.contexts.map(c => c.id));
        f = { ...f, contexts: f.contexts.filter(c => ctxs.has(c)) };
        const ok = new Set(aggregateOptions(model, f.contexts).map(o => o.id));
        f = { ...f, aggregates: f.aggregates.filter(a => ok.has(a)) };
        save(f);
      }
      render();
    },
    // Danh sách thread của run đang nạp; thread đang lọc không còn trong run thì bỏ (không gọi onChange)
    setThreads(list) {
      threads = list;
      if (f.thread != null && !list.includes(f.thread)) { f = { ...f, thread: null }; save(f); }
      render();
    },
  };
}
