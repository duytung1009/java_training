import test from 'node:test';
import assert from 'node:assert/strict';
import { parseNav, toHash, normalizeNav, nodeLookup, aggregateKey, touchedFirst } from '../../main/resources/static/js/view/nav.js';

const node = (id, kind, cls, extra = {}) => ({ id, kind, cls, parent: null, items: [], ...extra });
const OVERVIEW = { area: null, node: null };

test('hash: round-trip; hash rỗng, lạ hoặc "#level=…" cũ → tổng quan', () => {
  assert.deepEqual(parseNav(toHash({ area: 'inventory' })), { area: 'inventory', node: null });
  assert.equal(toHash({ area: 'inventory' }), '#area=inventory');
  assert.equal(toHash({ area: 'inventory', node: 'warehouse:WH-HN' }), '#area=inventory&node=warehouse%3AWH-HN');
  assert.deepEqual(parseNav(toHash({ area: 'inventory', node: 'warehouse:WH-HN' })), { area: 'inventory', node: 'warehouse:WH-HN' });
  assert.deepEqual(parseNav('#node=hub'), OVERVIEW); // node không có area thì bỏ
  assert.equal(toHash({ area: null, node: 'hub' }), '');
  assert.equal(toHash(OVERVIEW), '');
  assert.deepEqual(parseNav(toHash(OVERVIEW)), OVERVIEW);
  assert.deepEqual(parseNav('#area=a%20b'), { area: 'a b', node: null });
  for (const bad of ['', '#', '#foo', '#level=aggregate&ctx=sales&agg=Order', '#area=', undefined, null]) assert.deepEqual(parseNav(bad), OVERVIEW);
});

test('normalizeNav: area lạ hoặc không có danh sách → tổng quan', () => {
  assert.deepEqual(normalizeNav({ area: 'order' }, ['order', 'inventory']), { area: 'order', node: null });
  assert.deepEqual(normalizeNav({ area: 'order', node: 'cart' }, ['order']), { area: 'order', node: 'cart' });
  assert.deepEqual(normalizeNav({ area: 'nope', node: 'cart' }, ['order']), OVERVIEW);
  assert.deepEqual(normalizeNav({ area: 'nope' }, ['order']), OVERVIEW);
  assert.deepEqual(normalizeNav({ area: null }, ['order']), OVERVIEW);
  assert.deepEqual(normalizeNav({ area: 'order' }), OVERVIEW);
  assert.deepEqual(normalizeNav(undefined, ['order']), OVERVIEW);
});

test('nodeLookup: world sau thắng world hiện tại thắng world trước; id lạ rơi về đầu mút theo tiền tố', () => {
  const b = { nodes: [node('x', 'object', 'Old'), node('only-before', 'object', 'B')] };
  const n = { nodes: [node('x', 'object', 'Now'), node('only-now', 'object', 'N')] };
  const a = { nodes: [node('x', 'object', 'New')] };
  const l = nodeLookup(b, n, a, [['order:', 'x']]);
  assert.equal(l('x').cls, 'New');
  assert.equal(l('only-now').cls, 'N');
  assert.equal(l('only-before').cls, 'B');
  assert.equal(l('order:99').cls, 'New'); // tiền tố → đầu mút x
  assert.equal(l('zzz'), undefined);
  assert.equal(l(null), undefined);
});

test('aggregateKey: aggregate chứa class, không có thì chính class', () => {
  const model = {
    version: 1, contexts: [{ id: 'a', title: 'A', color: null }],
    classes: [{ cls: 'X', context: 'a', role: 'aggregate', hidden: false }, { cls: 'Y', context: 'a', role: 'entity', hidden: false }, { cls: 'Z', context: 'a', role: 'service', hidden: false }],
    relations: [{ from: 'X', to: 'Y', source: 'field' }],
  };
  assert.equal(aggregateKey(model, 'Y'), 'X');
  assert.equal(aggregateKey(model, 'X'), 'X');
  assert.equal(aggregateKey(model, 'Z'), 'Z');
});

test('touchedFirst: stable partition, pred null giữ nguyên, không đổi mảng gốc', () => {
  const xs = [1, 2, 3, 4, 5, 6];
  assert.deepEqual(touchedFirst(xs, x => x % 3 === 0), [3, 6, 1, 2, 4, 5]);
  assert.deepEqual(touchedFirst(xs, null), xs);
  assert.deepEqual(touchedFirst(xs, () => false), xs);
  assert.deepEqual(touchedFirst(xs, () => true), xs);
  assert.deepEqual(xs, [1, 2, 3, 4, 5, 6]);
});
