// Bộ định tuyến tầng: giữ trạng thái tầng (context → aggregate → instance) và bộ lọc, bọc scene isometric + level-view
// sau cùng giao diện player đang dùng: reset(world), sync(world, opts), flow(f, ok, ms, after, event), flash(ids, ok).
// reset/sync luôn chuyển cho scene (đổi tầng về instance là tức thì); flow/flash chỉ đến view đang hiện.
import { h } from '../dom.js';
import { classOfNode, contextOfNode, lookup } from '../model/draft.js';
import {
  parseHash, toHash, normalizeState, nodeLookup, blockOf, aggregateKey, buildContextBlocks, buildAggregateBlocks,
  resolveFlowAt, countFlows, gridLayout, pairKey,
} from './levels.js';
import { DEFAULT, isActive, eventMatches, flowMatches, nodeMatches, changedNodeIds } from './filters.js';

const LEVEL_LABEL = { context: 'Context', aggregate: 'Aggregate', instance: 'Instance' };
const TITLE = { context: 'BẢN ĐỒ CONTEXT', aggregate: 'BẢN ĐỒ AGGREGATE' };
const HINT = { context: '· click một context để xem aggregate', aggregate: '· click một aggregate để xem instance, click cổng để sang context khác' };
const same = (a, b) => a.level === b.level && a.ctx === b.ctx && a.agg === b.agg;

// els: { isoSvg, flatSvg, title, hint, crumbs, switcher }; getBlocked(): đang có drawer/dialog nên Esc không lùi tầng
// Nhãn cạnh: chỉ hiện khi đã có lời gọi / event chạy qua, bỏ phần bằng 0 cho đỡ rối
function edgeLabel(calls = 0, events = 0) {
  return [calls ? `${calls} lời gọi` : '', events ? `${events} event` : ''].filter(Boolean).join(' · ');
}

export function createRouter({ scene, view, els, prefixes = [], isBlocked = () => false }) {
  let model = null, filters = { ...DEFAULT };
  let state = parseHash(location.hash);
  let baseWorld = null, prevWorld = null, curWorld = null;
  let changed = new Set();      // id node đổi ở lần sync gần nhất: viền "bị chạm" ở tầng context/aggregate
  let changedRun = new Set();   // id node đổi so với đầu lượt chạy: bộ lọc "chỉ object có thay đổi"
  let played = [];              // flow đã phát từ lần reset (để đếm trên cạnh)
  const seen = new Map();       // id đầu mút của flow → node, tra được cả khi world đã đổi
  const instanceTitle = els.title.textContent, instanceHint = els.hint.textContent;
  let wasInstance = true;

  const eff = () => normalizeState(state, model);
  // Không có mô hình thì context/aggregate không có nghĩa: bỏ qua hai điều kiện này
  const effFilters = () => (model ? filters : { ...filters, contexts: [], aggregates: [] });
  const lookupNow = () => nodeLookup(prevWorld, curWorld, null, prefixes);

  // ---- Làm mờ khối trên bản đồ isometric ----
  function dimmed(node) {
    if (model && lookup(model, classOfNode(node))?.hidden === true) return true;
    const f = effFilters();
    if (isActive(f) && !nodeMatches(f, node, model, changedRun)) return true;
    const s = eff();
    // Focus: chỉ làm mờ (không gỡ khối) để giữ nguyên bố cục của adapter
    if (s.level === 'instance' && model && s.ctx) {
      if (contextOfNode(model, node) !== s.ctx) return true;
      if (s.agg && node.kind !== 'area') {
        const cls = classOfNode(node);
        if (cls == null || aggregateKey(model, cls) !== s.agg) return true;
      }
    }
    return false;
  }

  // ---- Level view (context / aggregate) ----
  function renderView() {
    const s = eff();
    if (s.level === 'instance' || !model) return;
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
    let blocks, edges, sub;
    if (s.level === 'context') {
      const built = buildContextBlocks(model, world);
      blocks = built.blocks;
      sub = b => `${b.count} instance`;
      const known = new Set(built.edges.map(e => pairKey(e.a, e.b)));
      edges = [...built.edges.map(e => ({ ...e, ...(counts.get(pairKey(e.a, e.b)) ?? {}) })),
        ...[...counts].filter(([k]) => !known.has(k)).map(([, c]) => ({ ...c, relations: 0 }))]
        .filter(e => blocks.some(b => b.id === e.a) && blocks.some(b => b.id === e.b))
        .map(e => ({ a: e.a, b: e.b, label: edgeLabel(e.calls, e.events), tip: `${e.relations} quan hệ trong mô hình` }));
    } else {
      const built = buildAggregateBlocks(model, world, s.ctx);
      blocks = [...built.blocks, ...built.ports];
      sub = b => (b.port ? 'cổng' : `${b.role} · ${b.count} instance`);
      edges = [...counts.values()].map(c => ({ a: c.a, b: c.b, label: edgeLabel(c.calls, c.events) }));
    }
    const grid = gridLayout(blocks);
    view.render({
      blocks: blocks.map(b => ({ ...b, sub: sub(b), dim: active && !matching.has(b.id), touched: touched.has(b.id) })),
      edges, rects: grid.rects, width: grid.width, height: grid.height,
      onOpen: b => {
        if (s.level === 'context') go({ level: 'aggregate', ctx: b.ctx, agg: null });
        else if (b.port) go({ level: 'aggregate', ctx: b.ctx, agg: null });
        else go({ level: 'instance', ctx: s.ctx, agg: b.title });
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
    els.title.textContent = TITLE[s.level] ?? instanceTitle;
    els.hint.textContent = HINT[s.level] ?? instanceHint;
    for (const [lv, b] of Object.entries(switchBtns)) {
      b.disabled = !model;
      b.classList.toggle('on', lv === s.level);
      b.title = model ? '' : 'Cần mô hình domain (không có ở chế độ fixture)';
    }
    els.crumbs.textContent = '';
    if (!model || (s.level === 'instance' && !s.ctx)) return;
    const title = id => model.contexts.find(c => c.id === id)?.title ?? id;
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

  function refresh() {
    const s = eff(), inst = s.level === 'instance';
    // Ẩn bằng class (đẩy ra ngoài màn hình), không display:none: scene vẫn đo được độ dài chữ khi sync lúc đang ẩn
    els.isoSvg.classList.toggle('lv-off', !inst);
    els.flatSvg.toggleAttribute('hidden', inst); // SVGElement không có property hidden như HTMLElement
    if (!inst && wasInstance) scene.select(null); // bảng chú thích chỉ có ở tầng instance
    wasInstance = inst;
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
  }

  async function flow(f, ok, ms, after, event) {
    const s = eff(), fl = effFilters();
    const lk = nodeLookup(prevWorld, curWorld, after, prefixes);
    // Bị lọc: không vẽ, không chờ (phát nhanh)
    if (isActive(fl) && ((event && !eventMatches(fl, event, model, lk)) || !flowMatches(fl, f, model, lk))) return;
    for (const id of [f.from, f.to]) { const n = lk(id); if (n) seen.set(id, n); }
    played.push(f);
    if (s.level === 'instance') return scene.flow(f, ok, ms, after);
    const r = resolveFlowAt(s, model, lk, f);
    if (!r) return;
    if (r.kind === 'flash') { view.flash([r.block], ok); return; }
    renderView(); // bộ đếm trên cạnh tăng ngay khi mũi tên bắt đầu
    return view.flow(r, f.kind, ok, ms, f.label);
  }

  function flash(ids, ok) {
    const s = eff();
    if (s.level === 'instance') return scene.flash(ids, ok);
    const lk = lookupNow();
    view.flash([...new Set((ids ?? []).map(id => blockOf(s, model, lk(id))).filter(Boolean))], ok);
  }

  // Dòng log có khớp bộ lọc không (event không tra được node thì coi là khớp)
  function matchesEvent(e) {
    const fl = effFilters();
    if (!isActive(fl)) return true;
    let lk;
    return eventMatches(fl, e, model, id => (lk ??= nodeLookup(baseWorld, e.worldAfter, e.worldAfter, prefixes))(id));
  }

  refresh();
  return {
    reset, sync, flow, flash,
    setModel(m) { model = m ?? null; refresh(); },
    setFilters(f) { filters = f; refresh(); },
    eventMatches: matchesEvent,
    go, up, state: eff,
  };
}
