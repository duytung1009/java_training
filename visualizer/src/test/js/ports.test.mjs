import test from 'node:test';
import assert from 'node:assert/strict';
import { clipWire, placePorts } from '../../main/resources/static/js/map/ports.js';

const approx = (a, b) => a.forEach((v, i) => assert.ok(Math.abs(v - b[i]) < 1e-9, `${a} ≉ ${b}`));
const R = { x: 0, y: 0, w: 10, d: 8 };

test('clipWire: thoát thẳng qua từng mép', () => {
  assert.deepEqual(clipWire([[5, 3], [5, -4]], R), { inner: [[5, 3], [5, 0]], cross: [5, 0], side: 'top' });
  assert.deepEqual(clipWire([[5, 3], [5, 12]], R), { inner: [[5, 3], [5, 8]], cross: [5, 8], side: 'bottom' });
  assert.deepEqual(clipWire([[4, 2], [-3, 2]], R), { inner: [[4, 2], [0, 2]], cross: [0, 2], side: 'left' });
  assert.deepEqual(clipWire([[4, 2], [15, 2]], R), { inner: [[4, 2], [10, 2]], cross: [10, 2], side: 'right' });
});

test('clipWire: đường chữ L, giữ góc trong area', () => {
  const c = clipWire([[2, 2], [2, 6], [14, 6]], R);
  assert.deepEqual(c, { inner: [[2, 2], [2, 6], [10, 6]], cross: [10, 6], side: 'right' });
});

test('clipWire: fromStart=false đi từ điểm cuối, inner theo chiều từ đầu trong', () => {
  const c = clipWire([[14, 6], [2, 6], [2, 2]], R, false);
  assert.deepEqual(c, { inner: [[2, 2], [2, 6], [10, 6]], cross: [10, 6], side: 'right' });
});

test('clipWire: rời, vào lại rồi rời lần nữa thì cắt ở lần rời đầu', () => {
  const c = clipWire([[5, 4], [5, -2], [12, -2], [12, 4], [20, 4]], R);
  assert.deepEqual(c, { inner: [[5, 4], [5, 0]], cross: [5, 0], side: 'top' });
});

test('clipWire: đầu trong nằm đúng mép rồi đi ra', () => {
  const c = clipWire([[10, 5], [14, 5]], R);
  assert.deepEqual(c, { inner: [[10, 5]], cross: [10, 5], side: 'right' });
  assert.deepEqual(clipWire([[3, 8], [3, 12]], R)?.side, 'bottom');
});

test('clipWire: đường kết thúc đúng trên mép theo chiều đi thì cắt tại đó', () => {
  const c = clipWire([[3, 3], [3, 8]], R);
  assert.deepEqual(c, { inner: [[3, 3], [3, 8]], cross: [3, 8], side: 'bottom' });
});

test('clipWire: nằm trọn trong area (hoặc đầu trong ở ngoài) thì null', () => {
  assert.equal(clipWire([[1, 1], [1, 5], [6, 5]], R), null);
  assert.equal(clipWire([[20, 1], [1, 1]], R), null);
});

test('placePorts: đặt ngoài mép với khe, tâm theo điểm cắt, có stub', () => {
  const [t, b, l, r] = placePorts([
    { cross: [5, 0], side: 'top' }, { cross: [5, 8], side: 'bottom' }, { cross: [0, 2], side: 'left' }, { cross: [10, 2], side: 'right' },
  ], R, { w: 2.6, d: 0.8, gap: 0.15 });
  approx([t.x, t.y, t.w, t.d], [3.7, -0.95, 2.6, 0.8]);
  approx([b.x, b.y], [3.7, 8.15]);
  approx([l.x, l.y], [-2.75, 1.6]);
  approx([r.x, r.y], [10.15, 1.6]);
  assert.equal(t.stub.length, 2);
  approx(t.stub[1], [5, -0.15]);
  approx(r.stub[1], [10.15, 2]);
});

test('placePorts: hai cổng cùng mép bị đẩy ra, không chồng nhau, giữ thứ tự đầu vào', () => {
  const res = placePorts([{ cross: [6, 0], side: 'top' }, { cross: [5, 0], side: 'top' }, { cross: [0, 2], side: 'left' }, { cross: [0, 2.3], side: 'left' }], R);
  const [a, b, c, d] = res;
  assert.ok(b.x + b.w <= a.x, 'cổng cross x=5 đứng trước');
  assert.ok(a.x >= b.x + b.w);
  assert.ok(d.y >= c.y + c.d);
  assert.equal(a.side, 'top');
  // stub vẫn nối được tới cổng đã bị đẩy: điểm cuối nằm trong cạnh gần của khối
  assert.ok(a.stub.at(-1)[0] >= a.x && a.stub.at(-1)[0] <= a.x + a.w);
  assert.ok(d.stub.at(-1)[1] >= d.y && d.stub.at(-1)[1] <= d.y + d.d);
});
