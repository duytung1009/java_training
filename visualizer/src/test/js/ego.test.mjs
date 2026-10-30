import test from 'node:test';
import assert from 'node:assert/strict';
import { egoNeighbours, egoLayout, touchOf, captionOf, roleOf, MORE_ID } from '../../main/resources/static/js/view/ego.js';

const node = (id, parent = null, extra = {}) => ({ id, title: id.toUpperCase(), sub: '', kind: 'object', parent, items: [], ...extra });
const world = (...ns) => new Map(ns.map(n => [n.id, n]));
const call = (from, to) => ({ from, to, kind: 'CALL' });
const event = (from, to) => ({ from, to, kind: 'EVENT' });
const ids = ns => ns.map(n => n.id);

const NODES = world(node('pkg'), node('hub', 'pkg'), node('cart', 'pkg'), node('orders', 'pkg'), node('wh', 'pkg'), node('slot', 'hub'), node('other'));

test('egoNeighbours: không flow, không quan hệ → rỗng', () => {
  assert.deepEqual(egoNeighbours({ focusId: 'other', nodes: NODES }), []);
});

test('egoNeighbours: đếm vào / ra / hai chiều, tách lời gọi và event', () => {
  const flows = [call('cart', 'hub'), call('cart', 'hub'), event('cart', 'hub'), call('hub', 'orders'), call('hub', 'wh'), call('wh', 'hub'), call('cart', 'cart')];
  const r = Object.fromEntries(egoNeighbours({ focusId: 'hub', nodes: NODES, flows }).map(n => [n.id, n]));
  assert.deepEqual(r.cart.in, { calls: 2, events: 1 });
  assert.deepEqual(r.cart.out, { calls: 0, events: 0 });
  assert.deepEqual(r.orders.out, { calls: 1, events: 0 });
  assert.deepEqual([r.wh.in.calls, r.wh.out.calls], [1, 1]);
  assert.equal(roleOf(r.cart), 'in'); assert.equal(roleOf(r.orders), 'out'); assert.equal(roleOf(r.wh), 'out');
});

test('egoNeighbours: cha, con, dây nối; dây chỉ tính khi focus là đầu mút; gộp với flow thành một mục', () => {
  const wires = [{ a: 'hub', b: 'cart' }, { a: 'orders', b: 'hub' }, { a: 'cart', b: 'orders' }];
  const r = egoNeighbours({ focusId: 'hub', nodes: NODES, flows: [call('hub', 'cart')], wires });
  assert.deepEqual(r.map(n => [n.id, n.rel]), [['pkg', 'parent'], ['cart', 'wire'], ['orders', 'wire'], ['slot', 'child']]);
  const cart = r.find(n => n.id === 'cart');
  assert.equal(cart.out.calls, 1); // flow + dây = một mục
  assert.equal(r.filter(n => n.id === 'cart').length, 1);
  assert.equal(captionOf(r.find(n => n.id === 'orders')), 'nối');
  assert.equal(captionOf(r[0]), 'container');
  assert.equal(captionOf(r.find(n => n.id === 'slot')), 'con');
  assert.equal(captionOf(cart), '1 lời gọi ra');
});

test('egoNeighbours: flow từ hậu duệ của focus gộp về focus; flow nội bộ bỏ; đầu mút lạ bỏ', () => {
  const flows = [call('slot', 'cart'), call('cart', 'slot'), call('slot', 'hub'), call('hub', 'slot'), call('hub', 'ghost')];
  const r = Object.fromEntries(egoNeighbours({ focusId: 'hub', nodes: NODES, flows }).map(n => [n.id, n]));
  assert.deepEqual([r.cart.out.calls, r.cart.in.calls], [1, 1]);
  assert.equal(r.slot.in.calls + r.slot.out.calls, 0);
  assert.equal(r.ghost, undefined);
  assert.deepEqual(touchOf('hub', NODES, 'slot', 'hub'), null);
  assert.deepEqual(touchOf('hub', NODES, 'slot', 'cart'), { other: 'cart', dir: 'out' });
  assert.deepEqual(touchOf('hub', NODES, 'cart', 'hub'), { other: 'cart', dir: 'in' });
  assert.equal(touchOf('hub', NODES, 'cart', 'wh'), null);
});

test('egoNeighbours: xếp theo vai trò rồi tiêu đề, kết quả ổn định', () => {
  const flows = [call('orders', 'hub'), call('cart', 'hub'), call('hub', 'wh')];
  const a = egoNeighbours({ focusId: 'hub', nodes: NODES, flows });
  const b = egoNeighbours({ focusId: 'hub', nodes: NODES, flows: [...flows].reverse() });
  assert.deepEqual(ids(a), ['pkg', 'cart', 'orders', 'wh', 'slot']);
  assert.deepEqual(ids(a), ids(b));
});

// ---- Bố cục ----
const nb = (id, o = {}) => ({ id, title: id, sub: '', rel: null, in: { calls: 0, events: 0 }, out: { calls: 0, events: 0 }, ...o });
const IN = { calls: 1, events: 0 }, ZERO = { calls: 0, events: 0 };
const overlap = (p, q) => p.x < q.x + q.w && q.x < p.x + p.w && p.y < q.y + q.h && q.y < p.y + p.h;
const onBorder = ([x, y], r, e = 1e-6) => {
  const inX = x >= r.x - e && x <= r.x + r.w + e, inY = y >= r.y - e && y <= r.y + r.h + e;
  return (inY && (Math.abs(x - r.x) < e || Math.abs(x - r.x - r.w) < e)) || (inX && (Math.abs(y - r.y) < e || Math.abs(y - r.y - r.h) < e));
};
function check(l, focusId) {
  const rs = Object.entries(l.rects);
  rs.forEach(([a, p], i) => rs.slice(i + 1).forEach(([b, q]) => assert.ok(!overlap(p, q), `${a} chồng ${b}`)));
  for (const [id, r] of rs) assert.ok(r.x >= 0 && r.y >= 0 && r.x + r.w <= l.width + 1e-6 && r.y + r.h <= l.height + 1e-6, `${id} ngoài khung`);
  for (const e of l.edges) {
    assert.equal(e.from, focusId);
    assert.ok(onBorder(e.points[0], l.rects[focusId]), `${e.to}: đầu cạnh không nằm trên viền focus`);
    assert.ok(onBorder(e.points.at(-1), l.rects[e.to]), `${e.to}: cuối cạnh không nằm trên viền thẻ`);
    for (let i = 0; i + 1 < e.points.length; i++) assert.ok(e.points[i][0] === e.points[i + 1][0] || e.points[i][1] === e.points[i + 1][1], `${e.to}: đoạn chéo`);
  }
}

test('egoLayout: không hàng xóm → chỉ thẻ focus, cao theo số item', () => {
  const l = egoLayout({ id: 'f', rows: 3 }, []);
  assert.deepEqual(Object.keys(l.rects), ['f']);
  assert.deepEqual(l.edges, []);
  assert.deepEqual([l.rects.f.x, l.rects.f.y, l.width, l.height], [0, 0, l.rects.f.w, l.rects.f.h]);
  assert.ok(egoLayout({ id: 'f', rows: 20 }, []).rects.f.h > l.rects.f.h + 14 * 16);
  check(l, 'f');
});

test('egoLayout: vào → trái, ra / hai chiều / chỉ dây → phải; cạnh có hướng', () => {
  const ns = [nb('a', { in: IN }), nb('b', { out: IN }), nb('c', { in: IN, out: IN }), nb('d', { rel: 'wire' })];
  const l = egoLayout({ id: 'f', rows: 4 }, ns);
  const f = l.rects.f;
  assert.ok(l.rects.a.x + l.rects.a.w <= f.x);
  for (const id of ['b', 'c', 'd']) assert.ok(l.rects[id].x >= f.x + f.w, id);
  const dir = Object.fromEntries(l.edges.map(e => [e.to, [e.dir, e.structural]]));
  assert.deepEqual(dir, { a: ['in', false], b: ['out', false], c: ['both', false], d: [null, true] });
  check(l, 'f');
});

test('egoLayout: cha ở trên, con thành hàng dưới; nhiều con thì cắt và có thẻ +N', () => {
  const kids = Array.from({ length: 12 }, (_, i) => nb(`k${String(i).padStart(2, '0')}`, { rel: 'child' }));
  const l = egoLayout({ id: 'f', rows: 2 }, [nb('p', { rel: 'parent' }), ...kids]);
  assert.ok(l.rects.p.y + l.rects.p.h <= l.rects.f.y);
  assert.equal(l.more, 4);
  assert.ok(l.rects[MORE_ID]);
  assert.equal(Object.keys(l.rects).length, 1 + 1 + 8 + 1);
  for (const id of Object.keys(l.rects).filter(k => k.startsWith('k') || k === MORE_ID)) assert.ok(l.rects[id].y >= l.rects.f.y + l.rects.f.h);
  check(l, 'f');
  const few = egoLayout({ id: 'f', rows: 2 }, kids.slice(0, 3));
  assert.equal(few.more, 0); assert.equal(few.rects[MORE_ID], undefined);
});

test('egoLayout: con có flow được ưu tiên giữ khi cắt', () => {
  const kids = Array.from({ length: 10 }, (_, i) => nb(`k${i}`, { rel: 'child', ...(i === 9 ? { out: IN } : {}) }));
  assert.ok(egoLayout({ id: 'f', rows: 1 }, kids).rects.k9);
});

test('egoLayout: nhiều hàng xóm mọi phía, không chồng, cạnh bám viền, cổng không trùng', () => {
  const ns = [nb('p', { rel: 'parent' }), ...'abcde'.split('').map(id => nb(id, { in: IN })), ...'vwxyz'.split('').map(id => nb(id, { out: IN, in: ZERO })), nb('m', { rel: 'wire' }), nb('k1', { rel: 'child' }), nb('k2', { rel: 'child' })];
  const l = egoLayout({ id: 'f', rows: 6 }, ns);
  check(l, 'f');
  const starts = l.edges.map(e => e.points[0].join(','));
  assert.equal(new Set(starts).size, starts.length); // mỗi cạnh một cổng riêng
});
