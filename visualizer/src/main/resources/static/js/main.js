import * as api from './api.js';
import { createScene } from './map/scene.js';
import { createPlayer } from './player.js';
import { initControls } from './controls.js';
import { renderInfo } from './panels/info.js';
import { renderSync } from './panels/sync.js';

const $ = id => document.getElementById(id);
let setButtons = () => {};

const scene = createScene($('map'), { onSelect: (id, panel) => renderInfo($('info'), id, panel) });
$('info').addEventListener('click', e => { if (e.target.closest('.close')) scene.select(null); });

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
  try { renderSync($('sync'), await api.getSync()); } catch { /* banner lỗi không được chặn app */ }
  setButtons = initControls({ player, scenarios, onScenario: loadScenario });
  if (scenarios.length) await loadScenario(scenarios[0].id);
}

start().catch(err => { $('status').textContent = `Lỗi khởi động: ${err.message}`; });
