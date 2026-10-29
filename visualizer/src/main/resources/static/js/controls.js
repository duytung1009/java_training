// Nối nút/select ở header với player
export function initControls({ player, scenarios, onScenario }) {
  const $ = id => document.getElementById(id);
  const sel = $('scenario');
  for (const s of scenarios) {
    const o = document.createElement('option');
    o.value = s.id;
    o.textContent = `${s.session} · ${s.title}`;
    o.title = s.description ?? '';
    sel.appendChild(o);
  }
  sel.onchange = () => onScenario(sel.value);
  $('run').onclick = () => player.play();
  $('pause').onclick = () => player.pause();
  $('back').onclick = () => player.back();
  $('step').onclick = () => player.step();
  $('reset').onclick = () => player.reset();
  $('speed').onchange = e => player.setSpeed(Number(e.target.value));

  // Bật/tắt nút theo trạng thái player
  return ({ playing, busy, loaded, done, atStart }) => {
    $('run').disabled = !loaded || playing || busy;
    $('pause').disabled = !playing;
    $('back').disabled = !loaded || playing || busy || atStart;
    $('step').disabled = !loaded || playing || busy || done;
    $('reset').disabled = !loaded;
  };
}
