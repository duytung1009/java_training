// Giữ run + con trỏ bước; phát lại tuần tự từng event.
// Mỗi bước: flow → đồng bộ ô theo worldAfter → nháy panel → cập nhật log/diff.
import { renderLog } from './panels/log.js';
import { renderDiff } from './panels/diff.js';

const sleep = ms => new Promise(r => setTimeout(r, ms));
const GAP_MS = 500, FLOW_MS = 900;

export function createPlayer({ scene, els, onState }) {
  let run = null, cursor = 0, selected = -1;
  let playing = false, busy = false, speed = 1;
  let gen = 0; // tăng mỗi lần reset/đổi run để bước đang chờ tự huỷ

  const events = () => run?.events ?? [];
  const notify = () => onState?.({ playing, busy, loaded: !!run, done: cursor >= events().length });

  function render() {
    const done = events().slice(0, cursor);
    renderLog(els.log, done, selected, select);
    renderDiff(els.diff, events()[selected], run);
    els.status.textContent = run
      ? `${run.scenarioId} · step ${cursor}/${events().length} · threads ${new Set(done.map(e => e.thread)).size} · exceptions ${done.filter(e => e.outcome === 'THROWN').length}`
      : '';
    notify();
  }

  // Bấm dòng log: chỉ chọn bước + nháy panel, không phát lại
  function select(i) {
    selected = i;
    render();
    scene.flash(events()[i].panels, events()[i].outcome === 'OK');
  }

  function load(newRun) { run = newRun; reset(); }

  function reset() {
    gen++; playing = false; busy = false; cursor = 0; selected = -1;
    scene.reset(run?.worldBefore);
    render();
  }

  async function step() {
    if (busy || !run || cursor >= events().length) return false;
    busy = true; notify();
    const g = gen, e = events()[cursor], ok = e.outcome === 'OK';
    await scene.flow(e.flow, ok, FLOW_MS / speed);
    if (g !== gen) return false;
    scene.sync(e.worldAfter, { speed });
    scene.flash(e.panels, ok);
    cursor++; selected = cursor - 1;
    busy = false;
    render();
    return true;
  }

  async function play() {
    if (playing || busy || !run) return;
    if (cursor >= events().length) reset();
    const g = gen;
    playing = true; notify();
    while (playing && cursor < events().length) {
      if (!await step()) break;
      if (cursor < events().length && playing) await sleep(GAP_MS / speed);
      if (g !== gen) return;
    }
    if (g === gen) { playing = false; notify(); }
  }

  // Dừng sau bước hiện tại
  function pause() { playing = false; notify(); }

  function setSpeed(s) {
    speed = s;
    document.documentElement.style.setProperty('--spd', s);
  }

  return { load, reset, step, play, pause, setSpeed };
}
