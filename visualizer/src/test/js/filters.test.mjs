import test from 'node:test';
import assert from 'node:assert/strict';
import {
  DEFAULT, isActive, activeCount, normalize, aggregateOptions, eventMatches, flowMatches, nodeMatches, changedNodeIds, threadsOf,
} from '../../main/resources/static/js/view/filters.js';
import { nodeLookup } from '../../main/resources/static/js/view/levels.js';

const model = {
  version: 1,
  contexts: [{ id: 'sales', title: 'Bán hàng' }, { id: 'billing', title: 'Thanh toán' }],
  classes: [
    { cls: 'Order', context: 'sales', role: 'aggregate', hidden: false },
    { cls: 'Line', context: 'sales', role: 'entity', hidden: false },
    { cls: 'Oops', context: 'sales', role: 'other', hidden: true },
    { cls: 'Invoice', context: 'billing', role: 'aggregate', hidden: false },
  ],
  relations: [{ from: 'Order', to: 'Line', source: 'field' }],
};
const node = (id, kind, cls, items = []) => ({ id, kind, cls, items });
const world = { nodes: [
  node('sales', 'area', 'domain.sales'), node('order:1', 'object', 'Order'), node('line:1', 'object', 'Line'),
  node('invoice:1', 'object', 'Invoice'), node('free', 'object', 'Mystery'),
] };
const lk = nodeLookup(null, world, null, []);
const f = (patch = {}) => ({ ...DEFAULT, ...patch });
const ev = (o = {}) => ({ outcome: 'OK', thread: 't1', flows: [{ from: 'order:1', to: 'invoice:1' }], ...o });

test('isActive và normalize', () => {
  assert.equal(isActive(DEFAULT), false);
  for (const p of [{ contexts: ['a'] }, { aggregates: ['A'] }, { outcome: 'OK' }, { flowKind: 'EVENT' }, { thread: 't' }, { changedOnly: true }])
    assert.equal(isActive(f(p)), true);
  assert.deepEqual(normalize(null), DEFAULT);
  assert.deepEqual(normalize({ outcome: 'bad', flowKind: 'CALL', contexts: ['a', 1], thread: '' }), f({ flowKind: 'CALL', contexts: ['a'] }));
});

test('aggregateOptions: gộp entity vào aggregate, bỏ class ẩn, lọc theo context', () => {
  assert.deepEqual(aggregateOptions(model).map(o => o.id), ['Order', 'Invoice']);
  assert.deepEqual(aggregateOptions(model, ['billing']).map(o => o.id), ['Invoice']);
  // service lẻ có khối riêng ở tầng aggregate nhưng không là lựa chọn của bộ lọc aggregate
  const withService = { ...model, classes: [...model.classes, { cls: 'Pricer', context: 'sales', role: 'service', hidden: false }] };
  assert.deepEqual(aggregateOptions(withService).map(o => o.id), ['Order', 'Invoice']);
});

test('thread: chỉ giữ event của thread đó', () => {
  const events = [ev({ thread: 't1' }), ev({ thread: 't2' }), ev({ thread: 't1' })];
  const kept = events.filter(e => eventMatches(f({ thread: 't2' }), e, model, lk));
  assert.equal(kept.length, 1);
  assert.equal(kept[0].thread, 't2');
  assert.equal(events.filter(e => eventMatches(DEFAULT, e, model, lk)).length, 3);
  assert.deepEqual(threadsOf(events), ['t1', 't2']);
});

test('eventMatches: kết quả, context/aggregate theo đầu mút flow, đầu mút lạ coi là khớp', () => {
  assert.equal(eventMatches(f({ outcome: 'THROWN' }), ev(), model, lk), false);
  assert.equal(eventMatches(f({ outcome: 'THROWN' }), ev({ outcome: 'THROWN' }), model, lk), true);
  assert.equal(eventMatches(f({ contexts: ['billing'] }), ev(), model, lk), true); // một đầu là invoice
  assert.equal(eventMatches(f({ contexts: ['billing'] }), ev({ flows: [{ from: 'order:1', to: 'line:1' }] }), model, lk), false);
  assert.equal(eventMatches(f({ aggregates: ['Invoice'] }), ev({ flows: [{ from: 'order:1', to: 'line:1' }] }), model, lk), false);
  assert.equal(eventMatches(f({ aggregates: ['Order'] }), ev({ flows: [{ from: 'order:1', to: 'line:1' }] }), model, lk), true);
  assert.equal(eventMatches(f({ contexts: ['billing'] }), ev({ flows: [{ from: 'x', to: 'y' }] }), model, lk), true);
  assert.equal(eventMatches(f({ contexts: ['billing'] }), ev({ flows: [] }), model, lk), true);
  assert.equal(eventMatches(f({ contexts: ['billing'] }), ev({ flows: [{ from: 'order:1', to: 'line:1' }] }), null, lk), true); // không mô hình
});

test('flowMatches: loại flow (kind mặc định CALL) và vùng chọn', () => {
  const call = { from: 'order:1', to: 'line:1' }, event = { ...call, kind: 'EVENT' };
  assert.equal(flowMatches(f({ flowKind: 'CALL' }), call, model, lk), true);
  assert.equal(flowMatches(f({ flowKind: 'CALL' }), event, model, lk), false);
  assert.equal(flowMatches(f({ flowKind: 'EVENT' }), call, model, lk), false);
  assert.equal(flowMatches(f({ flowKind: 'EVENT' }), event, model, lk), true);
  assert.equal(flowMatches(f({ contexts: ['billing'] }), call, model, lk), false);
});

test('nodeMatches: context, aggregate, chỉ object có thay đổi', () => {
  const n = id => world.nodes.find(x => x.id === id);
  const sales = f({ contexts: ['sales'] });
  assert.equal(nodeMatches(sales, n('order:1'), model), true);
  assert.equal(nodeMatches(sales, n('invoice:1'), model), false);
  assert.equal(nodeMatches(sales, n('sales'), model), true); // area theo context
  assert.equal(nodeMatches(sales, n('free'), model), false);
  assert.equal(nodeMatches(f({ aggregates: ['Order'] }), n('line:1'), model), true); // Line thuộc Order (field)
  assert.equal(nodeMatches(f({ aggregates: ['Order'] }), n('invoice:1'), model), false);
  assert.equal(nodeMatches(f({ aggregates: ['Order'] }), n('sales'), model), true); // area không xét aggregate
  assert.equal(nodeMatches(sales, n('invoice:1'), null), true); // không mô hình: bỏ qua
  const changed = new Set(['order:1']);
  const co = f({ changedOnly: true });
  assert.equal(nodeMatches(co, n('order:1'), model, changed), true);
  assert.equal(nodeMatches(co, n('line:1'), model, changed), false);
  assert.equal(nodeMatches(co, n('sales'), model, changed), true); // area là khung chứa
});

test('changedNodeIds: node mới hoặc items đổi, so theo node id', () => {
  const it = (key, value) => ({ key, value });
  const before = { nodes: [node('a', 'object', 'A', [it('x', 1)]), node('b', 'object', 'B', [it('x', 1)]), node('c', 'object', 'C', [it('x', 1), it('y', 2)])] };
  const now = { nodes: [node('a', 'object', 'A', [it('x', 2)]), node('b', 'object', 'B', [it('x', 1)]), node('c', 'object', 'C', [it('x', 1)]), node('d', 'object', 'D')] };
  assert.deepEqual([...changedNodeIds(before, now)].sort(), ['a', 'c', 'd']);
  assert.equal(changedNodeIds(before, before).size, 0);
  assert.equal(changedNodeIds(null, null).size, 0);
});

test('activeCount: đếm số chiều lọc đang bật', () => {
  assert.equal(activeCount(DEFAULT), 0);
  assert.equal(activeCount({ ...DEFAULT, contexts: ['a', 'b'], outcome: 'OK', changedOnly: true }), 3);
  assert.equal(activeCount({ ...DEFAULT, aggregates: ['A'], flowKind: 'EVENT', thread: 't' }), 3);
});
