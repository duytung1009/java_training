// Gọi REST; không có API (hoặc ?fixture=...) thì dùng fixture: ?fixture=events → sample-events.json, còn lại sample-run.json
const params = new URLSearchParams(location.search);
const FIXTURE_URL = params.get('fixture') === 'events' ? 'fixtures/sample-events.json' : 'fixtures/sample-run.json';
let fixtureMode = params.has('fixture');
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

// Mô hình domain (P3b); fixture hoặc lỗi mạng thì trả null
export async function getModel() { // → { origin, model } | null
  if (fixtureMode) return null;
  try { return await json(await fetch('/api/viz/model')); }
  catch { return null; }
}

export async function getSuggestion() { // → DomainModel | null
  if (fixtureMode) return null;
  try { return await json(await fetch('/api/viz/model/suggestion')); }
  catch { return null; }
}

// PUT mô hình; không ném lỗi với 400/409: trả { ok, status, body } để editor hiện lỗi (lỗi mạng: status 0)
export async function saveModel(model) {
  if (fixtureMode) return null;
  try {
    const res = await fetch('/api/viz/model', {
      method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(model),
    });
    const body = await res.json().catch(() => null);
    return { ok: res.ok, status: res.status, body };
  } catch (err) {
    return { ok: false, status: 0, body: { detail: `Không gọi được BE: ${err.message}` } };
  }
}
