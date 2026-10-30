import * as api from './api.js';
import { h } from './dom.js';
import { createScene } from './map/scene.js';
import { createPlayer } from './player.js';
import { initControls } from './controls.js';
import { renderInfo } from './panels/info.js';
import { renderSync } from './panels/sync.js';
import { initAbout } from './panels/about.js';
import { createModelEditor } from './panels/model-editor.js';
import { createRouter } from './view/router.js';
import { createFilterBar } from './panels/filter-bar.js';
import { createLegend } from './panels/legend.js';
import { createProjectionToggle } from './panels/projection-toggle.js';
import { threadsOf } from './view/filters.js';
import { PREFIXES } from './map/layout.js';
import { adapter } from '../adapter/index.js';

const $ = id => document.getElementById(id);
let setButtons = () => {};
initAbout($('about'), $('about-dlg'));

// Legend của domain (adapter.legend: [{ color, label }]) đứng trước các mục chung trong index.html (bảng nổi trên bản đồ isometric)
for (const { color, label } of [...adapter.legend].reverse()) {
  const i = h('i');
  i.style.borderColor = `var(${color})`;
  const span = h('span');
  span.append(i, label);
  document.querySelector('.lg-iso').prepend(span);
}

let currentModel = null; // bản nháp nếu có, không thì mô hình đã lưu; null ở chế độ fixture
let selection = null;    // { id, node } khối đang chọn, để vẽ lại bảng khi mô hình đổi
let router = null;       // tạo sau scene; onSelect có thể chạy trước đó nên dùng ?.
const showInfo = (id, node) => renderInfo($('info'), id, node, currentModel, { openLabel: id ? router?.openLabel(id) : null });
const onSelect = (id, node) => { selection = id && node ? { id, node } : null; showInfo(id, node); };
const scene = createScene($('map'), { onSelect, onOpen: id => router.open(id), onGo: id => router.goArea(id) });
$('info').addEventListener('click', e => {
  if (e.target.closest('.close')) router.select(null);
  else if (e.target.closest('.open') && selection) router.open(selection.id);
});

createLegend({ el: $('legend'), button: $('legend-toggle') });
const projToggle = createProjectionToggle({ el: $('proj-switch'), onChange: v => scene.setProjection(v) });
scene.setProjection(projToggle.get(), { animate: false });

// Router đứng giữa player và bản đồ: điều hướng tổng quan → package (hash, breadcrumb, Esc) + bộ lọc
router = createRouter({
  scene,
  els: { title: $('map-title'), hint: $('map-hint'), crumbs: $('crumbs') },
  prefixes: PREFIXES,
  isBlocked: () => !$('model-editor').hidden || $('about-dlg').open || filterBar.isOpen(), // Esc đang dành cho drawer / dialog / bảng lọc
});

const player = createPlayer({
  scene: router,
  els: { log: $('log'), diff: $('diff'), status: $('status') },
  onState: s => setButtons(s),
  eventFilter: e => router.eventMatches(e),
});

// Bộ lọc đổi: làm mờ lại bản đồ + log
const filterBar = createFilterBar({ el: $('filter-bar'), button: $('filter-toggle'), onChange: f => { router.setFilters(f); player.refresh(); } });
router.setFilters(filterBar.get());

// Áp mô hình lên bản đồ: class bị ẩn thì làm mờ khối, tầng/bộ lọc vẽ lại, và vẽ lại bảng chú thích
function applyModel(model) {
  currentModel = model;
  filterBar.setModel(model);
  router.update({ model, filters: filterBar.get() });
  player.refresh();
  if (selection) showInfo(selection.id, selection.node);
}
const editor = createModelEditor({ el: $('model-editor'), button: $('model-btn'), onChange: applyModel });

async function loadScenario(id) {
  try {
    const run = await api.run(id);
    filterBar.setThreads(threadsOf(run.events));
    player.load(run);
  } catch (err) {
    $('status').textContent = `Lỗi tải kịch bản: ${err.message}`;
  }
}

async function start() {
  const scenarios = await api.getScenarios();
  $('badge').hidden = !api.isFixture();
  if (api.isFixture()) {
    $('model-btn').disabled = true;
    $('model-btn').title = 'Trình sửa mô hình cần backend (đang chạy chế độ fixture)';
  } else {
    try { await editor.load(); } catch { /* không nạp được mô hình: bản đồ vẫn chạy */ }
  }
  try { renderSync($('sync'), await api.getSync()); } catch { /* banner lỗi không được chặn app */ }
  setButtons = initControls({ player, scenarios, onScenario: loadScenario });
  if (scenarios.length) await loadScenario(scenarios[0].id);
}

start().catch(err => { $('status').textContent = `Lỗi khởi động: ${err.message}`; });
