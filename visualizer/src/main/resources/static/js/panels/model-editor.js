// Trình sửa mô hình domain (drawer bên phải): dựng context / vai trò / quan hệ từ gợi ý của BE, lưu thành domain-model.json.
// Bản nháp để trong localStorage (bọc try/catch, không có storage thì app vẫn chạy); mọi thay đổi gọi onChange(model).
import { h } from '../dom.js';
import * as api from '../api.js';
import * as D from '../model/draft.js';
import { diffModel, adoptAdded } from '../model/drift.js';

const DRAFT_KEY = 'viz.model.draft';
const UNSET = '';

const store = {
  read() {
    try { const v = JSON.parse(localStorage.getItem(DRAFT_KEY)); return v?.model ? v : null; }
    catch { return null; }
  },
  write(baseOrigin, model) { try { localStorage.setItem(DRAFT_KEY, JSON.stringify({ baseOrigin, model })); } catch { /* không có storage: bỏ qua */ } },
  clear() { try { localStorage.removeItem(DRAFT_KEY); } catch { /* bỏ qua */ } },
};

const same = (a, b) => JSON.stringify(a) === JSON.stringify(b);

function select(options, value, onPick, label) {
  const s = h('select');
  if (label) s.setAttribute('aria-label', label);
  for (const [v, text] of options) {
    const o = h('option', '', text);
    o.value = v;
    s.appendChild(o);
  }
  s.value = value;
  s.addEventListener('change', () => onPick(s.value));
  return s;
}

// el: <aside> của drawer; button: nút mở ở header; onChange(model): gọi mỗi khi mô hình hiện hành đổi
export function createModelEditor({ el, button, onChange = () => {} }) {
  let loaded = null;       // { origin, model } trên BE
  let model = null;        // bản đang sửa (nháp hoặc loaded.model)
  let suggestion = null;   // gợi ý hiện tại, dùng để báo lệch
  let removedRels = [];    // quan hệ đã bỏ, hiện gạch ngang cho tới khi lưu
  let errors = [];         // [{ cls, message }] từ lần lưu lỗi gần nhất
  let top = '';            // thông báo đầu drawer (409, lỗi thêm context…)
  let renaming = null;     // id context đang đổi tên
  let newCtx = { id: '', title: '' };
  let open = false;

  const body = h('div', 'me-body');
  const head = h('div', 'me-top');
  el.append(head, body);

  const dirty = () => !!loaded && !same(loaded.model, model);

  function commit(next) {
    model = next;
    store.write(loaded?.origin ?? null, model);
    top = '';
    onChange(model);
    render();
  }
  // Chạy thao tác draft; thao tác ném lỗi (id trùng…) thì hiện ở đầu drawer thay vì đổi mô hình
  const edit = fn => { try { commit(fn(model)); } catch (err) { top = err.message; render(); } };

  async function load() {
    const res = await api.getModel();
    if (!res) return null;
    loaded = res;
    const draft = store.read();
    model = draft ? draft.model : res.model;
    onChange(model);
    render();
    return model;
  }

  async function openDrawer() {
    open = true; el.hidden = false; render();
    suggestion = await api.getSuggestion();
    render();
  }
  const closeDrawer = () => { open = false; el.hidden = true; };

  async function save() {
    const res = await api.saveModel(model);
    if (!res) return;
    errors = []; top = '';
    if (res.ok) {
      loaded = res.body; model = res.body.model; removedRels = [];
      store.clear();
      onChange(model);
    } else if (res.status === 400) {
      errors = res.body?.errors ?? [];
      if (!errors.length) top = res.body?.detail ?? res.body?.title ?? 'Mô hình không hợp lệ';
    } else top = res.body?.detail ?? res.body?.title ?? `Lưu lỗi (HTTP ${res.status})`;
    render();
  }

  async function discard() {
    store.clear();
    const res = await api.getModel();
    if (res) loaded = res;
    model = loaded.model; removedRels = []; errors = []; top = '';
    onChange(model);
    render();
  }

  function resetToSuggestion() {
    if (!suggestion) return;
    removedRels = []; errors = [];
    commit(structuredClone(suggestion));
  }

  function renderTop() {
    head.textContent = '';
    head.append(h('b', 'me-title', 'MÔ HÌNH DOMAIN'));
    if (loaded) head.append(h('span', 'me-badge', loaded.origin));
    if (dirty()) head.append(h('span', 'me-badge draft', 'nháp'));
    const bar = h('div', 'me-actions');
    const mk = (text, fn, title, primary) => {
      const b = h('button', primary ? 'primary' : '', text);
      b.title = title; b.disabled = !model; b.addEventListener('click', fn);
      return b;
    };
    bar.append(
      mk('Lưu', save, 'Ghi vào domain-model.json', true),
      mk('Bỏ nháp', discard, 'Bỏ bản nháp, nạp lại mô hình đã lưu'),
      mk('Lấy lại gợi ý', resetToSuggestion, 'Thay bản nháp bằng gợi ý hiện tại'),
    );
    const x = h('button', 'me-close', '×');
    x.setAttribute('aria-label', 'Đóng'); x.addEventListener('click', closeDrawer);
    head.append(bar, x);
  }

  function section(title, count) {
    const s = h('section', 'me-sec');
    s.appendChild(h('h3', '', count === undefined ? title : `${title} · ${count}`));
    return s;
  }

  function renderContexts() {
    const s = section('Context', model.contexts.length);
    const count = id => model.classes.filter(c => c.context === id).length;
    for (const c of model.contexts) {
      const row = h('div', 'me-row');
      if (renaming === c.id) {
        const input = h('input', 'me-input'); input.value = c.title;
        const done = ok => {
          if (renaming !== c.id) return;
          renaming = null;
          if (ok && input.value.trim() !== c.title) edit(m => D.renameContext(m, c.id, input.value)); else render();
        };
        input.addEventListener('keydown', e => { if (e.key === 'Enter') done(true); else if (e.key === 'Escape') done(false); });
        input.addEventListener('blur', () => done(true));
        row.appendChild(input);
        queueMicrotask(() => input.focus());
      } else {
        const t = h('button', 'me-link', c.title);
        t.title = `id: ${c.id} · bấm để đổi tên`;
        t.addEventListener('click', () => { renaming = c.id; render(); });
        row.appendChild(t);
      }
      row.append(h('span', 'me-dim', `${count(c.id)} class`));
      const others = model.contexts.filter(o => o.id !== c.id);
      if (others.length) {
        const merge = select([[UNSET, 'Gộp vào…'], ...others.map(o => [o.id, o.title])], UNSET,
          to => { if (to) edit(m => D.mergeContexts(m, c.id, to)); }, `Gộp ${c.title} vào context khác`);
        row.appendChild(merge);
      }
      const del = h('button', 'danger', 'Xoá');
      del.title = 'Class của context này về "chưa xếp"';
      del.addEventListener('click', () => edit(m => D.deleteContext(m, c.id)));
      row.appendChild(del);
      s.appendChild(row);
    }
    const add = h('div', 'me-row');
    const idIn = h('input', 'me-input'); idIn.placeholder = 'id (vd. payment)'; idIn.value = newCtx.id;
    const titleIn = h('input', 'me-input'); titleIn.placeholder = 'Tiêu đề'; titleIn.value = newCtx.title;
    idIn.addEventListener('input', () => { newCtx.id = idIn.value; });
    titleIn.addEventListener('input', () => { newCtx.title = titleIn.value; });
    const go = h('button', '', 'Thêm context');
    const submit = () => edit(m => { const r = D.addContext(m, newCtx.id, newCtx.title); newCtx = { id: '', title: '' }; return r; });
    go.addEventListener('click', submit);
    for (const i of [idIn, titleIn]) i.addEventListener('keydown', e => { if (e.key === 'Enter') submit(); });
    add.append(idIn, titleIn, go);
    s.appendChild(add);
    return s;
  }

  function classRow(c) {
    const row = h('div', 'me-class' + (c.hidden ? ' hidden-cls' : ''));
    const line = h('div', 'me-row');
    const name = h('span', 'me-cls', c.cls);
    name.title = c.cls; // tên dài bị cắt "…" trong lưới cột
    line.append(name, h('span', 'me-chip', c.source ?? '—'));
    const ctx = select([[UNSET, '— chưa xếp —'], ...model.contexts.map(x => [x.id, x.title])], c.context ?? UNSET,
      v => edit(m => D.setClassContext(m, c.cls, v === UNSET ? null : v)), `Context của ${c.cls}`);
    const role = select(D.ROLES.map(r => [r, r]), c.role, v => edit(m => D.setRole(m, c.cls, v)), `Vai trò của ${c.cls}`);
    const lab = h('label', 'me-check');
    const cb = h('input'); cb.type = 'checkbox'; cb.checked = c.hidden;
    cb.addEventListener('change', () => edit(m => D.setHidden(m, c.cls, cb.checked)));
    lab.append(cb, 'ẩn');
    line.append(ctx, role, lab);
    row.appendChild(line);
    for (const e of errors.filter(e => e.cls === c.cls)) row.appendChild(h('div', 'me-err', e.message));
    return row;
  }

  function renderClasses() {
    const s = section('Class', model.classes.length);
    // Class có context không khai báo (vd. file sửa tay) cũng vào "Chưa xếp" để dòng và lỗi của nó vẫn hiện
    const known = new Set(model.contexts.map(c => c.id));
    const groups = [...model.contexts.map(c => [c.title, model.classes.filter(x => x.context === c.id)]),
      ['Chưa xếp', model.classes.filter(x => x.context == null || !known.has(x.context))]];
    for (const [title, list] of groups) {
      if (!list.length) continue;
      s.appendChild(h('h4', '', `${title} · ${list.length}`));
      for (const c of list) s.appendChild(classRow(c));
    }
    return s;
  }

  function renderRelations() {
    const s = section('Quan hệ', model.relations.length);
    const row = (r, removed) => {
      const li = h('div', 'me-row' + (removed ? ' removed' : ''));
      li.append(h('span', 'me-rel', `${r.from} → ${r.to}`), h('span', 'me-chip', r.source ?? '—'));
      const b = h('button', removed ? '' : 'danger', removed ? 'Khôi phục' : '×');
      b.title = removed ? 'Thêm lại quan hệ' : 'Bỏ quan hệ';
      b.addEventListener('click', () => {
        if (removed) { removedRels = removedRels.filter(x => x !== r); edit(m => D.restoreRelation(m, r)); }
        else { removedRels.push(r); edit(m => D.removeRelation(m, r.from, r.to)); }
      });
      li.appendChild(b);
      return li;
    };
    for (const r of model.relations) s.appendChild(row(r, false));
    for (const r of removedRels) s.appendChild(row(r, true));
    return s;
  }

  function renderDrift() {
    if (!suggestion) return null;
    const { added, removed } = diffModel(model, suggestion);
    if (!added.length && !removed.length) return null;
    const s = section('Lệch so với domain hiện tại');
    s.classList.add('drift');
    if (added.length) {
      s.appendChild(h('h4', '', `Class mới chưa xếp · ${added.length}`));
      const all = h('button', 'primary', 'Đưa vào context gợi ý');
      all.addEventListener('click', () => edit(m => adoptAdded(m, suggestion, added.map(c => c.cls))));
      s.appendChild(all);
      for (const c of added) {
        const row = h('div', 'me-row');
        row.append(h('span', 'me-cls', c.cls), h('span', 'me-dim', `${c.context ?? 'chưa xếp'} · ${c.role}`));
        const b = h('button', '', 'Thêm');
        b.addEventListener('click', () => edit(m => adoptAdded(m, suggestion, [c.cls])));
        row.appendChild(b);
        s.appendChild(row);
      }
    }
    if (removed.length) {
      s.appendChild(h('h4', '', `Class không còn trong domain · ${removed.length}`));
      for (const cls of removed) {
        const row = h('div', 'me-row');
        row.appendChild(h('span', 'me-cls', cls));
        const b = h('button', 'danger', 'Bỏ');
        b.title = 'Bỏ class và quan hệ của nó khỏi mô hình';
        b.addEventListener('click', () => edit(m => D.removeClass(m, cls)));
        row.appendChild(b);
        s.appendChild(row);
      }
    }
    return s;
  }

  function render() {
    if (!open) return;
    const scroll = body.scrollTop;
    renderTop();
    body.textContent = '';
    if (!model) { body.appendChild(h('div', 'me-dim', 'Đang tải…')); return; }
    // Lỗi không gắn class, hoặc gắn class không có dòng trong panel, hiện ở đầu
    const shown = new Set(model.classes.map(c => c.cls));
    const general = [top, ...errors.filter(e => !e.cls || !shown.has(e.cls))
      .map(e => (e.cls ? `${e.cls}: ` : '') + e.message)].filter(Boolean);
    for (const msg of general) body.appendChild(h('div', 'me-err top', msg));
    body.append(renderContexts(), renderClasses(), renderRelations());
    const drift = renderDrift();
    if (drift) body.appendChild(drift);
    body.scrollTop = scroll;
  }

  el.hidden = true;
  button.addEventListener('click', () => (open ? closeDrawer() : openDrawer()));
  return { load, current: () => model };
}
