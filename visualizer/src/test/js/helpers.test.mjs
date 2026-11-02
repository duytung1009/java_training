import test from 'node:test';
import assert from 'node:assert/strict';
import { guessByPrefix } from '../../main/resources/static/js/map/layout.js';
import { ellipsize } from '../../main/resources/static/js/dom.js';
import { shortThread } from '../../main/resources/static/js/panels/log.js';
import { lookup, aggregateOf, contextById, setRole, removeRelation } from '../../main/resources/static/js/model/draft.js';

const model = () => ({
  version: 1,
  contexts: [{ id: 'a', title: 'A', color: null }, { id: 'b', title: 'B', color: null }],
  classes: [
    { cls: 'X', context: 'a', role: 'aggregate', hidden: false },
    { cls: 'Y', context: 'a', role: 'entity', hidden: false },
  ],
  relations: [{ from: 'X', to: 'Y', source: 'field' }],
});

test('guessByPrefix: khớp tiền tố đầu tiên, không khớp thì undefined', () => {
  const prefixes = [['cart/', 'cart'], ['order', 'orders']];
  assert.equal(guessByPrefix('cart/line-1', prefixes), 'cart');
  assert.equal(guessByPrefix('order-7', prefixes), 'orders');
  assert.equal(guessByPrefix('zzz', prefixes), undefined);
});

test('ellipsize: cắt ở max-1 ký tự kèm …; ngắn hơn thì giữ nguyên; shortThread giữ kết quả cũ', () => {
  assert.equal(ellipsize('abcdef', 6), 'abcdef');
  assert.equal(ellipsize('abcdefg', 6), 'abcde…');
  assert.equal(ellipsize('abcdefg', 7), 'abcdefg');
  assert.equal(shortThread('abcdefgh'), 'abcdef…');
  assert.equal(shortThread('abcdefg'), 'abcdefg');
});

test('contextById / lookup: tra được, lạ hoặc mô hình null thì null', () => {
  const m = model();
  assert.equal(contextById(m, 'b').title, 'B');
  assert.equal(contextById(m, 'zz'), null);
  assert.equal(contextById(null, 'a'), null);
  assert.equal(lookup(m, 'X').role, 'aggregate');
  assert.equal(lookup(m, 'nope'), null);
  assert.equal(lookup(null, 'X'), null);
});

test('lookup / aggregateOf memo: sau thao tác draft cho kết quả theo mô hình mới, mô hình cũ không đổi', () => {
  const m = model();
  assert.equal(aggregateOf(m, 'Y'), 'X');
  assert.equal(aggregateOf(m, 'Y'), 'X'); // lần hai đọc từ memo
  const m2 = setRole(m, 'Y', 'service');
  assert.equal(lookup(m2, 'Y').role, 'service');
  assert.equal(aggregateOf(m2, 'Y'), null);
  assert.equal(lookup(m, 'Y').role, 'entity');
  assert.equal(aggregateOf(m, 'Y'), 'X');
  const m3 = removeRelation(m, 'X', 'Y');
  assert.equal(aggregateOf(m3, 'Y'), null);
  assert.equal(aggregateOf(m, 'Y'), 'X');
});
