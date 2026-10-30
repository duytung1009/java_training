import * as api from './api.js';
import { createScene } from './map/scene.js';
import { createPlayer } from './player.js';
import { initControls } from './controls.js';
import { renderInfo } from './panels/info.js';
import { renderSync } from './panels/sync.js';
import { initAbout } from './panels/about.js';
import { createModelEditor } from './panels/model-editor.js';
import { classOfNode, lookup } from './model/draft.js';
import { adapter } from '../adapter/index.js';

const $ = id => document.getElementById(id);
let setButtons = () => {};
initAbout($('about'), $('about-dlg'));

// Legend của domain (adapter.legend: [{ color, label }]) đứng trước các mục chung trong index.html
for (const { color, label } of [...adapter.legend].reverse()) {
  const i = document.createElement('i');
  i.style.borderColor = `var(${color})`;
  const span = document.createElement('span');
  span.append(i, label);
  document.querySelector('.legend').prepend(span);
}

let currentModel = null; // bản nháp nếu có, không thì mô hình đã lưu; null ở chế độ fixture
let selection = null;    // { id, node } khối đang chọn, để vẽ lại bảng khi mô hình đổi
const scene = createScene($('map'), {
  onSelect: (id, node) => { selection = id && node ? { id, node } : null; renderInfo($('info'), id, node, currentModel); },
});
$('info').addEventListener('click', e => { if (e.target.closest('.close')) scene.select(null); });

// Áp mô hình lên bản đồ: class bị ẩn thì làm mờ khối, và vẽ lại bảng chú thích
function applyModel(model) {
  currentModel = model;
  scene.setDim(node => lookup(currentModel, classOfNode(node))?.hidden === true);
  if (selection) renderInfo($('info'), selection.id, selection.node, currentModel);
}
const editor = createModelEditor({ el: $('model-editor'), button: $('model-btn'), onChange: applyModel });

const player = createPlayer({
  scene,
  els: { log: $('log'), diff: $('diff'), status: $('status') },
  onState: s => setButtons(s),
});

async function loadScenario(id) {
  try {
    player.load(await api.run(id));
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
