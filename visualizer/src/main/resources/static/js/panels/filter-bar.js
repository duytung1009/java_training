// Thanh lọc trên bản đồ: context, aggregate (chip chọn nhiều), kết quả, loại flow, thread, "chỉ object có thay đổi", Xoá lọc.
// Lựa chọn lưu ở localStorage (viz.filters); logic lọc nằm ở view/filters.js.
import { h, selectEl } from '../dom.js';
import { readJson, writeJson } from '../storage.js';
import { contextById } from '../model/draft.js';
import { DEFAULT, normalize, isActive, aggregateOptions } from '../view/filters.js';
import { shortThread } from './log.js';

const KEY = 'viz.filters';

const load = () => normalize(readJson(KEY));
const save = f => writeJson(KEY, f);

function select(label, options) {
  const wrap = h('label', 'fb-item');
  const s = selectEl(options, options[0][0]);
  wrap.append(label + ' ', s);
  return { wrap, select: s };
}

// Bỏ context/aggregate không còn tồn tại trong mô hình; không đổi gì thì trả lại đúng f (để so sánh bằng tham chiếu)
function prune(f, model) {
  if (!model) return f;
  const contexts = f.contexts.filter(c => contextById(model, c));
  const ok = new Set(aggregateOptions(model, contexts).map(o => o.id));
  const aggregates = f.aggregates.filter(a => ok.has(a));
  return contexts.length === f.contexts.length && aggregates.length === f.aggregates.length ? f : { ...f, contexts, aggregates };
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
    // Đổi context thì bỏ aggregate không còn thuộc context nào được chọn
    f = prune(normalize({ ...f, ...patch }), model);
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
    // Mô hình đổi: bỏ context/aggregate không còn tồn tại; bộ lọc có đổi thì gọi onChange
    setModel(m) {
      model = m ?? null;
      const next = prune(f, model);
      const moved = next !== f;
      if (moved) { f = next; save(f); }
      render();
      if (moved) onChange(f);
    },
    // Danh sách thread của run đang nạp; thread đang lọc không còn trong run thì bỏ (bộ lọc đổi thì gọi onChange)
    setThreads(list) {
      threads = list;
      const moved = f.thread != null && !list.includes(f.thread);
      if (moved) { f = { ...f, thread: null }; save(f); }
      render();
      if (moved) onChange(f);
    },
  };
}
