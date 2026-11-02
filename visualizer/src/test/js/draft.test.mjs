import test from 'node:test';
import assert from 'node:assert/strict';
import {
  setClassContext, setRole, setHidden, addContext, renameContext, mergeContexts, deleteContext,
  removeRelation, restoreRelation, removeClass, classOfNode, contextOfNode, lookup, aggregateOf,
} from '../../main/resources/static/js/model/draft.js';

const base = () => ({
  version: 1,
  contexts: [{ id: 'a', title: 'A', color: null }, { id: 'b', title: 'B', color: null }],
  classes: [
    { cls: 'X', context: 'a', role: 'aggregate', hidden: false, source: 'identity' },
    { cls: 'Y', context: 'a', role: 'entity', hidden: false, source: 'field' },
    { cls: 'Z', context: 'b', role: 'value', hidden: false, source: 'type' },
    { cls: 'W', context: null, role: 'other', hidden: false, source: 'package' },
  ],
  relations: [{ from: 'X', to: 'Y', source: 'field' }, { from: 'X', to: 'Z', source: 'observed:s1' }],
});
const frozen = () => { const m = base(); return [m, JSON.stringify(m)]; };

test('setClassContext đổi context, source = manual, không sửa đầu vào', () => {
  const [m, snap] = frozen();
  const r = setClassContext(m, 'W', 'b');
  assert.equal(lookup(r, 'W').context, 'b');
  assert.equal(lookup(r, 'W').source, 'manual');
  assert.equal(lookup(r, 'X').source, 'identity');
  assert.equal(JSON.stringify(m), snap);
});

test('setClassContext null = chưa xếp; context lạ hoặc class lạ ném lỗi', () => {
  assert.equal(lookup(setClassContext(base(), 'X', null), 'X').context, null);
  assert.throws(() => setClassContext(base(), 'X', 'nope'));
  assert.throws(() => setClassContext(base(), 'Q', 'a'));
});

test('setRole hợp lệ, không hợp lệ ném lỗi', () => {
  const [m, snap] = frozen();
  const r = setRole(m, 'Z', 'entity');
  assert.equal(lookup(r, 'Z').role, 'entity');
  assert.equal(lookup(r, 'Z').source, 'manual');
  assert.equal(JSON.stringify(m), snap);
  assert.throws(() => setRole(m, 'Z', 'boss'), Error);
});

test('setHidden', () => {
  const [m, snap] = frozen();
  const r = setHidden(m, 'Y', true);
  assert.equal(lookup(r, 'Y').hidden, true);
  assert.equal(lookup(r, 'Y').source, 'manual');
  assert.equal(JSON.stringify(m), snap);
  assert.equal(lookup(setHidden(r, 'Y', false), 'Y').hidden, false);
});

test('addContext: id rỗng hoặc trùng ném lỗi, title mặc định = id', () => {
  const [m, snap] = frozen();
  const r = addContext(m, 'c', 'C');
  assert.deepEqual(r.contexts.at(-1), { id: 'c', title: 'C', color: null });
  assert.equal(JSON.stringify(m), snap);
  assert.equal(addContext(m, 'd', '').contexts.at(-1).title, 'd');
  assert.throws(() => addContext(m, '', 'x'));
  assert.throws(() => addContext(m, '   ', 'x'));
  assert.throws(() => addContext(m, 'a', 'x'));
});

test('renameContext chỉ đổi title', () => {
  const [m, snap] = frozen();
  const r = renameContext(m, 'a', 'Mới');
  assert.deepEqual(r.contexts[0], { id: 'a', title: 'Mới', color: null });
  assert.equal(lookup(r, 'X').context, 'a');
  assert.equal(JSON.stringify(m), snap);
  assert.throws(() => renameContext(m, 'nope', 'x'));
  assert.throws(() => renameContext(m, 'a', ' '));
});

test('mergeContexts chuyển class và bỏ context nguồn', () => {
  const [m, snap] = frozen();
  const r = mergeContexts(m, 'a', 'b');
  assert.deepEqual(r.contexts.map(c => c.id), ['b']);
  assert.equal(lookup(r, 'X').context, 'b');
  assert.equal(lookup(r, 'Y').context, 'b');
  assert.equal(lookup(r, 'X').source, 'manual');
  assert.equal(lookup(r, 'Z').source, 'type');
  assert.equal(lookup(r, 'W').context, null);
  assert.equal(JSON.stringify(m), snap);
  assert.throws(() => mergeContexts(m, 'a', 'a'));
  assert.throws(() => mergeContexts(m, 'a', 'nope'));
  assert.throws(() => mergeContexts(m, 'nope', 'a'));
});

test('deleteContext đưa class về chưa xếp', () => {
  const [m, snap] = frozen();
  const r = deleteContext(m, 'a');
  assert.deepEqual(r.contexts.map(c => c.id), ['b']);
  assert.equal(lookup(r, 'X').context, null);
  assert.equal(lookup(r, 'Y').context, null);
  assert.equal(lookup(r, 'Z').context, 'b');
  assert.equal(r.classes.length, 4);
  assert.equal(JSON.stringify(m), snap);
  assert.throws(() => deleteContext(m, 'nope'));
});

test('removeRelation / restoreRelation', () => {
  const [m, snap] = frozen();
  const r = removeRelation(m, 'X', 'Y');
  assert.deepEqual(r.relations, [{ from: 'X', to: 'Z', source: 'observed:s1' }]);
  assert.equal(JSON.stringify(m), snap);
  const back = restoreRelation(r, { from: 'X', to: 'Y', source: 'field' });
  assert.equal(back.relations.length, 2);
  assert.equal(restoreRelation(back, { from: 'X', to: 'Y', source: 'field' }).relations.length, 2);
  assert.equal(removeRelation(m, 'Q', 'R').relations.length, 2);
});

test('removeClass bỏ class và quan hệ chạm tới nó', () => {
  const [m, snap] = frozen();
  const r = removeClass(m, 'Y');
  assert.equal(lookup(r, 'Y'), null);
  assert.deepEqual(r.relations, [{ from: 'X', to: 'Z', source: 'observed:s1' }]);
  assert.equal(JSON.stringify(m), snap);
});

test('classOfNode: object, collection, area', () => {
  assert.equal(classOfNode({ kind: 'object', cls: 'Warehouse' }), 'Warehouse');
  assert.equal(classOfNode({ kind: 'collection', cls: 'List<Order>' }), 'Order');
  assert.equal(classOfNode({ kind: 'collection', cls: 'Map<A, B>' }), 'A, B');
  assert.equal(classOfNode({ kind: 'collection', cls: 'List' }), null);
  assert.equal(classOfNode({ kind: 'area', cls: 'x.y' }), null);
  assert.equal(classOfNode({ kind: 'object', cls: null }), null);
  assert.equal(classOfNode(null), null);
});

test('contextOfNode: object, collection, area khớp id hoặc hậu tố ".id"', () => {
  const m = base();
  assert.equal(contextOfNode(m, { kind: 'object', cls: 'Z' }), 'b');
  assert.equal(contextOfNode(m, { kind: 'collection', cls: 'List<Y>' }), 'a');
  assert.equal(contextOfNode(m, { kind: 'object', cls: 'W' }), null);
  assert.equal(contextOfNode(m, { kind: 'object', cls: 'Lạ' }), null);
  assert.equal(contextOfNode(m, { kind: 'area', cls: 'a' }), 'a');
  assert.equal(contextOfNode(m, { kind: 'area', cls: 'com.x.domain.b' }), 'b');
  assert.equal(contextOfNode(m, { kind: 'area', cls: 'com.x.domain.ab' }), null);
  assert.equal(contextOfNode(m, { kind: 'area', cls: null }), null);
});

test('aggregateOf: aggregate là chính nó, entity thuộc chủ duy nhất cùng context', () => {
  const m = base();
  assert.equal(aggregateOf(m, 'X'), 'X');
  assert.equal(aggregateOf(m, 'Y'), 'X');
  assert.equal(aggregateOf(m, 'Z'), null); // chủ quan sát (không phải field) và khác context
  assert.equal(aggregateOf(m, 'W'), null);
  assert.equal(aggregateOf(m, 'Lạ'), null);
});
