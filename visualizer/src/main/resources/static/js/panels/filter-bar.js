// Bảng lọc nổi trên bản đồ (nút "Lọc" ở tiêu đề mở/đóng): context, aggregate (chip chọn nhiều, mỗi nhóm một hàng cuộn được),
// kết quả, loại flow, thread, "chỉ object có thay đổi", Xoá lọc.
// Lựa chọn lưu ở localStorage (viz.filters), trạng thái mở/đóng ở viz.filters.open; logic lọc nằm ở view/filters.js.
import { h, selectEl } from '../dom.js';
import { readJson, writeJson } from '../storage.js';
import { contextById } from '../model/draft.js';
import { DEFAULT, normalize, isActive, activeCount, aggregateOptions } from '../view/filters.js';
import { shortThread } from './log.js';

const KEY = 'viz.filters', OPEN_KEY = 'viz.filters.open';

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

// el: khung bảng lọc (nổi, nằm trong .map); button: nút "Lọc" ở tiêu đề (hiện số chiều lọc đang bật)
export function createFilterBar({ el, button, onChange }) {
  let f = load(), model = null, threads = [], open = readJson(OPEN_KEY) === true;

  const head = h('div', 'fb-head');
  const closeBtn = h('button', 'fb-close', '×'); closeBtn.type = 'button'; closeBtn.setAttribute('aria-label', 'Đóng bộ lọc');
  head.append(h('span', 'fb-title', 'Bộ lọc'), closeBtn);
  const ctxGroup = h('div', 'fb-group'), aggGroup = h('div', 'fb-group');
  const outcome = select('Kết quả', [['all', 'tất cả'], ['OK', 'OK'], ['THROWN', 'THROWN']]);
  const kind = select('Flow', [['all', 'tất cả'], ['CALL', 'call'], ['EVENT', 'event']]);
  const thread = select('Thread', [['', 'tất cả']]);
  const changedLabel = h('label', 'fb-item');
  const changed = h('input'); changed.type = 'checkbox';
  changedLabel.append(changed, ' chỉ object có thay đổi');
  const clear = h('button', 'fb-clear', 'Xoá lọc'); clear.type = 'button';
  const misc = h('div', 'fb-misc');
  misc.append(outcome.wrap, kind.wrap, thread.wrap, changedLabel, clear);
  el.append(head, ctxGroup, aggGroup, misc);
  const badge = h('span', 'fb-badge'); badge.hidden = true;
  button.append('Lọc ', badge);
  button.setAttribute('aria-controls', el.id);

  // Một nhóm chip: hàng nhãn (+ đếm đã chọn, "chọn hết / bỏ hết") và danh sách chip cuộn được; chip đã chọn xếp trước
  function chips(group, title, items, selected, key) {
    group.textContent = '';
    group.hidden = items.length === 0;
    const top = h('div', 'fb-row');
    const n = items.filter(i => selected.includes(i.id)).length;
    top.append(h('span', 'fb-label', n ? `${title} (đã chọn ${n}/${items.length})` : title));
    const link = (text, ids) => {
      const b = h('button', 'fb-link', text); b.type = 'button';
      b.addEventListener('click', () => change({ [key]: ids }));
      return b;
    };
    top.append(link('chọn hết', items.map(i => i.id)), link('bỏ hết', []));
    const list = h('div', 'fb-chips');
    for (const { id, text } of [...items.filter(i => selected.includes(i.id)), ...items.filter(i => !selected.includes(i.id))]) {
      const lb = h('label', 'fb-chip' + (selected.includes(id) ? ' on' : ''));
      const cb = h('input'); cb.type = 'checkbox'; cb.checked = selected.includes(id);
      cb.addEventListener('change', () => change({ [key]: cb.checked ? [...selected, id] : selected.filter(x => x !== id) }));
      lb.append(cb, text);
      list.append(lb);
    }
    group.append(top, list);
  }

  function renderButton() {
    const n = activeCount(f);
    badge.hidden = n === 0;
    badge.textContent = String(n);
    button.setAttribute('aria-expanded', String(open));
    button.classList.toggle('active', n > 0 && !open); // đang lọc mà bảng đóng: nút sáng lên để biết vì sao bản đồ bị mờ
    button.title = n ? `Bộ lọc: ${n} chiều đang bật` : 'Bộ lọc';
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
    renderButton();
  }

  function change(patch) {
    // Đổi context thì bỏ aggregate không còn thuộc context nào được chọn
    f = prune(normalize({ ...f, ...patch }), model);
    save(f); render(); onChange(f);
  }

  // Bảng neo ngay dưới tiêu đề bản đồ; tiêu đề có thể xuống 2-3 dòng ở màn hẹp nên theo dõi chiều cao của nó
  const header = button.closest('h2');
  const placeUnderHeader = () => { if (header) el.style.top = `${header.offsetHeight}px`; };
  if (header && typeof ResizeObserver === 'function') new ResizeObserver(placeUnderHeader).observe(header);

  function setOpen(v) {
    if (open === v) return;
    open = v;
    el.hidden = !open;
    writeJson(OPEN_KEY, open);
    renderButton();
  }

  outcome.select.addEventListener('change', () => change({ outcome: outcome.select.value }));
  kind.select.addEventListener('change', () => change({ flowKind: kind.select.value }));
  thread.select.addEventListener('change', () => change({ thread: thread.select.value || null }));
  changed.addEventListener('change', () => change({ changedOnly: changed.checked }));
  clear.addEventListener('click', () => change({ ...DEFAULT }));
  closeBtn.addEventListener('click', () => { setOpen(false); button.focus(); });
  button.addEventListener('click', () => setOpen(!open));
  // Esc đóng bảng (router thấy isOpen() thì không lùi tầng); click ra ngoài bảng và nút cũng đóng
  document.addEventListener('keydown', e => { if (e.key === 'Escape' && open && !e.defaultPrevented) { setOpen(false); button.focus(); } });
  document.addEventListener('pointerdown', e => { if (open && e.target instanceof Node && !el.contains(e.target) && !button.contains(e.target)) setOpen(false); });
  placeUnderHeader();
  el.hidden = !open;
  render();

  return {
    get: () => f,
    isOpen: () => open,
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
