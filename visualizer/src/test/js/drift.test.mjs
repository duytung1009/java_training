import test from 'node:test';
import assert from 'node:assert/strict';
import { diffModel, adoptAdded } from '../../main/resources/static/js/model/drift.js';

const cd = (cls, context, role = 'value') => ({ cls, context, role, hidden: false, source: 'package' });
const model = () => ({
  version: 1,
  contexts: [{ id: 'a', title: 'A', color: null }],
  classes: [cd('X', 'a'), cd('Old', 'a')],
  relations: [{ from: 'X', to: 'Old', source: 'field' }],
});
const suggestion = () => ({
  version: 1,
  contexts: [{ id: 'a', title: 'A', color: null }, { id: 'n', title: 'N', color: '--c' }],
  classes: [cd('X', 'a'), cd('New', 'n'), cd('New2', 'a'), cd('Free', null)],
  relations: [{ from: 'X', to: 'New', source: 'param' }, { from: 'New', to: 'Gone', source: 'field' }],
});

test('diffModel: added và removed', () => {
  const d = diffModel(model(), suggestion());
  assert.deepEqual(d.added.map(c => c.cls), ['New', 'New2', 'Free']);
  assert.deepEqual(d.removed, ['Old']);
});

test('diffModel: không lệch thì rỗng', () => {
  const m = model();
  assert.deepEqual(diffModel(m, m), { added: [], removed: [] });
});

test('adoptAdded thêm class theo def gợi ý và context còn thiếu, không sửa đầu vào', () => {
  const m = model(), s = suggestion(), snap = JSON.stringify(m);
  const r = adoptAdded(m, s, ['New', 'Free']);
  assert.deepEqual(r.classes.map(c => c.cls), ['X', 'Old', 'New', 'Free']);
  assert.equal(r.classes.find(c => c.cls === 'New').context, 'n');
  assert.deepEqual(r.contexts.map(c => c.id), ['a', 'n']);
  assert.equal(r.contexts[1].color, '--c');
  assert.equal(JSON.stringify(m), snap);
});

test('adoptAdded không thêm context đã có, bỏ qua class đã có hoặc không có trong gợi ý', () => {
  const r = adoptAdded(model(), suggestion(), ['New2', 'X', 'Không có']);
  assert.deepEqual(r.contexts.map(c => c.id), ['a']);
  assert.deepEqual(r.classes.map(c => c.cls), ['X', 'Old', 'New2']);
});

test('adoptAdded kéo theo quan hệ có cả hai đầu trong mô hình', () => {
  const r = adoptAdded(model(), suggestion(), ['New']);
  assert.deepEqual(r.relations, [{ from: 'X', to: 'Old', source: 'field' }, { from: 'X', to: 'New', source: 'param' }]);
});

test('adoptAdded rỗng trả mô hình cũ', () => {
  const m = model();
  assert.equal(adoptAdded(m, suggestion(), []), m);
});
