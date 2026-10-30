// Bộ định tuyến tầng: giữ trạng thái tầng (context → aggregate → instance) và bộ lọc, bọc scene isometric + level-view
// (bản đồ 2D phẳng: tầng context, aggregate, và tầng instance khi có focus ctx/agg → chỉ vẽ phần thuộc focus).
// sau cùng giao diện player đang dùng: reset(world), sync(world, opts), flow(f, ok, ms, after, event), flash(ids, ok).
// reset/sync luôn chuyển cho scene (đổi tầng về instance là tức thì); flow/flash chỉ đến view đang hiện.
import { h } from '../dom.js';
import { classOfNode, lookup, contextById } from '../model/draft.js';
import {
  parseHash, toHash, normalizeState, nodeLookup, blockOf, buildContextBlocks, buildAggregateBlocks,
  resolveFlowAt, countFlows, layoutGraph, edgeWidth, hasFocus, buildInstanceBlocks, instanceLayout, itemRows, contextColor,
} from './levels.js';
import { DEFAULT, isActive, eventMatches, flowMatches, nodeMatches, changedNodeIds } from './filters.js';

const LEVEL_LABEL = { context: 'Context', aggregate: 'Aggregate', instance: 'Instance' };
const TITLE = { context: 'BẢN ĐỒ CONTEXT', aggregate: 'BẢN ĐỒ AGGREGATE' };
const HINT = { context: '· click một context để xem aggregate', aggregate: '· rê chuột để xem quan hệ, click aggregate để xem instance, click cổng để sang context khác' };
const FOCUS_TITLE = 'BẢN ĐỒ INSTANCE';
const FOCUS_HINT = '· click một khối để xem chú thích, click cổng để sang nơi khác';
const same = (a, b) => a.level === b.level && a.ctx === b.ctx && a.agg === b.agg;

// els: { isoSvg, flatSvg, title, hint, crumbs, switcher }; isBlocked(): đang có drawer/dialog nên Esc không lùi tầng;
// onView(iso): gọi mỗi lần vẽ lại, iso = đang hiện bản đồ isometric (bảng chú thích chỉ có nghĩa ở đó)
// onSelect(id, node): chọn một khối ở bản đồ 2D (cùng đường với onSelect của scene, để hiện bảng chú thích)
// Nhãn cạnh: chỉ hiện khi đã có lời gọi / event chạy qua, bỏ phần bằng 0 cho đỡ rối
function edgeLabel(calls = 0, events = 0) {
  return [calls ? `${calls} lời gọi` : '', events ? `${events} event` : ''].filter(Boolean).join(' · ');
}

export function createRouter({ scene, view, els, prefixes = [], isBlocked = () => false, onSelect = () => {}, onView = () => {} }) {
  let model = null, filters = { ...DEFAULT };
  let state = parseHash(location.hash);
  let baseWorld = null, prevWorld = null, curWorld = null;
  let changed = new Set();      // id node đổi ở lần sync gần nhất: viền "bị chạm" ở tầng context/aggregate
  let changedRun = new Set();   // id node đổi so với đầu lượt chạy: bộ lọc "chỉ object có thay đổi"
  let played = [];              // flow đã phát từ lần reset (để đếm trên cạnh)
  const seen = new Map();       // id đầu mút của flow → node, tra được cả khi world đã đổi
  const instanceTitle = els.title.textContent, instanceHint = els.hint.textContent;
  let mode = 'iso';             // 'iso' | 'ctx' | 'agg:<ctx>' | 'focus:<ctx>/<agg>': đổi mode thì bỏ chọn khối
  let selId = null;             // khối đang chọn ở bản đồ 2D tầng instance
  let inst = null;              // kết quả buildInstanceBlocks lần vẽ gần nhất (để resolve flow về khối "+N")

  const eff = () => normalizeState(state, model);
  // Không có mô hình thì context/aggregate không có nghĩa: bỏ qua hai điều kiện này
  const effFilters = () => (model ? filters : { ...filters, contexts: [], aggregates: [] });
  const lookupNow = () => nodeLookup(prevWorld, curWorld, null, prefixes);

  // ---- Làm mờ khối trên bản đồ isometric ----
  function dimmed(node) {
    if (model && lookup(model, classOfNode(node))?.hidden === true) return true;
    const f = effFilters();
    return isActive(f) && !nodeMatches(f, node, model, changedRun);
  }

  // Node "đã chạm" trong lượt chạy: đổi so với đầu lượt hoặc là đầu mút của flow đã phát (ưu tiên khi cắt "+N")
  const playedIds = () => new Set([...seen].flatMap(([id, n]) => [id, n.id]));
  const touchedIds = () => new Set([...changedRun, ...playedIds()]);

  // ---- Tầng instance có focus: bản đồ 2D thu gọn ----
  function renderFocus(s) {
    const world = curWorld ?? { nodes: [] };
    const f = effFilters(), active = isActive(f);
    inst = buildInstanceBlocks(model, world, s, touchedIds(), { endpoints: seen.values() });
    const blocks = [...inst.blocks, ...inst.ports];
    const matching = new Set();
    for (const n of world.nodes) {
      const b = blockOf(s, model, n, inst);
      if (b && (!active || nodeMatches(f, n, model, changedRun))) matching.add(b);
    }
    if (selId && !blocks.some(b => b.id === selId)) { selId = null; onSelect(null, null); }
    const lay = instanceLayout(blocks), color = contextColor(model, s.ctx);
    view.render({
      blocks: blocks.map(b => ({
        ...b, color: b.color ?? color, rows: b.items ? itemRows(b.items) : undefined,
        dim: active && !matching.has(b.id), touched: changed.has(b.id), selected: b.id === selId,
      })),
      edges: [], labels: lay.labels, rects: lay.rects, width: lay.width, height: lay.height,
      onOpen: b => {
        if (b.port) return go(b.agg ? { level: 'instance', ctx: s.ctx, agg: b.agg } : { level: 'aggregate', ctx: b.ctx, agg: null });
        if (b.summary) return;
        selId = selId === b.id ? null : b.id;
        renderView();
        syncSelection();
      },
    });
  }

  // Đẩy node đang chọn (đã cập nhật theo world hiện tại) ra bảng chú thích
  function syncSelection() {
    if (!selId) return onSelect(null, null);
    const n = lookupNow()(selId);
    onSelect(selId, n ? { ...n, children: [] } : null);
  }

  // ---- Level view (context / aggregate / instance có focus) ----
  function renderView() {
    const s = eff();
    if (!model) return;
    if (hasFocus(s)) return renderFocus(s);
    if (s.level === 'instance') return;
    const world = curWorld ?? { nodes: [] };
    const f = effFilters(), active = isActive(f);
    const matching = new Set(), touched = new Set();
    for (const n of world.nodes) {
      const b = blockOf(s, model, n);
      if (!b) continue;
      if (!active || nodeMatches(f, n, model, changedRun)) matching.add(b);
      if (changed.has(n.id)) touched.add(b);
    }
    const counts = countFlows(s, model, id => seen.get(id), played);
    const built = s.level === 'context' ? buildContextBlocks(model, world, counts) : buildAggregateBlocks(model, world, s.ctx, counts);
    const blocks = [...built.blocks, ...(built.ports ?? [])];
    const lay = layoutGraph(blocks, built.edges);
    // Nhãn trên cạnh: "id" (tham chiếu theo id giữa hai aggregate) và bộ đếm lời gọi/event đã chạy (chỉ hiện khi > 0)
    const edges = built.edges.map(e => ({
      ...e, points: lay.routes[e.id], width: e.style === 'param' ? 1 : edgeWidth(e.relations),
      label: [e.style === 'id' ? 'id' : '', edgeLabel(e.calls, e.events)].filter(Boolean).join(' · '),
    }));
    view.render({
      blocks: blocks.map(b => ({
        ...b, card: true, dim: active && !matching.has(b.id), touched: touched.has(b.id),
      })),
      edges, rects: lay.rects, width: lay.width, height: lay.height,
      onOpen: b => {
        if (s.level === 'context' || b.port) go({ level: 'aggregate', ctx: b.ctx, agg: null });
        else go({ level: 'instance', ctx: s.ctx, agg: b.key ?? b.title });
      },
    });
  }

  // ---- Breadcrumb + nút chuyển tầng ----
  const switchBtns = {};
  for (const lv of Object.keys(LEVEL_LABEL)) {
    const b = h('button', 'lv-btn', LEVEL_LABEL[lv]);
    b.type = 'button';
    b.addEventListener('click', () => {
      const s = eff();
      go(lv === 'instance' ? { level: 'instance', ctx: null, agg: null } : { level: lv, ctx: s.ctx, agg: null });
    });
    switchBtns[lv] = b;
    els.switcher.append(b);
  }

  function renderChrome(s) {
    const focus = hasFocus(s);
    els.title.textContent = focus ? FOCUS_TITLE : TITLE[s.level] ?? instanceTitle;
    els.hint.textContent = focus ? FOCUS_HINT : HINT[s.level] ?? instanceHint;
    els.hint.title = els.hint.textContent; // dòng gợi ý bị cắt bằng "…" khi hẹp: bản đầy đủ nằm ở tooltip
    for (const [lv, b] of Object.entries(switchBtns)) {
      b.disabled = !model;
      b.classList.toggle('on', lv === s.level);
      b.title = model ? '' : 'Cần mô hình domain (không có ở chế độ fixture)';
    }
    els.crumbs.textContent = '';
    if (!model || (s.level === 'instance' && !s.ctx)) return;
    const title = id => contextById(model, id)?.title ?? id;
    const parts = [['Context map', { level: 'context', ctx: null, agg: null }]];
    if (s.ctx) parts.push([title(s.ctx), { level: 'aggregate', ctx: s.ctx, agg: null }]);
    if (s.agg) parts.push([s.agg, { level: 'instance', ctx: s.ctx, agg: s.agg }]);
    parts.forEach(([text, target], i) => {
      if (i) els.crumbs.append(' › ');
      const b = h('button', 'crumb', text);
      b.type = 'button';
      b.addEventListener('click', () => go(target));
      els.crumbs.append(b);
    });
  }

  function clearSelection() { selId = null; scene.select(null); }

  function refresh() {
    const s = eff(), iso = s.level === 'instance' && !hasFocus(s);
    // Ẩn bằng class (đẩy ra ngoài màn hình), không display:none: scene vẫn đo được độ dài chữ khi sync lúc đang ẩn
    els.isoSvg.classList.toggle('lv-off', !iso);
    els.flatSvg.toggleAttribute('hidden', iso); // SVGElement không có property hidden như HTMLElement
    const next = iso ? 'iso' : hasFocus(s) ? `focus:${s.ctx}/${s.agg ?? ''}` : s.level === 'context' ? 'ctx' : `agg:${s.ctx}`;
    if (next !== mode) { mode = next; clearSelection(); } // bảng chú thích thuộc về bản đồ vừa rời đi
    if (!hasFocus(s)) inst = null;
    onView(iso);
    scene.setDim(dimmed);
    renderChrome(s);
    renderView();
  }

  // ---- Điều hướng + URL hash ----
  function go(next) {
    state = normalizeState({ level: next.level, ctx: next.ctx ?? null, agg: next.agg ?? null }, model);
    const hash = toHash(state);
    if (location.hash !== hash) location.hash = hash; // hashchange sau đó thấy cùng trạng thái nên không làm lại
    refresh();
  }

  function up() {
    const s = eff();
    if (s.level === 'instance' && s.ctx) go({ level: 'aggregate', ctx: s.ctx });
    else if (s.level === 'aggregate') go({ level: 'context' });
  }

  window.addEventListener('hashchange', () => {
    const next = parseHash(location.hash);
    if (!same(next, state)) { state = next; refresh(); }
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
    changed = new Set(); changedRun = new Set(); played = []; seen.clear();
    scene.reset(world);
    view.clearFlows();
    renderView();
  }

  function sync(world, opts) {
    prevWorld = curWorld; curWorld = world ?? null;
    changed = changedNodeIds(prevWorld, curWorld);
    changedRun = changedNodeIds(baseWorld, curWorld);
    scene.sync(world, opts); // áp lại làm mờ với tập thay đổi mới
    renderView();
    if (selId) syncSelection();
  }

  async function flow(f, ok, ms, after, event) {
    const s = eff(), fl = effFilters();
    const lk = nodeLookup(prevWorld, curWorld, after, prefixes);
    // Bị lọc: không vẽ, không chờ (phát nhanh)
    if (isActive(fl) && ((event && !eventMatches(fl, event, model, lk)) || !flowMatches(fl, f, model, lk))) return;
    for (const id of [f.from, f.to]) { const n = lk(id); if (n) seen.set(id, n); }
    played.push(f);
    if (s.level === 'instance' && !hasFocus(s)) return scene.flow(f, ok, ms, after);
    // Tầng instance có focus: vẽ lại trước để đầu mút vừa chạm có khối (không bị gộp vào "+N") và cổng của nó
    if (hasFocus(s)) renderView();
    const r = resolveFlowAt(s, model, lk, f, inst);
    if (!r) return;
    if (r.kind === 'flash') { view.flash([r.block], ok); return; }
    if (!hasFocus(s)) renderView(); // bộ đếm trên cạnh tăng ngay khi mũi tên bắt đầu
    return view.flow(r, f.kind, ok, ms, f.label);
  }

  function flash(ids, ok) {
    const s = eff();
    if (s.level === 'instance' && !hasFocus(s)) return scene.flash(ids, ok);
    const lk = lookupNow();
    view.flash([...new Set((ids ?? []).map(id => blockOf(s, model, lk(id), inst)).filter(Boolean))], ok);
  }

  // Dòng log có khớp bộ lọc không (event không tra được node thì coi là khớp)
  function matchesEvent(e) {
    const fl = effFilters();
    if (!isActive(fl)) return true;
    let lk;
    return eventMatches(fl, e, model, id => (lk ??= nodeLookup(baseWorld, e.worldAfter, e.worldAfter, prefixes))(id));
  }

  scene.setPriority(n => changedRun.has(n.id) || seen.has(n.id));
  refresh();
  return {
    reset, sync, flow, flash,
    select(id) { if (id == null) { clearSelection(); renderView(); } }, // đóng bảng chú thích: bỏ chọn ở cả hai bản đồ
    setModel(m) { model = m ?? null; refresh(); },
    setFilters(f) { filters = f; refresh(); },
    // Đổi mô hình và bộ lọc cùng lúc, vẽ lại một lần
    update({ model: m, filters: f }) {
      if (m !== undefined) model = m ?? null;
      if (f !== undefined) filters = f;
      refresh();
    },
    eventMatches: matchesEvent,
  };
}
