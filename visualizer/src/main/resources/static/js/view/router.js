// Bộ định tuyến: giữ vị trí đang xem (tổng quan → package, URL hash `#area=…`) và bộ lọc, bọc scene isometric.
// Đứng giữa player và scene: reset(world), sync(world, opts), flow(f, ok, ms, after, event), flash(ids, ok).
// Điều hướng không cần mô hình domain (chạy cả ở chế độ fixture); mô hình chỉ dùng cho làm mờ theo bộ lọc / class ẩn.
import { h } from '../dom.js';
import { classOfNode, lookup } from '../model/draft.js';
import { parseNav, toHash, normalizeNav, nodeLookup } from './nav.js';
import { DEFAULT, isActive, eventMatches, flowMatches, nodeMatches, changedNodeIds } from './filters.js';

const OPEN_LABEL = 'Xem bên trong ›';
const OVERVIEW_HINT = '· click để xem chú thích, double click một package để vào trong';
const AREA_HINT = '· Esc hoặc breadcrumb để quay lại tổng quan';

// els: { title, hint, crumbs }; isBlocked(): đang có drawer/dialog nên Esc không lùi
export function createRouter({ scene, els, prefixes = [], isBlocked = () => false }) {
  let model = null, filters = { ...DEFAULT };
  let baseWorld = null, prevWorld = null, curWorld = null;
  let changedRun = new Set();   // id node đổi so với đầu lượt chạy: bộ lọc "chỉ object có thay đổi"
  const seen = new Map();       // id đầu mút của flow đã phát → node (ưu tiên giữ lại khi container cắt "+N")
  const overviewTitle = els.title.textContent;
  const nav = s => normalizeNav(s, scene.areas().map(a => a.id));
  let state = nav(parseNav(location.hash));
  let shown;                    // area scene đang hiện (undefined = chưa vẽ lần nào; null = tổng quan): đổi thì đổi camera và bỏ chọn
  let chromeKey = '';

  // Không có mô hình thì context/aggregate không có nghĩa: bỏ qua hai điều kiện này
  const effFilters = () => (model ? filters : { ...filters, contexts: [], aggregates: [] });
  const areaTitle = id => scene.areas().find(a => a.id === id)?.title ?? id;

  // ---- Làm mờ khối trên bản đồ ----
  function dimmed(node) {
    if (model && lookup(model, classOfNode(node))?.hidden === true) return true;
    const f = effFilters();
    return isActive(f) && !nodeMatches(f, node, model, changedRun);
  }

  // ---- Tiêu đề + breadcrumb ----
  function renderChrome() {
    const a = state.area, title = a ? areaTitle(a) : null, key = String(title);
    els.title.textContent = a ? `PACKAGE · ${title}` : overviewTitle;
    els.hint.textContent = a ? AREA_HINT : OVERVIEW_HINT;
    els.hint.title = els.hint.textContent; // dòng gợi ý bị cắt bằng "…" khi hẹp: bản đầy đủ nằm ở tooltip
    if (key === chromeKey) return; // breadcrumb giữ nguyên nút (không mất focus bàn phím)
    chromeKey = key;
    els.crumbs.textContent = '';
    if (!a) return;
    const b = h('button', 'crumb', 'Tổng quan');
    b.type = 'button';
    b.addEventListener('click', () => go({ area: null }));
    els.crumbs.append(b, ` › ${title}`);
  }

  function refresh({ animate = true } = {}) {
    if (state.area !== shown) {
      const first = shown === undefined;
      shown = state.area;
      if (!first) scene.select(null); // bảng chú thích thuộc về tầng vừa rời đi
      scene.focusArea(state.area, { animate });
    }
    scene.setDim(dimmed);
    renderChrome();
  }

  // ---- Điều hướng + URL hash ----
  function go(next) {
    state = nav(next);
    const hash = toHash(state);
    if (location.hash !== hash) location.hash = hash; // hashchange sau đó thấy cùng trạng thái nên không làm lại
    refresh();
  }

  // Đi sâu vào khối `id`. Tổng quan → package chứa nó; ở tầng package: P4d sẽ mở Instance ở đây
  function open(id) {
    if (!state.area) {
      const a = scene.areaOf(id);
      if (a) go({ area: a });
    }
  }
  const openLabel = id => (!state.area && scene.areaOf(id) ? OPEN_LABEL : null);

  const goArea = id => go({ area: id }); // từ cổng: sang area bên kia

  function up() { if (state.area) go({ area: null }); }

  window.addEventListener('hashchange', () => {
    const next = nav(parseNav(location.hash));
    if (next.area !== state.area) { state = next; refresh(); }
  });
  document.addEventListener('keydown', e => {
    if (e.key !== 'Escape' || e.defaultPrevented) return;
    if (e.target instanceof Element && e.target.closest('input, select, textarea, [contenteditable]')) return;
    if (isBlocked()) return;
    up();
  });

  // ---- Giao diện player đang dùng ----
  function reset(world) {
    baseWorld = prevWorld = curWorld = world ?? null;
    changedRun = new Set(); seen.clear();
    scene.reset(world);
  }

  function sync(world, opts) {
    prevWorld = curWorld; curWorld = world ?? null;
    changedRun = changedNodeIds(baseWorld, curWorld);
    scene.sync(world, opts); // áp lại làm mờ với tập thay đổi mới
    if (state.area) renderChrome(); // tiêu đề area có thể đổi theo node
  }

  async function flow(f, ok, ms, after, event) {
    const fl = effFilters();
    const lk = nodeLookup(prevWorld, curWorld, after, prefixes);
    // Bị lọc: không vẽ, không chờ (phát nhanh)
    if (isActive(fl) && ((event && !eventMatches(fl, event, model, lk)) || !flowMatches(fl, f, model, lk))) return;
    for (const id of [f.from, f.to]) { const n = lk(id); if (n) seen.set(id, n); }
    return scene.flow(f, ok, ms, after);
  }

  const flash = (ids, ok) => scene.flash(ids, ok);

  // Dòng log có khớp bộ lọc không (event không tra được node thì coi là khớp)
  function matchesEvent(e) {
    const fl = effFilters();
    if (!isActive(fl)) return true;
    let lk;
    return eventMatches(fl, e, model, id => (lk ??= nodeLookup(baseWorld, e.worldAfter, e.worldAfter, prefixes))(id));
  }

  scene.setPriority(n => changedRun.has(n.id) || seen.has(n.id));
  refresh({ animate: false });
  return {
    reset, sync, flow, flash,
    select(id) { if (id == null) scene.select(null); }, // đóng bảng chú thích
    setModel(m) { model = m ?? null; refresh(); },
    setFilters(f) { filters = f; refresh(); },
    // Đổi mô hình và bộ lọc cùng lúc, làm mờ lại một lần
    update({ model: m, filters: f }) {
      if (m !== undefined) model = m ?? null;
      if (f !== undefined) filters = f;
      refresh();
    },
    eventMatches: matchesEvent,
    open, openLabel, goArea,
  };
}
