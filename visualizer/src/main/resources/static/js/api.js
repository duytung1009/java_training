// Gọi REST; không có API (hoặc ?fixture=1) thì dùng fixtures/sample-run.json
const FIXTURE_URL = 'fixtures/sample-run.json';
let fixtureMode = new URLSearchParams(location.search).has('fixture');
let fixtureCache = null;

export const isFixture = () => fixtureMode;

async function loadFixture() {
  fixtureCache ??= await (await fetch(FIXTURE_URL)).json();
  return fixtureCache;
}

async function json(res) {
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

export async function getScenarios() {
  if (!fixtureMode) {
    try { return await json(await fetch('/api/viz/scenarios')); }
    catch { fixtureMode = true; }
  }
  const f = await loadFixture();
  return [{ id: f.scenarioId, title: f.title, session: 'fixture', description: 'Dữ liệu tay' }];
}

export async function run(id) {
  if (fixtureMode) return loadFixture();
  return json(await fetch(`/api/viz/scenarios/${encodeURIComponent(id)}/run`, { method: 'POST' }));
}

export async function getWorld() {
  if (fixtureMode) return (await loadFixture()).worldBefore;
  return json(await fetch('/api/viz/world'));
}

// Trạng thái đồng bộ với domain; fixture hoặc lỗi thì trả null (không hiện banner)
export async function getSync() {
  if (fixtureMode) return null;
  try { return await json(await fetch('/api/viz/sync')); }
  catch { return null; }
}
