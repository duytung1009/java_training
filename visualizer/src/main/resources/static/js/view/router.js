// Bộ định tuyến: giữ vị trí đang xem (tổng quan → package → instance, URL hash `#area=…&node=…`) và bộ lọc, bọc scene isometric + node-map.
// Đứng giữa player và scene: reset(world), sync(world, opts), flow(f, ok, ms, after, event), flash(ids, ok).
// Điều hướng không cần mô hình domain (chạy cả ở chế độ fixture); mô hình chỉ dùng cho làm mờ theo bộ lọc / class ẩn.
import { h } from '../dom.js';
import { classOfNode, lookup } from '../model/draft.js';
import { parseNav, toHash, normalizeNav, nodeLookup } from './nav.js';
import { DEFAULT, isActive, eventMatches, flowMatches, nodeMatches, changedNodeIds } from './filters.js';
import { egoNeighbours, egoLayout, touchOf, captionOf, MORE_ID } from './ego.js';
import { swapViews } from './transition.js';
import { ROOTS, WIRES } from '../map/layout.js';
import { css } from '../map/iso.js';

const OPEN_LABEL = 'Xem bên trong ›';
const OPEN_NODE_LABEL = 'Xem instance ›';
const OVERVIEW_HINT = '· click để xem chú thích, double click một package để vào trong';
const AREA_HINT = '· double click một khối để xem instance, Esc hoặc breadcrumb để quay lại tổng quan';
const NODE_HINT = '· click khối kề để sang instance đó, Esc để quay lại package';
const STALE = '(đã bị xoá khỏi world)';
const dispVal = it => (it.computed && !String(it.label ?? '').startsWith('=') ? `= ${it.value}` : String(it.value));

// els: { title, hint, crumbs }; nodeView: bản đồ ego của tầng instance (createNodeView); isBlocked(): đang có drawer/dialog nên Esc không lùi;
// onMode('map' | 'node'): tầng instance bật/tắt (chú thích đổi theo)
export function createRouter({ scene, nodeView, els, prefixes = [], isBlocked = () => false, onMode = () => {} }) {
  let model = null, filters = { ...DEFAULT };
  let baseWorld = null, prevWorld = null, curWorld = null;
  let changedRun = new Set();   // id node đổi so với đầu lượt chạy: bộ lọc "chỉ object có thay đổi"
  const seen = new Map();       // id đầu mút của flow đã phát → node (ưu tiên giữ lại khi container cắt "+N")
  const overviewTitle = els.title.textContent;
  const nav = s => normalizeNav(s, scene.areas().map(a => a.id));
  let state = nav(parseNav(location.hash));
  let shown, shownNode;         // area / node đang hiện (undefined = chưa vẽ lần nào; null = tổng quan / không có node): đổi thì đổi camera và bỏ chọn
  let nodeUp = false;           // node-map đang hiện (scene bị đẩy ra ngoài màn hình nhưng vẫn sync để đo chữ được)
  let idx = new Map();          // id → node của world hiện tại
  let afterWorld = null;        // world sau của flow đang phát (node mới chưa có trong world hiện tại)
  const played = [];            // flow đã phát (qua bộ lọc), đã quy về id node: [{ from, to, kind }] cho hàng xóm của instance
  let lastFocus = null;         // node instance thấy lần cuối (còn hiện khi nó biến mất khỏi world)
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

  // ---- Instance: mô hình ego -> node-map ----
  const focusNode = () => idx.get(state.node) ?? (lastFocus?.id === state.node ? lastFocus : null);
  const colorOf = id => css(ROOTS[scene.areaOf(id)]?.color ?? '--muted');
  function egoNodes() {
    const m = new Map();
    for (const w of [prevWorld, curWorld, afterWorld]) for (const n of w?.nodes ?? []) m.set(n.id, n);
    if (state.node && !m.has(state.node) && lastFocus?.id === state.node) m.set(state.node, lastFocus);
    return m;
  }

  function renderNode() {
    const id = state.node, live = idx.get(id);
    if (live) lastFocus = live;
    const fn = focusNode();
    if (!fn) return nodeView.clear();
    const nodes = egoNodes(), items = fn.items ?? [];
    const nbs = egoNeighbours({ focusId: id, nodes, flows: played, wires: WIRES });
    const layout = egoLayout({ id, rows: items.length }, nbs);
    const sub = [fn.sub, !live && STALE].filter(Boolean).join(' ');
    const cards = [{
      id, focus: true, title: fn.title ?? fn.cls ?? id, sub, color: colorOf(id), tip: sub ? `${fn.title}\n${sub}` : fn.title,
      items: items.map(it => ({ k: it.computed ? (it.label ?? it.key) : it.key, v: dispVal(it) })),
      node: { ...fn, children: [...nodes.values()].filter(c => c.parent === id) },
    }];
    for (const n of nbs) if (layout.rects[n.id]) cards.push({ id: n.id, title: n.title, sub: n.sub, color: colorOf(n.id), caption: captionOf(n), dim: dimmed(nodes.get(n.id)) });
    if (layout.more) cards.push({ id: MORE_ID, more: true, title: `+${layout.more}`, sub: 'khác', color: css('--muted') });
    nodeView.render({ cards, layout });
  }

  // ---- Tiêu đề + breadcrumb ----
  function renderChrome() {
    const a = state.area, n = state.node, title = a ? areaTitle(a) : null, nt = n ? (focusNode()?.title ?? n) : null, key = String([title, nt]);
    els.title.textContent = n ? `INSTANCE · ${nt}` : a ? `PACKAGE · ${title}` : overviewTitle;
    els.hint.textContent = n ? NODE_HINT : a ? AREA_HINT : OVERVIEW_HINT;
    els.hint.title = els.hint.textContent; // dòng gợi ý bị cắt bằng "…" khi hẹp: bản đầy đủ nằm ở tooltip
    if (key === chromeKey) return; // breadcrumb giữ nguyên nút (không mất focus bàn phím)
    chromeKey = key;
    els.crumbs.textContent = '';
    if (!a) return;
    const crumb = (text, to) => {
      const b = h('button', 'crumb', text);
      b.type = 'button';
      b.addEventListener('click', () => go(to));
      return b;
    };
    els.crumbs.append(crumb('Tổng quan', { area: null }), ' › ', ...(n ? [crumb(title, { area: a }), ` › ${nt}`] : [title]));
  }

  // Cảnh nào đang hiện đổi (package ↔ instance): cross-fade + khối ma bay từ khung nguồn sang khung đích; không animate thì đổi ngay
  function swapScene(toNode, { animate, areaSame, prevNode }) {
    const sceneSvg = scene.svg, nodeSvg = nodeView.svg;
    if (!animate) {
      sceneSvg.classList.toggle('lv-off', toNode); nodeSvg.classList.toggle('lv-off', !toNode);
      return;
    }
    const id = toNode ? state.node : prevNode, color = colorOf(id);
    if (toNode) {
      const from = areaSame ? scene.blockRect(id) : null;
      sceneSvg.parentElement.scrollTo(0, 0);
      swapViews({ out: sceneSvg, inn: nodeSvg, from, to: () => nodeView.cardRect(id), color });
    } else {
      swapViews({ out: nodeSvg, inn: sceneSvg, from: nodeView.cardRect(id), to: () => (areaSame ? scene.blockRect(id) : null), color });
    }
  }

  function refresh({ animate = true } = {}) {
    const first = shown === undefined, areaChanged = state.area !== shown, nodeChanged = state.node !== shownNode;
    const want = !!state.node, was = nodeUp, prevNode = shownNode;
    if ((areaChanged || nodeChanged) && !first) { scene.select(null); nodeView.select(null); } // bảng chú thích thuộc về tầng vừa rời đi
    if (areaChanged) { shown = state.area; scene.focusArea(state.area, { animate: animate && !want }); }
    shownNode = state.node;
    if (want) renderNode();
    if (want !== was || first) {
      nodeUp = want;
      swapScene(want, { animate: animate && !first, areaSame: !areaChanged, prevNode });
      onMode(want ? 'node' : 'map');
    }
    scene.setDim(dimmed);
    renderChrome();
  }

  // ---- Điều hướng + URL hash ----
  function go(next) {
    const prev = state;
    state = nav(next);
    if (state.node !== prev.node) lastFocus = null;
    const hash = toHash(state);
    if (location.hash !== hash) location.hash = hash; // hashchange sau đó thấy cùng trạng thái nên không làm lại
    refresh();
  }

  const canOpenNode = id => !!state.area && id !== state.area && idx.has(id) && scene.areaOf(id) === state.area;

  // Đi sâu vào khối `id`: tổng quan → package chứa nó; package → instance của khối đó
  function open(id) {
    if (!state.area) {
      const a = scene.areaOf(id);
      if (a) go({ area: a });
    } else if (!state.node && canOpenNode(id)) go({ area: state.area, node: id });
  }
  const openLabel = id => (!state.area ? (scene.areaOf(id) ? OPEN_LABEL : null) : !state.node && canOpenNode(id) ? OPEN_NODE_LABEL : null);

  const goArea = id => go({ area: id }); // từ cổng: sang area bên kia

  // Từ thẻ hàng xóm: sang instance đó (có thể thuộc package khác); thẻ package thì mở package
  function goNode(id) {
    if (ROOTS[id]) return go({ area: id });
    go({ area: scene.areaOf(id) ?? state.area, node: id });
  }

  function up() {
    if (state.node) go({ area: state.area });
    else if (state.area) go({ area: null });
  }

  window.addEventListener('hashchange', () => {
    const next = nav(parseNav(location.hash));
    if (next.area !== state.area || next.node !== state.node) {
      if (next.node !== state.node) lastFocus = null;
      state = next; refresh();
    }
  });
  document.addEventListener('keydown', e => {
    if (e.key !== 'Escape' || e.defaultPrevented) return;
    if (e.target instanceof Element && e.target.closest('input, select, textarea, [contenteditable]')) return;
    if (isBlocked()) return;
    up();
  });

  // ---- Giao diện player đang dùng ----
  const index = world => new Map((world?.nodes ?? []).map(n => [n.id, n]));

  function reset(world) {
    baseWorld = prevWorld = curWorld = world ?? null;
    idx = index(world); afterWorld = null; played.length = 0;
    changedRun = new Set(); seen.clear();
    scene.reset(world);
    if (state.node) renderNode();
  }

  function sync(world, opts) {
    prevWorld = curWorld; curWorld = world ?? null;
    idx = index(world); afterWorld = null;
    changedRun = changedNodeIds(baseWorld, curWorld);
    scene.sync(world, opts); // áp lại làm mờ với tập thay đổi mới
    if (state.node) {
      // Deep link tới node chưa từng thấy trong world nào: sau lần sync đầu thì quay về package
      if (!idx.has(state.node) && lastFocus?.id !== state.node) return go({ area: state.area });
      renderNode();
    }
    if (state.area) renderChrome(); // tiêu đề area có thể đổi theo node
  }

  async function flow(f, ok, ms, after, event) {
    const fl = effFilters();
    const lk = nodeLookup(prevWorld, curWorld, after, prefixes);
    // Bị lọc: không vẽ, không chờ (phát nhanh)
    if (isActive(fl) && ((event && !eventMatches(fl, event, model, lk)) || !flowMatches(fl, f, model, lk))) return;
    for (const id of [f.from, f.to]) { const n = lk(id); if (n) seen.set(id, n); }
    const a = lk(f.from)?.id, b = lk(f.to)?.id;
    if (a != null && b != null) played.push({ from: a, to: b, kind: f.kind });
    const wait = scene.flow(f, ok, ms, after); // scene luôn đếm flow (cổng); ở tầng instance không chờ nó
    if (!state.node) return wait;
    afterWorld = after ?? null;
    const t = a != null && b != null && touchOf(state.node, egoNodes(), a, b);
    if (!t || !egoNodes().has(t.other)) return; // ngoài cây con của instance: không vẽ, không chờ
    renderNode();
    const out = t.dir === 'out';
    return nodeView.flow({ from: out ? state.node : t.other, to: out ? t.other : state.node, kind: f.kind, label: f.label }, ok, ms);
  }

  const flash = (ids, ok) => { scene.flash(ids, ok); if (nodeUp) nodeView.flash(ids, ok); };

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
    select(id) { if (id == null) { scene.select(null); nodeView.select(null); } }, // đóng bảng chú thích
    setModel(m) { model = m ?? null; refresh(); },
    setFilters(f) { filters = f; refresh(); },
    // Đổi mô hình và bộ lọc cùng lúc, làm mờ lại một lần
    update({ model: m, filters: f }) {
      if (m !== undefined) model = m ?? null;
      if (f !== undefined) filters = f;
      refresh();
    },
    eventMatches: matchesEvent,
    open, openLabel, goArea, goNode,
  };
}
