import * as api from './api.js';
import { createScene } from './map/scene.js';
import { createPlayer } from './player.js';
import { initControls } from './controls.js';

const $ = id => document.getElementById(id);
let setButtons = () => {};

const player = createPlayer({
  scene: createScene($('map')),
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
  setButtons = initControls({ player, scenarios, onScenario: loadScenario });
  if (scenarios.length) await loadScenario(scenarios[0].id);
}

start().catch(err => { $('status').textContent = `Lỗi khởi động: ${err.message}`; });
